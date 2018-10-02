/*public
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public class CacheStorageSignedFacade {

	private final CacheStorage cacheStorage;
	private final SignatureStorage signatureStorage;

	public CacheStorageSignedFacade(CacheStorage cacheStorage, SignatureStorage signatureStorage) {
		this.cacheStorage=cacheStorage;
		this.signatureStorage=signatureStorage;
	}

	public StateCacheEntryInfoSignees getCacheDescriptor(int version, String baseName) {
		throw new UnsupportedOperationException();
	}

	public StateCacheSigned getCache(int version, String baseName) {
		final StateCache cache=cacheStorage.getCache(version, baseName);
		if (null==cache) {
			return null;
		}
		StateCacheSigned cacheSigned=new StateCacheSigned();
		cache.copyShallowTo(cacheSigned);
		final ImmutableSet<StateCacheEntrySigned> signedEntries=cache.getEntries().parallelStream()
				.map(e -> new StateCacheEntrySigned(e, signatureStorage.getSignatures(e.getEntryInfo())))
				.collect(ImmutableSet.toImmutableSet());
		cacheSigned.setEntries(signedEntries);
		final ImmutableSet<PublicKey> usedPublicKeys=signedEntries.parallelStream()
				.map(StateCacheEntrySigned::getSignatures)
				.flatMap(Set::stream)
				.map(SignaturePublicKeyInfo::getPublicKeyInfo)
				.distinct()
				.map(signatureStorage::getPublicKey)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
		cacheSigned.setPublicKeys(usedPublicKeys);
		return cacheSigned;
	}

	public void store(StateCacheSigned cache) throws IOException {
		throw new UnsupportedOperationException();
	}

	public Set<StateCacheEntrySigned> getMissingEntries(StateCacheInfo existingCache) {
		throw new UnsupportedOperationException();
	}

	public Set<String> findBaseNames(int version, String subString) {
		return cacheStorage.findBaseNames(version, subString);
	}

}
