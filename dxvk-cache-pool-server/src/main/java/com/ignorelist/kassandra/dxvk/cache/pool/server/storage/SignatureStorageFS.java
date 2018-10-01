/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Striped;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Signature;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Simple storage for signatures and keys using the filesystem.
 *
 * storage layout: storageRoot / signatures / entry sha256[0-1] / entry sha256[2-] / publickey sha256
 *
 * @author poison
 */
public class SignatureStorageFS implements Closeable, SignatureStorage {

	private static final Logger LOG=Logger.getLogger(SignatureStorageFS.class.getName());
	private static final BaseEncoding BASE16=BaseEncoding.base16();

	private final Path storageRoot;
	private final Striped<ReadWriteLock> storageLock=Striped.lazyWeakReadWriteLock(64);
	private final Interner<PublicKeyInfo> publicKeyInfoInterner=Interners.newWeakInterner();
	private ConcurrentMap<StateCacheEntryInfo, Set<PublicKeyInfo>> storageCache;
	private ForkJoinPool storageThreadPool;
	

	public SignatureStorageFS(Path storageRoot) {
		this.storageRoot=storageRoot;
	}

	private synchronized ForkJoinPool getThreadPool() {
		if (null==storageThreadPool) {
			storageThreadPool=new ForkJoinPool(8);
		}
		return storageThreadPool;
	}

	private Lock getReadLock(StateCacheEntryInfo entryInfo) {
		final ReadWriteLock lock=storageLock.get(entryInfo);
		final Lock readLock=lock.readLock();
		return readLock;
	}

	private Lock getWriteLock(StateCacheEntryInfo entryInfo) {
		final ReadWriteLock lock=storageLock.get(entryInfo);
		final Lock writeLock=lock.writeLock();
		return writeLock;
	}

	private synchronized ConcurrentMap<StateCacheEntryInfo, Set<PublicKeyInfo>> getStorageCache() throws IOException {
		if (null==storageCache) {
			Files.createDirectories(storageRoot);
			final ImmutableSetMultimap<Path, Path> signatureFiles=Files.walk(storageRoot)
					.filter(p -> Util.SHA_256_HEX_PATTERN.matcher(p.getFileName().toString()).matches())
					.filter(Files::isRegularFile)
					.collect(ImmutableSetMultimap.toImmutableSetMultimap(Path::getParent, Functions.identity()));
			ConcurrentMap<StateCacheEntryInfo, Set<PublicKeyInfo>> m=new ConcurrentHashMap<>();
			signatureFiles.asMap().entrySet().parallelStream()
					.forEach(e -> {
						final Path entryHashFragment0=e.getKey().getParent().getFileName();
						final Path entryHashFragment1=e.getKey().getFileName();
						final byte[] entryHash=BASE16.decode(entryHashFragment0.toString()+entryHashFragment1.toString());
						StateCacheEntryInfo cacheEntryInfo=new StateCacheEntryInfo(entryHash);
						final Set<PublicKeyInfo> pubKeyInfo=e.getValue().stream()
								.map(Path::getFileName)
								.map(Path::toString)
								.map(BASE16::decode)
								.map(PublicKeyInfo::new)
								.map(publicKeyInfoInterner::intern)
								.collect(Collectors.toCollection(Sets::newConcurrentHashSet));
						m.put(cacheEntryInfo, pubKeyInfo);
					});
			storageCache=m;
		}
		return storageCache;
	}

	@Override
	public Set<PublicKeyInfo> getSignedBy(final StateCacheEntryInfo entryInfo) {
		throw new UnsupportedOperationException();
	}

	public void setSignedBy(final StateCacheEntryInfo entryInfo, final Signature signature) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Signature> getSignatures(final StateCacheEntryInfo entryInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PublicKey getPublicKey(final PublicKeyInfo keyInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Identity getIdentity(final PublicKeyInfo keyInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws IOException {
		if (null!=storageThreadPool) {
			MoreExecutors.shutdownAndAwaitTermination(storageThreadPool, 1, TimeUnit.MINUTES);
		}
	}
}
