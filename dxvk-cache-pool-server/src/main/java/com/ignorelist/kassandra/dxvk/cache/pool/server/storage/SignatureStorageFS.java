/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Functions;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Striped;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.IdentifiedFirstOrdering;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.IdentityStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityWithVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Signature;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfoSignees;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
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
import java.util.stream.Collectors;

/**
 * Simple storage for signatures and keys using the filesystem.
 *
 * storage layout signatures: storageRoot / signatures / entry sha256[0-1] / entry sha256[2-] / publickey sha256 storage layout
 * public keys: storageRoot / keys / publickey sha256
 *
 * @author poison
 */
public class SignatureStorageFS implements Closeable, SignatureStorage {

	private static final Logger LOG=Logger.getLogger(SignatureStorageFS.class.getName());
	private static final BaseEncoding BASE16=BaseEncoding.base16();
	private static final Path PATH_SIGNATURES=Paths.get("signatures");
	private static final Path PATH_KEYS=Paths.get("keys");
	private static final Path PATH_IDENTITIES=Paths.get("identities");

	private final Path storageRoot;
	private final Path signaturesPath;
	private final Path keysPath;
	private final Path identitiesPath;
	private final Striped<ReadWriteLock> storageLock=Striped.lazyWeakReadWriteLock(64);
	private final Interner<PublicKeyInfo> publicKeyInfoInterner=Interners.newWeakInterner();
	private final Cache<PublicKeyInfo, PublicKey> publicKeyStorageCache=CacheBuilder.newBuilder()
			.weigher((PublicKeyInfo i, PublicKey k) -> k.getKey().length)
			.maximumWeight(8*1024*1024) // 8MiB
			.build();
	private final IdentityStorage identityStorage;
	private final IdentifiedFirstOrdering identifiedFirstOrdering;
	private ConcurrentMap<StateCacheEntryInfo, Set<PublicKeyInfo>> signatureStorageCache;
	private ForkJoinPool storageThreadPool;

	public SignatureStorageFS(Path storageRoot) throws IOException {
		this.storageRoot=storageRoot;
		signaturesPath=storageRoot.resolve(PATH_SIGNATURES);
		Files.createDirectories(signaturesPath);

		keysPath=storageRoot.resolve(PATH_KEYS);
		Files.createDirectories(keysPath);

		identitiesPath=storageRoot.resolve(PATH_IDENTITIES);
		identityStorage=new IdentityStorageFS(storageRoot, publicKeyInfoInterner);

		identifiedFirstOrdering=new IdentifiedFirstOrdering(this);
	}

	private synchronized ForkJoinPool getThreadPool() {
		if (null==storageThreadPool) {
			storageThreadPool=new ForkJoinPool(Math.max(4, Runtime.getRuntime().availableProcessors()/2));
		}
		return storageThreadPool;
	}

	private Lock getReadLock(Path path) {
		final ReadWriteLock lock=storageLock.get(path);
		final Lock readLock=lock.readLock();
		return readLock;
	}

	private Lock getWriteLock(Path path) {
		final ReadWriteLock lock=storageLock.get(path);
		final Lock writeLock=lock.writeLock();
		return writeLock;
	}

	private synchronized ConcurrentMap<StateCacheEntryInfo, Set<PublicKeyInfo>> getSignatureStorageCache() throws IOException {
		if (null==signatureStorageCache) {
			Stopwatch stopwatch=Stopwatch.createStarted();
			final ImmutableSetMultimap<Path, Path> signatureFiles=Files.walk(signaturesPath)
					.filter(p -> Util.SHA_256_HEX_PATTERN.matcher(p.getFileName().toString()).matches())
					.filter(Files::isRegularFile)
					.collect(ImmutableSetMultimap.toImmutableSetMultimap(Path::getParent, Functions.identity()));
			ConcurrentMap<StateCacheEntryInfo, Set<PublicKeyInfo>> m=new ConcurrentHashMap<>();
			final ForkJoinTask<ImmutableSet<StateCacheEntryInfoSignees>> task=getThreadPool().submit(()
					-> signatureFiles.asMap().entrySet().parallelStream()
							.map(e -> buildStateCacheEntryInfoSignees(e.getKey(), e.getValue()))
							.collect(ImmutableSet.toImmutableSet()));
			try {
				task.get().forEach(iS -> {
					m.put(iS.getEntryInfo(), iS.getPublicKeyInfos());
				});
			} catch (Exception ex) {
				throw new IOException(ex);
			}
			stopwatch.stop();
			LOG.log(Level.INFO, "populated signatureStorageCache in {0}ms with {1} keys and {2} values", new Object[]{stopwatch.elapsed().toMillis(), m.size(), m.values().size()});
			signatureStorageCache=m;
		}
		return signatureStorageCache;
	}

