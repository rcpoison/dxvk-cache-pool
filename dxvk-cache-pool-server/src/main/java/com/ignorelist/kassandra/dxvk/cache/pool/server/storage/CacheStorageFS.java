/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Striped;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheMeta;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfoEquivalenceRelativePath;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Simple storage using the filesystem.
 *
 * FS layout: storageRoot / version / executable parent path / executable name / entry sha256
 *
 * @author poison
 */
public class CacheStorageFS implements CacheStorage, Closeable {

	private static final Logger LOG=Logger.getLogger(CacheStorageFS.class.getName());
	private static final Pattern SHA_256_HEX_PATTERN=Pattern.compile("[0-9A-F]{16}", Pattern.CASE_INSENSITIVE);
	private static final BaseEncoding BASE16=BaseEncoding.base16();

	private final Equivalence<ExecutableInfo> equivalence=new ExecutableInfoEquivalenceRelativePath();

	private final Path storageRoot;
	private final Striped<ReadWriteLock> storageLock=Striped.lazyWeakReadWriteLock(64);
	private ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheInfo> storageCache;
	private ForkJoinPool storageThreadPool;

	public CacheStorageFS(Path storageRoot) {
		this.storageRoot=storageRoot;
	}

	private synchronized ForkJoinPool getThreadPool() {
		if (null==storageThreadPool) {
			storageThreadPool=new ForkJoinPool(8);
		}
		return storageThreadPool;
	}

	private Lock getReadLock(ExecutableInfo key) {
		final ReadWriteLock lock=storageLock.get(key.getRelativePath());
		final Lock readLock=lock.readLock();
		return readLock;
	}

	private Lock getWriteLock(ExecutableInfo key) {
		final ReadWriteLock lock=storageLock.get(key.getRelativePath());
		final Lock writeLock=lock.writeLock();
		return writeLock;
	}

	private synchronized ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheInfo> getStorageCache() throws IOException {
		if (null==storageCache) {
			Files.createDirectories(storageRoot);
			ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheInfo> m=new ConcurrentHashMap<>();
			ImmutableSet<String> versions=Files.list(storageRoot)
					.filter(Files::isDirectory)
					.map(Path::getFileName)
					.map(Path::toString)
					.collect(ImmutableSet.toImmutableSet());
			for (String versionString : versions) {
				try {
					final int version=Integer.parseInt(versionString);
					final Path versionDirectory=storageRoot.resolve(versionString);
					final ImmutableSetMultimap<Path, Path> entriesInRelativePath=Files.walk(versionDirectory)
							.filter(Files::isRegularFile)
							.filter(p -> SHA_256_HEX_PATTERN.matcher(p.getFileName().toString()).matches())
							.collect(ImmutableSetMultimap.toImmutableSetMultimap(p -> versionDirectory.relativize(p.getParent()), p -> p));
					entriesInRelativePath.asMap().entrySet().parallelStream()
							.map(e -> buildCacheDescriptor(e.getKey(), e.getValue(), version))
							.forEach(d -> m.put(equivalence.wrap(d.getExecutableInfo()), d));
				} catch (Exception e) {
					LOG.log(Level.WARNING, null, e);
				}
			}
			storageCache=m;
		}
		return storageCache;
	}

	private static DxvkStateCacheInfo buildCacheDescriptor(final Path relativePath, final Collection<Path> cacheEntryPaths, int version) {
		DxvkStateCacheInfo cacheInfo=new DxvkStateCacheInfo();
		cacheInfo.setVersion(version);
		final ExecutableInfo ei=new ExecutableInfo(relativePath);
		cacheInfo.setExecutableInfo(ei);
		ImmutableSet<DxvkStateCacheEntryInfo> entryDescriptors=cacheEntryPaths.stream()
				.map(Path::getFileName)
				.map(Path::toString)
				.map(BASE16::decode)
				.map(h -> new DxvkStateCacheEntryInfo(h))
				.collect(ImmutableSet.toImmutableSet());
		cacheInfo.setEntries(entryDescriptors);
		final Optional<FileTime> lastModified=cacheEntryPaths.stream()
				.map(p -> {
					try {
						return Files.getLastModifiedTime(p);
					} catch (IOException ex) {
						throw new IllegalStateException("failed to get mtime for:"+p, ex);
					}
				})
				.max(FileTime::compareTo);
		if (lastModified.isPresent()) {
			cacheInfo.setLastModified(lastModified.get().toInstant());
		}
		return cacheInfo;
	}

