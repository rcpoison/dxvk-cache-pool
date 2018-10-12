/*public
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.ignorelist.kassandra.dxvk.cache.pool.common.api.PredicateSignature;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorageSigned;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.TreeMultiset;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignatureCount;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author poison
 */
public class CacheStorageSignedFacade implements CacheStorageSigned {

	private static final Logger LOG=Logger.getLogger(CacheStorageSignedFacade.class.getName());

	private final CacheStorage cacheStorage;
	private final SignatureStorage signatureStorage;

	public CacheStorageSignedFacade(CacheStorage cacheStorage, SignatureStorage signatureStorage) {
		this.cacheStorage=cacheStorage;
		this.signatureStorage=signatureStorage;
	}

	@Override
	public StateCacheInfoSignees getCacheDescriptorSignees(int version, String baseName) {
		// TODO: predicate
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

	@Override
	public StateCacheSigned getCacheSigned(final int version, final String baseName) {
		return getCacheSigned(version, baseName, new PredicateStateCacheEntrySigned());
	}

	@Override
	public StateCacheSigned getCacheSigned(final int version, final String baseName, final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned) {
		final StateCache cache=cacheStorage.getCache(version, baseName);
		if (null==cache) {
			return null;
		}
		StateCacheSigned cacheSigned=new StateCacheSigned();
		cache.copyShallowTo(cacheSigned);
		// TODO: optimize: only read entries matching signature predicate
		final ImmutableSet<StateCacheEntrySigned> signedEntries=buildSignedEntries(cache.getEntries(), predicateStateCacheEntrySigned);
		cacheSigned.setEntries(signedEntries);
		final ImmutableSet<PublicKey> usedPublicKeys=getUsedPublicKeys(signedEntries);
		cacheSigned.setPublicKeys(usedPublicKeys);
		return cacheSigned;
	}

	private ImmutableSet<PublicKey> getUsedPublicKeys(final ImmutableSet<StateCacheEntrySigned> signedEntries) {
		final ImmutableSet<PublicKey> usedPublicKeys=StateCacheEntrySigned.getUsedPublicKeyInfos(signedEntries).parallelStream()
				.map(signatureStorage::getPublicKey)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
		return usedPublicKeys;
	}

	private ImmutableSet<StateCacheEntrySigned> buildSignedEntries(final Set<StateCacheEntry> entries, final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned) {
		final PredicateSignature signaturePredicate=PredicateSignature.buildFrom(signatureStorage, predicateStateCacheEntrySigned);
		return entries.parallelStream()
				.map(e -> new StateCacheEntrySigned(e, getSignaturesFiltered(signaturePredicate, e.getEntryInfo())))
				.filter(predicateStateCacheEntrySigned)
				.collect(ImmutableSet.toImmutableSet());
	}

	private ImmutableSet<SignaturePublicKeyInfo> getSignaturesFiltered(final PredicateSignature signaturePredicate, final StateCacheEntryInfo entryInfo) {
		final Iterable<SignaturePublicKeyInfo> filteredSignatures=Iterables.filter(signatureStorage.getSignatures(entryInfo), signaturePredicate);
		return ImmutableSet.copyOf(filteredSignatures);
	}

	@Override
	public void storeSigned(StateCacheSigned cache) throws IOException {
		try {
			final PublicKey publicKeyUntrustedInfo=Iterables.getOnlyElement(cache.getPublicKeys()); // submission must only have asingle public key attached
			// do not trust key info, rebuild
			final PublicKey publicKey=new PublicKey(publicKeyUntrustedInfo.getKey());
			signatureStorage.storePublicKey(publicKey);

			final ImmutableMap<PublicKeyInfo, java.security.PublicKey> keyByInfo=ImmutableMap.of(publicKey.getKeyInfo(), CryptoUtil.decodePublicKey(publicKey.getKey()));

			Stopwatch stopwatch=Stopwatch.createStarted();
			final ImmutableSet<StateCacheEntrySigned> verifiedEntries=cache.getEntries().parallelStream()
					.map(StateCacheEntrySigned::copySafe) // do not trust info, rebuild
					.filter(e -> 1==e.getSignatures().size())
					.filter(e -> 1==e.verifiedSignatures(keyByInfo::get).size())
					.collect(ImmutableSet.toImmutableSet());
			stopwatch.stop();
			LOG.log(Level.INFO, "{0}: verified {1} entries in {2}ms", new Object[]{cache.getBaseName(), verifiedEntries.size(), stopwatch.elapsed().toMillis()});
			stopwatch.reset();

			stopwatch.start();
			verifiedEntries.parallelStream()
					.forEach(e -> signatureStorage.addSignee(e.getCacheEntry().getEntryInfo(), Iterables.getOnlyElement(e.getSignatures())));
			stopwatch.stop();
			LOG.log(Level.INFO, "{0}: stored signatures for {1} entries in {2}ms", new Object[]{cache.getBaseName(), verifiedEntries.size(), stopwatch.elapsed().toMillis()});

			StateCache unsigned=new StateCache();
			cache.copyShallowTo(unsigned);
			final ImmutableSet<StateCacheEntry> verifiedEntriesUnsigned=verifiedEntries.stream()
					.map(StateCacheEntrySigned::getCacheEntry)
					.collect(ImmutableSet.toImmutableSet());
			unsigned.setEntries(verifiedEntriesUnsigned);
			cacheStorage.store(unsigned);
		} catch (IOException e) {
			throw e;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public Set<StateCacheEntrySigned> getMissingEntriesSigned(final StateCacheInfo existingCache) {
		final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned=null==existingCache.getPredicateStateCacheEntrySigned() ? new PredicateStateCacheEntrySigned() : existingCache.getPredicateStateCacheEntrySigned();
		final Set<StateCacheEntry> missingEntries=cacheStorage.getMissingEntries(existingCache);
		return buildSignedEntries(missingEntries, predicateStateCacheEntrySigned);

	}

	@Override
	public Set<StateCacheInfoSignees> getCacheDescriptorsSignees(int version, Set<String> baseNames) {
		return baseNames.parallelStream()
				.map(bN -> getCacheDescriptorSignees(version, bN))
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
	}
	
	@Override
	public Set<SignatureCount> getSignatureCounts(final int version, final String baseName) {
		TreeMultiset<Integer> signatureCounts=cacheStorage.getCacheDescriptor(version, baseName).getEntries().stream()
				.map(signatureStorage::getSignedBy)
				.map(Set::size)
				.collect(Collectors.toCollection(TreeMultiset::create));
		return SignatureCount.build(signatureCounts);
	}
	
	private HashMultiset<PublicKeyInfo> buildSigneeSignatureCount(final int version, final String baseName) {
		return cacheStorage.getCacheDescriptor(version, baseName).getEntries().stream()
				.map(signatureStorage::getSignedBy)
				.flatMap(Set::stream)
				.collect(Collectors.toCollection(HashMultiset::create));
		
	}

}