	private StateCacheEntryInfoSignees buildStateCacheEntryInfoSignees(Path basePath, Collection<Path> signatureFiles) {
		final Path entryHashFragment0=basePath.getParent().getFileName();
		final Path entryHashFragment1=basePath.getFileName();
		final byte[] entryHash=BASE16.decode(entryHashFragment0.toString()+entryHashFragment1.toString());
		StateCacheEntryInfo cacheEntryInfo=new StateCacheEntryInfo(entryHash);
		final Set<PublicKeyInfo> pubKeyInfo=signatureFiles.stream()
				.map(Path::getFileName)
				.map(Path::toString)
				.map(BASE16::decode)
				.map(PublicKeyInfo::new)
				.map(publicKeyInfoInterner::intern)
				.collect(Collectors.toCollection(Sets::newConcurrentHashSet));
		return new StateCacheEntryInfoSignees(cacheEntryInfo, pubKeyInfo);
	}

	private Path buildTargetPath(final StateCacheEntryInfo entryInfo) {
		final String entryHashString=BASE16.encode(entryInfo.getHash());
		return signaturesPath
				.resolve(entryHashString.substring(0, 2))
				.resolve(entryHashString.substring(2));
	}

	private static Path buildTargetPathFile(final Path targetPath, final PublicKeyInfo publicKeyInfo) {
		final String keyHashString=BASE16.encode(publicKeyInfo.getHash());
		return targetPath.resolve(keyHashString);
	}

	public void init() throws IOException {
		getSignatureStorageCache();
	}