	@Override
	public Set<DxvkStateCacheInfo> getCacheDescriptors() {
		try {
			return ImmutableSet.copyOf(getStorageCache().values());
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, null, ex);
			return ImmutableSet.of();
		}
	}

	@Override
	public Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCacheInfo existingCache) {
		final ExecutableInfo executableInfo=existingCache.getExecutableInfo();
		final Lock readLock=getReadLock(executableInfo);
		readLock.lock();
		try {
			final DxvkStateCacheInfo cacheDescriptor=getCacheDescriptor(executableInfo);
			if (null==cacheDescriptor) {
				throw new IllegalArgumentException("no entry for executableInfo: "+executableInfo);
			}
			final Sets.SetView<DxvkStateCacheEntryInfo> missingEntries=Sets.difference(cacheDescriptor.getEntries(), existingCache.getEntries());
			final Path targetDirectory=buildTargetDirectory(existingCache);
			ForkJoinTask<ImmutableSet<DxvkStateCacheEntry>> task=getThreadPool().submit(()
					-> missingEntries.parallelStream()
							.map(e -> readEntry(targetDirectory, e))
							.collect(ImmutableSet.toImmutableSet()));
			return task.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public DxvkStateCacheInfo getCacheDescriptor(ExecutableInfo executableInfo) {
		try {
			return getStorageCache().get(equivalence.wrap(executableInfo));
		} catch (Exception ex) {
			LOG.log(Level.INFO, null, ex);
			return null;
		}
	}

	@Override
	public DxvkStateCache getCache(ExecutableInfo executableInfo) {
		final Lock readLock=getReadLock(executableInfo);
		readLock.lock();
		try {
			final DxvkStateCacheInfo cacheDescriptor=getCacheDescriptor(executableInfo);
			if (null==cacheDescriptor) {
				throw new IllegalArgumentException("no entry for executableInfo: "+executableInfo);
			}
			final Path targetDirectory=buildTargetDirectory(cacheDescriptor);

			DxvkStateCache cache=new DxvkStateCache();
			cache.setExecutableInfo(executableInfo);
			cache.setVersion(cacheDescriptor.getVersion());
			cache.setEntrySize(cacheDescriptor.getEntrySize());
			ForkJoinTask<ImmutableSet<DxvkStateCacheEntry>> task=getThreadPool().submit(()
					-> cacheDescriptor.getEntries().parallelStream()
							.map(e -> readEntry(targetDirectory, e))
							.collect(ImmutableSet.toImmutableSet()));
			cache.setEntries(task.get());
			return cache;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			readLock.unlock();
		}
	}

	private DxvkStateCacheEntry readEntry(final Path targetDirectory, final DxvkStateCacheEntryInfo cacheEntryInfo) {
		final Path entryFile=targetDirectory.resolve(BASE16.encode(cacheEntryInfo.getHash()));
		try (InputStream entryStream=Files.newInputStream(entryFile)) {
			final byte[] entryData=ByteStreams.toByteArray(entryStream);
			return new DxvkStateCacheEntry(cacheEntryInfo, entryData);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, null, ex);
			throw new IllegalStateException("failed to read entry: "+cacheEntryInfo, ex);
		}
	}

	@Override
	public void store(final DxvkStateCache cache) throws IOException {
		final ExecutableInfo executableInfo=cache.getExecutableInfo();
		final Equivalence.Wrapper<ExecutableInfo> executableInfoWrapper=equivalence.wrap(executableInfo);
		final Lock writeLock=getWriteLock(executableInfo);
		writeLock.lock();
		try {
			final DxvkStateCacheInfo descriptor=getStorageCache().computeIfAbsent(executableInfoWrapper, w -> {
				DxvkStateCacheInfo d=new DxvkStateCacheInfo();
				d.setVersion(cache.getVersion());
				d.setEntrySize(cache.getEntrySize());
				d.setExecutableInfo(executableInfo);
				d.setEntries(Sets.newConcurrentHashSet());
				return d;
			});

			final Path targetDirectory=buildTargetDirectory(cache);
			Files.createDirectories(targetDirectory);
			ImmutableSet<DxvkStateCacheEntry> newEntries=cache.getEntries().stream()
					.filter(e -> !descriptor.getEntries().contains(e.getDescriptor()))
					.collect(ImmutableSet.toImmutableSet());
			ForkJoinTask<?> task=getThreadPool().submit(()
					-> newEntries.parallelStream()
							.forEach(e -> writeCacheEntry(targetDirectory, e)));
			task.get();

			descriptor.getEntries().addAll(newEntries.stream().map(DxvkStateCacheEntry::getDescriptor).collect(ImmutableSet.toImmutableSet()));
			descriptor.setLastModified(Instant.now());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			writeLock.unlock();
		}
	}

	private static void writeCacheEntry(final Path targetDirectory, final DxvkStateCacheEntry dxvkStateCacheEntry) {
		final String fileName=BASE16.encode(dxvkStateCacheEntry.getDescriptor().getHash());
		final Path targetFile=targetDirectory.resolve(fileName);
		try (InputStream entryContent=new ByteArrayInputStream(dxvkStateCacheEntry.getEntry())) {
			Files.copy(entryContent, targetFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			throw new IllegalStateException("failed to write entry: "+dxvkStateCacheEntry, ex);
		}
	}

	private Path buildTargetDirectory(DxvkStateCacheMeta cache) {
		final ExecutableInfo executableInfo=cache.getExecutableInfo();
		final Path targetPath=storageRoot
				.resolve(Integer.toString(cache.getVersion()))
				.resolve(executableInfo.getRelativePath());
		return targetPath;
	}

	@Override
	public void close() throws IOException {
		if (null!=storageThreadPool) {
			MoreExecutors.shutdownAndAwaitTermination(storageThreadPool, 1, TimeUnit.MINUTES);
		}
	}

}
