/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.Striped;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheMeta;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple storage using the filesystem.
 *
 * FS layout: storageRoot / version / executable baseName / entry sha256
 *
 * @author poison
 */
public class CacheStorageFS implements CacheStorage {

	private static final Logger LOG=Logger.getLogger(CacheStorageFS.class.getName());
	private static final BaseEncoding BASE16=BaseEncoding.base16();

	private final Path storageRoot;
	private final Striped<ReadWriteLock> storageLock=Striped.lazyWeakReadWriteLock(64);
	private final ForkJoinPool storageThreadPool;
	private ConcurrentMap<Integer, ConcurrentMap<String, StateCacheInfo>> storageCache;

	public CacheStorageFS(final Path storageRoot, final ForkJoinPool storageThreadPool) {
		this.storageRoot=storageRoot;
		this.storageThreadPool=storageThreadPool;
	}

	private ForkJoinPool getThreadPool() {
		return storageThreadPool;
	}

	public void init() throws IOException {
		getStorageCache(StateCacheHeaderInfo.getLatestVersion());
	}

	private Lock getReadLock(String baseName) {
		final ReadWriteLock lock=storageLock.get(baseName);
		final Lock readLock=lock.readLock();
		return readLock;
	}

	private Lock getWriteLock(String baseName) {
		final ReadWriteLock lock=storageLock.get(baseName);
		final Lock writeLock=lock.writeLock();
		return writeLock;
	}

	private synchronized ConcurrentMap<String, StateCacheInfo> getStorageCache(int version) throws IOException {
		if (null==storageCache) {
			Stopwatch stopwatch=Stopwatch.createStarted();
			Files.createDirectories(storageRoot);

			final ImmutableSet<String> versions=Files.list(storageRoot)
					.filter(Files::isDirectory)
					.map(Path::getFileName)
					.map(Path::toString)
					.collect(ImmutableSet.toImmutableSet());
			ConcurrentMap<Integer, ConcurrentMap<String, StateCacheInfo>> m=new ConcurrentHashMap<>();
			final AtomicInteger entryCount=new AtomicInteger();
			final AtomicInteger baseNameCount=new AtomicInteger();
			for (String versionString : versions) {
				try {
					ConcurrentMap<String, StateCacheInfo> infoForVersion=new ConcurrentHashMap<>();
					final int currentVersion=Integer.parseInt(versionString);
					final Path versionDirectory=storageRoot.resolve(versionString);
					final ImmutableSetMultimap<Path, Path> entriesInRelativePath=Files.walk(versionDirectory)
							.filter(Files::isRegularFile)
							.filter(p -> Util.SHA_256_HEX_PATTERN.matcher(p.getFileName().toString()).matches())
							.collect(ImmutableSetMultimap.toImmutableSetMultimap(p -> versionDirectory.relativize(p.getParent()), p -> p));

					entriesInRelativePath.asMap().entrySet().parallelStream()
							.map(e -> buildCacheDescriptor(e.getKey(), e.getValue(), currentVersion))
							.peek(d -> {
								final int entrySize=d.getEntries().size();
								LOG.info(() -> "loaded: "+d+" with "+entrySize+" entries");
								entryCount.addAndGet(entrySize);
								baseNameCount.incrementAndGet();
							})
							.forEach(d -> infoForVersion.put(d.getBaseName(), d));

					m.put(currentVersion, infoForVersion);
				} catch (Exception e) {
					LOG.log(Level.WARNING, null, e);
				}
			}
			stopwatch.stop();
			LOG.log(Level.INFO, "populated storageCache in {0}ms with {1} baseNames and {2} entries", new Object[]{stopwatch.elapsed().toMillis(), baseNameCount.intValue(), entryCount.intValue()});
			storageCache=m;
		}
		return storageCache.computeIfAbsent(version, i -> new ConcurrentHashMap<>());
	}