	@Override
	public Set<PublicKeyInfo> getSignedBy(final StateCacheEntryInfo entryInfo) {
		try {
			final Set<PublicKeyInfo> signedBy=getSignatureStorageCache().get(entryInfo);
			if (null!=signedBy) {
				if (signedBy.size()<=MAX_SIGNATURES) {
					return signedBy;
				}
				return signedBy.stream()
						.sorted(identifiedFirstOrdering)
						.limit(MAX_SIGNATURES)
						.collect(ImmutableSet.toImmutableSet());
			}
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, null, ex);
		}
		return ImmutableSet.of();
	}

	@Override
	public void addSignee(final StateCacheEntryInfo entryInfo, final SignaturePublicKeyInfo signaturePublicKeyInfo) {
		if (null==entryInfo) {
			throw new IllegalArgumentException("entryInfo is null");
		}
		if (null==signaturePublicKeyInfo) {
			throw new IllegalArgumentException("signaturePublicKeyInfo is null");
		}
		final Path targetPath=buildTargetPath(entryInfo);
		final Lock writeLock=getWriteLock(targetPath);
		writeLock.lock();
		try {
			final Set<PublicKeyInfo> existingEntries=getSignatureStorageCache().computeIfAbsent(entryInfo, k -> Sets.newConcurrentHashSet());
			final PublicKeyInfo publicKeyInfo=signaturePublicKeyInfo.getPublicKeyInfo();
			if (existingEntries.contains(publicKeyInfo)) {
				return;
			}

			//if (existingEntries.size()>=MAX_SIGNATURES&&null!=getIdentity(publicKeyInfo)) {
			//LOG.log(Level.INFO, "already have {0} unidentified for {1}", new Object[]{MAX_SIGNATURES, entryInfo});
			//}
			if (existingEntries.isEmpty()) {
				Files.createDirectories(targetPath);
			}
			final Path filePath=buildTargetPathFile(targetPath, publicKeyInfo);
			Files.write(filePath, signaturePublicKeyInfo.getSignature().getSignature());
			existingEntries.add(publicKeyInfoInterner.intern(publicKeyInfo));

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public Set<SignaturePublicKeyInfo> getSignatures(final StateCacheEntryInfo entryInfo) {
		final Path targetPath=buildTargetPath(entryInfo);
		final Lock readLock=getReadLock(targetPath);
		readLock.lock();
		try {
			final Set<PublicKeyInfo> signedBy=getSignedBy(entryInfo);
			if (null==signedBy||signedBy.isEmpty()) {
				return ImmutableSet.of();
			}
			ForkJoinTask<ImmutableSet<SignaturePublicKeyInfo>> task=getThreadPool().submit(()
					-> signedBy.parallelStream()
							.map(k -> readSignature(targetPath, k))
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

	private static SignaturePublicKeyInfo readSignature(final Path basePath, final PublicKeyInfo keyInfo) {
		final Path filePath=buildTargetPathFile(basePath, keyInfo);
		try (InputStream in=Files.newInputStream(filePath)) {
			final byte[] signature=ByteStreams.toByteArray(in);
			final Signature s=new Signature(signature);
			return new SignaturePublicKeyInfo(s, keyInfo);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "failed to read: "+keyInfo, e);
			throw new IllegalStateException("failed to read: "+keyInfo, e);
		}
	}

	private Path buildPublicKeyPath(final PublicKeyInfo keyInfo) {
		return buildTargetPathFile(keysPath, keyInfo);
	}

	@Override
	public PublicKey getPublicKey(final PublicKeyInfo keyInfo) {
		final PublicKeyInfo keyInfoInterned=publicKeyInfoInterner.intern(keyInfo);
		try {
			return publicKeyStorageCache.get(keyInfoInterned, () -> {
				final Path keyPath=buildPublicKeyPath(keyInfoInterned);
				final Lock readLock=getReadLock(keyPath);
				readLock.lock();
				try (InputStream in=Files.newInputStream(keyPath)) {
					byte[] keyBytes=ByteStreams.toByteArray(in);
					return new PublicKey(keyBytes, keyInfoInterned);
				} finally {
					readLock.unlock();
				}
			});
		} catch (Exception ex) {
			LOG.log(Level.INFO, "no key found for: {0}", keyInfo);
			return null;
		}

	}

	@Override
	public void storePublicKey(final PublicKey publicKey) throws IOException {
		final PublicKeyInfo keyInfoInterned=publicKeyInfoInterner.intern(publicKey.getKeyInfo());
		if (null!=getPublicKey(keyInfoInterned)) {
			return;
		}
		final Path keyPath=buildPublicKeyPath(keyInfoInterned);
		final Lock writeLock=getWriteLock(keyPath);
		writeLock.lock();
		try (OutputStream out=Files.newOutputStream(keyPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) { // don't overwrite existing
			CryptoUtil.write(out, CryptoUtil.decodePublicKey(publicKey));
			final PublicKey keyInterned=new PublicKey(publicKey.getKey(), keyInfoInterned);
			publicKeyStorageCache.put(keyInfoInterned, keyInterned);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public Identity getIdentity(PublicKeyInfo keyInfo) {
		return identityStorage.getIdentity(keyInfo);
	}

	@Override
	public IdentityVerification getIdentityVerification(PublicKeyInfo publicKeyInfo) {
		return identityStorage.getIdentityVerification(publicKeyInfo);
	}

	@Override
	public void storeIdentity(IdentityWithVerification identityWithVerification) throws IOException {
		identityStorage.storeIdentity(identityWithVerification);
	}

	@Override
	public Set<PublicKeyInfo> getVerifiedKeyInfos() {
		return identityStorage.getVerifiedKeyInfos();
	}

	@Override
	public void close() throws IOException {
		if (null!=storageThreadPool) {
			MoreExecutors.shutdownAndAwaitTermination(storageThreadPool, 1, TimeUnit.MINUTES);
		}
	}
}
