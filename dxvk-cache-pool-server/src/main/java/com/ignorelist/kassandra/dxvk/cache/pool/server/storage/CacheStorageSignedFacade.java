/*public
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorageSigned;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultiset;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.PredicatePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignatureCount;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheMeta;
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

	public StateCacheInfoSignees getCacheDescriptorSignees(final int version, final String baseName, final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned) {
		final StateCacheInfo cacheDescriptor=cacheStorage.getCacheDescriptor(version, baseName);
		if (null==cacheDescriptor) {
			return null;
		}
		StateCacheInfoSignees cacheInfoSignees=new StateCacheInfoSignees();
		cacheDescriptor.copyShallowTo(cacheInfoSignees);
		final Set<StateCacheEntryInfo> cacheInfos=cacheDescriptor.getEntries();

		ImmutableSet<StateCacheEntryInfoSignees> entriesSignees=buildCacheEntryInfosSignees(predicateStateCacheEntrySigned, cacheInfos);
		cacheInfoSignees.setEntries(entriesSignees);
		return cacheInfoSignees;
	}

	/**
	 * build filtered entries with signees
	 *
	 * @param predicateStateCacheEntrySigned predicate to apply to cache infos
	 * @param cacheInfos
	 * @return
	 */
	private ImmutableSet<StateCacheEntryInfoSignees> buildCacheEntryInfosSignees(final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned, final Set<StateCacheEntryInfo> cacheInfos) {
		Stopwatch stopwatch=Stopwatch.createStarted();
		final PredicatePublicKeyInfo predicatePublicKeyInfo=PredicatePublicKeyInfo.buildFrom(signatureStorage, predicateStateCacheEntrySigned);
		final int signatureLimit;
		if (null!=predicateStateCacheEntrySigned.getMinimumSignatures()&&null!=predicateStateCacheEntrySigned.getMinimumSignatures().getMinimumSignatures()) {
			signatureLimit=predicateStateCacheEntrySigned.getMinimumSignatures().getMinimumSignatures();
		} else {
			signatureLimit=PredicateStateCacheEntrySigned.DEFAULT_SIGNATURE_MINIMUM;
		}
		final ImmutableSet<StateCacheEntryInfoSignees> entriesSignees=cacheInfos.parallelStream()
				.map(e -> new StateCacheEntryInfoSignees(e, getPublicKeyInfosFiltered(predicatePublicKeyInfo, signatureLimit, e)))
				.filter(predicateStateCacheEntrySigned)
				.collect(ImmutableSet.toImmutableSet());

		stopwatch.stop();
		LOG.log(Level.INFO, "built {0} filtered entries in {1}ms", new Object[]{entriesSignees.size(), stopwatch.elapsed().toMillis()});
		return entriesSignees;
	}

	private Set<PublicKeyInfo> getPublicKeyInfosFiltered(final PredicatePublicKeyInfo predicatePublicKeyInfo, final int signatureLimit, final StateCacheEntryInfo stateCacheEntryInfo) {
		return signatureStorage.getSignedBy(stateCacheEntryInfo).stream()
				.filter(predicatePublicKeyInfo)
				.limit(signatureLimit)
				.collect(ImmutableSet.toImmutableSet());
	}

	@Override
	public StateCacheInfoSignees getCacheDescriptorSignees(final int version, final String baseName) {
		return getCacheDescriptorSignees(version, baseName, new PredicateStateCacheEntrySigned());
	}

	@Override
	public StateCacheSigned getCacheSigned(final int version, final String baseName) {
		return getCacheSigned(version, baseName, new PredicateStateCacheEntrySigned());
	}

	@Override
	public StateCacheSigned getCacheSigned(final int version, final String baseName, final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned) {
		Stopwatch stopwatch=Stopwatch.createStarted();
		final StateCacheInfoSignees cacheDescriptorSignees=getCacheDescriptorSignees(version, baseName, predicateStateCacheEntrySigned);
		if (null==cacheDescriptorSignees) {
			return null;
		}
		StateCacheSigned cacheSigned=new StateCacheSigned();
		cacheDescriptorSignees.copyShallowTo(cacheSigned);
		final Set<StateCacheEntryInfoSignees> entryInfosSignees=cacheDescriptorSignees.getEntries();

		ImmutableSet<StateCacheEntrySigned> signedEntries=buildSignedEntries(cacheSigned, entryInfosSignees);

		cacheSigned.setEntries(signedEntries);

		final ImmutableSet<PublicKey> usedPublicKeys=getUsedPublicKeys(signedEntries);
		cacheSigned.setPublicKeys(usedPublicKeys);

		int signatureCount=StateCacheEntrySigned.countTotalSignatures(signedEntries);
		stopwatch.stop();

		LOG.log(Level.INFO, "{0}: read {1} entries with {2} signatures in {3}ms", new Object[]{baseName, signedEntries.size(), signatureCount, stopwatch.elapsed().toMillis()});

		return cacheSigned;
	}

	private ImmutableSet<StateCacheEntrySigned> buildSignedEntries(final StateCacheMeta cacheMeta, final Set<StateCacheEntryInfoSignees> entryInfosSignees) {
		final ImmutableSet<StateCacheEntryInfo> cacheEntryInfos=entryInfosSignees.stream()
				.map(StateCacheEntryInfoSignees::getEntryInfo)
				.collect(ImmutableSet.toImmutableSet());
		final Set<StateCacheEntry> cacheEntries=cacheStorage.getCacheEntries(cacheMeta, cacheEntryInfos);
		final ImmutableMap<StateCacheEntryInfo, StateCacheEntry> entriesByInfo=Maps.uniqueIndex(cacheEntries, StateCacheEntry::getEntryInfo);
		final ImmutableSet<StateCacheEntrySigned> signedEntries=entryInfosSignees.parallelStream()
				//.filter(iS -> null!=entriesByInfo.get(iS.getEntryInfo()))
				.map(iS -> new StateCacheEntrySigned(entriesByInfo.get(iS.getEntryInfo()), signatureStorage.getSignatures(iS.getEntryInfo(), iS.getPublicKeyInfos())))
				.collect(ImmutableSet.toImmutableSet());
		return signedEntries;
	}

	private ImmutableSet<PublicKey> getUsedPublicKeys(final ImmutableSet<StateCacheEntrySigned> signedEntries) {
		final ImmutableSet<PublicKey> usedPublicKeys=StateCacheEntrySigned.getUsedPublicKeyInfos(signedEntries).parallelStream()
				.map(signatureStorage::getPublicKey)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
		return usedPublicKeys;
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
		Stopwatch stopwatch=Stopwatch.createStarted();
		final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned=null==existingCache.getPredicateStateCacheEntrySigned() ? new PredicateStateCacheEntrySigned() : existingCache.getPredicateStateCacheEntrySigned();
		final StateCacheInfo cacheDescriptor=cacheStorage.getCacheDescriptor(existingCache.getVersion(), existingCache.getBaseName());
		if (null==cacheDescriptor) {
			return null;
		}
		final ImmutableSet<StateCacheEntryInfo> missingEntries=cacheDescriptor.getMissingEntries(existingCache);
		final ImmutableSet<StateCacheEntryInfoSignees> missingEntriesSignees=buildCacheEntryInfosSignees(predicateStateCacheEntrySigned, missingEntries);

		final ImmutableSet<StateCacheEntrySigned> signedEntries=buildSignedEntries(cacheDescriptor, missingEntriesSignees);
		stopwatch.stop();

		int signatureCount=StateCacheEntrySigned.countTotalSignatures(signedEntries);

		LOG.log(Level.INFO, "{0}: read {1} entries with {2} signatures in {3}ms", new Object[]{cacheDescriptor.getBaseName(), signedEntries.size(), signatureCount, stopwatch.elapsed().toMillis()});
		return signedEntries;
	}


	@Override
	public Set<StateCacheInfoSignees> getCacheDescriptorsSignees(int version, Set<String> baseNames) {
		return baseNames.parallelStream()
				.map(bN -> getCacheDescriptorSignees(version, bN))
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
	}
	
	public Set<SignatureCount> getTotalSignatureCounts(final int version) {
		final TreeMultiset<Integer> signatureCounts=cacheStorage.findBaseNames(version, null).stream()
				.map(bN -> cacheStorage.getCacheDescriptor(version, bN))
				.map(StateCacheInfo::getEntries)
				.flatMap(Set::stream)
				.map(signatureStorage::getSignedBy)
				.map(Set::size)
				.collect(Collectors.toCollection(TreeMultiset::create));
		return SignatureCount.build(signatureCounts);
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