	private static StateCacheInfo buildCacheDescriptor(final Path relativePath, final Collection<Path> cacheEntryPaths, int version) {
		StateCacheInfo cacheInfo=new StateCacheInfo();
		cacheInfo.setVersion(version);
		cacheInfo.setEntrySize(StateCacheHeaderInfo.getEntrySize(version));
		cacheInfo.setBaseName(relativePath.getFileName().toString());
		final Set<StateCacheEntryInfo> entryDescriptors=cacheEntryPaths.stream()
				.map(Path::getFileName)
				.map(Path::toString)
				.map(BASE16::decode)
				.map(h -> new StateCacheEntryInfo(h))
				.collect(Collectors.toCollection(Sets::newConcurrentHashSet));
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
			cacheInfo.setLastModified(lastModified.get().toMillis());
		}
		return cacheInfo;
	}

	@Override
	public Set<String> findBaseNames(final int version, final String subString) {
		try {
			return getStorageCache(version).keySet().stream()
					.filter(e -> Strings.isNullOrEmpty(subString)||e.toLowerCase().contains(subString.toLowerCase()))
					.collect(ImmutableSet.toImmutableSet());
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, null, ex);
			return ImmutableSet.of();
		}
	}

	@Override
	public Set<StateCacheEntry> getMissingEntries(final StateCacheInfo existingCache) {
		final String baseName=existingCache.getBaseName();
		final Lock readLock=getReadLock(baseName);
		readLock.lock();
		try {
			final int version=existingCache.getVersion();
			final StateCacheInfo cacheDescriptor=getCacheDescriptor(version, baseName);
			if (null==cacheDescriptor) {
				throw new IllegalArgumentException("no entry for executableInfo: "+baseName);
			}
			final Set<StateCacheEntryInfo> missingEntries=cacheDescriptor.getMissingEntries(existingCache);
			final Path targetDirectory=buildTargetDirectory(existingCache);
			ForkJoinTask<ImmutableSet<StateCacheEntry>> task=getThreadPool().submit(()
					-> missingEntries.parallelStream()
							.map(e -> readCacheEntry(targetDirectory, e))
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
	public StateCacheInfo getCacheDescriptor(final int version, final String baseName) {
		try {
			return getStorageCache(version).get(baseName);
		} catch (Exception ex) {
			LOG.log(Level.INFO, null, ex);
			return null;
		}
	}

	@Override
	public StateCache getCache(final int version, final String baseName) {
		final Stopwatch stopwatch=Stopwatch.createStarted();
		final Lock readLock=getReadLock(baseName);
		readLock.lock();
		try {
			final StateCacheInfo cacheDescriptor=getCacheDescriptor(version, baseName);
			if (null==cacheDescriptor) {
				throw new IllegalArgumentException("no entry for executableInfo: "+baseName);
			}

			StateCache cache=new StateCache();
			cacheDescriptor.copyShallowTo(cache);
			final Set<StateCacheEntry> cacheEntries=getCacheEntries(cache, cacheDescriptor.getEntries());
			cache.setEntries(cacheEntries);

			final Duration elapsed=stopwatch.elapsed();
			LOG.log(Level.INFO, "{0} read {1} entries in {2}ms", new Object[]{baseName, cache.getEntries().size(), elapsed.toMillis()});
			return cache;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public Set<StateCacheEntry> getCacheEntries(final StateCacheMeta cacheMeta, final Set<StateCacheEntryInfo> cacheEntryInfos) {
		final String baseName=cacheMeta.getBaseName();
		final Lock readLock=getReadLock(baseName);
		readLock.lock();
		try {
			final Path targetDirectory=buildTargetDirectory(cacheMeta);
			final ForkJoinTask<ImmutableSet<StateCacheEntry>> task=getThreadPool().submit(()
					-> cacheEntryInfos.parallelStream()
							.map(e -> readCacheEntry(targetDirectory, e))
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

	private StateCacheEntry readCacheEntry(final Path targetDirectory, final StateCacheEntryInfo cacheEntryInfo) {
		final Path entryFile=targetDirectory.resolve(BASE16.encode(cacheEntryInfo.getHash()));
		try (InputStream entryStream=new GZIPInputStream(Files.newInputStream(entryFile))) {
			final byte[] entryData=ByteStreams.toByteArray(entryStream);
			return new StateCacheEntry(cacheEntryInfo, entryData);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, null, ex);
			throw new IllegalStateException("failed to read entry: "+cacheEntryInfo, ex);
		}
	}

	private static void writeCacheEntry(final Path targetDirectory, final StateCacheEntry dxvkStateCacheEntry) {
		final String fileName=BASE16.encode(dxvkStateCacheEntry.getEntryInfo().getHash());
		final Path targetFile=targetDirectory.resolve(fileName);
		try (InputStream entryContent=new ByteArrayInputStream(dxvkStateCacheEntry.getEntry())) {
			try (OutputStream out=new GZIPOutputStream(Files.newOutputStream(targetFile))) {
				ByteStreams.copy(entryContent, out);
			}
		} catch (IOException ex) {
			throw new IllegalStateException("failed to write entry: "+dxvkStateCacheEntry, ex);
		}
	}

	@Override
	public void store(final StateCache cache) throws IOException {
		final Stopwatch stopwatch=Stopwatch.createStarted();
		final String baseName=cache.getBaseName();
		if (!Util.isSafeBaseName(baseName)) {
			throw new IllegalArgumentException("unsafe basename: "+baseName);
		}
		final Lock writeLock=getWriteLock(baseName);
		writeLock.lock();
		try {
			final int version=cache.getVersion();
			final StateCacheInfo descriptor=getStorageCache(version).computeIfAbsent(baseName, w -> {
				StateCacheInfo d=new StateCacheInfo();
				d.setVersion(version);
				d.setEntrySize(cache.getEntrySize());
				d.setBaseName(baseName);
				d.setEntries(Sets.newConcurrentHashSet());
				return d;
			});

			final Path targetDirectory=buildTargetDirectory(cache);
			Files.createDirectories(targetDirectory);
			final ImmutableSet<StateCacheEntry> newEntries=cache.getEntries().stream()
					.filter(e -> !descriptor.getEntries().contains(e.getEntryInfo()))
					.collect(ImmutableSet.toImmutableSet());
			ForkJoinTask<?> task=getThreadPool().submit(()
					-> newEntries.parallelStream()
							.forEach(e -> writeCacheEntry(targetDirectory, e)));
			task.get();
			final ImmutableSet<StateCacheEntryInfo> descriptors=newEntries.stream()
					.map(StateCacheEntry::getEntryInfo)
					.collect(ImmutableSet.toImmutableSet());
			descriptor.getEntries().addAll(descriptors);
			descriptor.setLastModified(Instant.now().toEpochMilli());
			final Duration elapsed=stopwatch.elapsed();
			LOG.log(Level.INFO, "{0} stored {1} entries in {2}ms", new Object[]{baseName, descriptors.size(), elapsed.toMillis()});
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			writeLock.unlock();
		}
	}

	private Path buildTargetDirectory(final StateCacheMeta cache) {
		final String baseName=cache.getBaseName();
		final Path targetPath=storageRoot
				.resolve(Integer.toString(cache.getVersion()))
				.resolve(baseName);
		return targetPath;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public Set<StateCacheInfo> getCacheDescriptors(int version, Set<String> baseNames) {
		try {
			return getThreadPool().submit(()
					-> baseNames.parallelStream()
							.map(bN -> getCacheDescriptor(version, bN))
							.filter(Predicates.notNull())
							.collect(ImmutableSet.toImmutableSet()))
					.get();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
