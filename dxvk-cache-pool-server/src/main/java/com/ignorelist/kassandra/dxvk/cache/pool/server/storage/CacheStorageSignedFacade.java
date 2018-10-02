/*public
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
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

	public StateCacheInfoSignees getCacheDescriptor(int version, String baseName) {
		final StateCacheInfo cacheDescriptor=cacheStorage.getCacheDescriptor(version, baseName);
		if (null==cacheDescriptor) {
			return null;
		}
		StateCacheInfoSignees cacheInfoSignees=new StateCacheInfoSignees();
		cacheDescriptor.copyShallowTo(cacheInfoSignees);
		final ImmutableSet<StateCacheEntryInfoSignees> entriesSignees=cacheDescriptor.getEntries().parallelStream()
				.map(e -> new StateCacheEntryInfoSignees(e, signatureStorage.getSignedBy(e)))
				.collect(ImmutableSet.toImmutableSet());
		cacheInfoSignees.setEntries(entriesSignees);
		return cacheInfoSignees;
	}

	public StateCacheSigned getCache(final int version, final String baseName) {
		final StateCache cache=cacheStorage.getCache(version, baseName);
		if (null==cache) {
			return null;
		}
		StateCacheSigned cacheSigned=new StateCacheSigned();
		cache.copyShallowTo(cacheSigned);
		final ImmutableSet<StateCacheEntrySigned> signedEntries=buildSignedEntries(cache.getEntries());
		cacheSigned.setEntries(signedEntries);
		final ImmutableSet<PublicKey> usedPublicKeys=getUsedPublicKeys(signedEntries);
		cacheSigned.setPublicKeys(usedPublicKeys);
		return cacheSigned;
	}

	private ImmutableSet<PublicKey> getUsedPublicKeys(final ImmutableSet<StateCacheEntrySigned> signedEntries) {
		final ImmutableSet<PublicKey> usedPublicKeys=signedEntries.parallelStream()
				.map(StateCacheEntrySigned::getSignatures)
				.filter(Predicates.notNull())
				.flatMap(Set::stream)
				.map(SignaturePublicKeyInfo::getPublicKeyInfo)
				.distinct()
				.map(signatureStorage::getPublicKey)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
		return usedPublicKeys;
	}

	private ImmutableSet<StateCacheEntrySigned> buildSignedEntries(final Set<StateCacheEntry> entries) {
		return entries.parallelStream()
				.map(e -> new StateCacheEntrySigned(e, signatureStorage.getSignatures(e.getEntryInfo())))
				.collect(ImmutableSet.toImmutableSet());
	}

	public void store(StateCacheSigned cache) throws IOException {
		try {
			final PublicKey publicKeyUntrustedInfo=Iterables.getOnlyElement(cache.getPublicKeys()); // submission must only have asingle public key attached
			// do not trust key info, rebuild
			final PublicKey publicKey=new PublicKey(publicKeyUntrustedInfo.getKey());

			final ImmutableMap<PublicKeyInfo, java.security.PublicKey> keyByInfo=ImmutableMap.of(publicKey.getKeyInfo(), CryptoUtil.decodePublicKey(publicKey.getKey()));
			final ImmutableSet<StateCacheEntrySigned> verifiedEntries=cache.getEntries().parallelStream()
					.map(StateCacheEntrySigned::copySafe)
					.filter(e -> 1==e.getSignatures().size())
					.filter(e -> 1==e.verify(keyByInfo::get).size())
					.collect(ImmutableSet.toImmutableSet());
			verifiedEntries.parallelStream()
					.forEach(e -> signatureStorage.addSignee(e.getCacheEntry().getEntryInfo(), Iterables.getOnlyElement(e.getSignatures())));

			StateCache unsigned=new StateCache();
			cache.copyShallowTo(unsigned);
			final ImmutableSet<StateCacheEntry> verifiedEntriesUnsigned=verifiedEntries.stream()
					.map(StateCacheEntrySigned::getCacheEntry)
					.collect(ImmutableSet.toImmutableSet());
			unsigned.setEntries(verifiedEntriesUnsigned);
			cacheStorage.store(unsigned);;
		} catch (IOException e) {
			throw e;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

	public Set<StateCacheEntrySigned> getMissingEntries(final StateCacheInfo existingCache) {
		final Set<StateCacheEntry> missingEntries=cacheStorage.getMissingEntries(existingCache);
		return buildSignedEntries(missingEntries);
	}

	public Set<String> findBaseNames(int version, String subString) {
		return cacheStorage.findBaseNames(version, subString);
	}

}
