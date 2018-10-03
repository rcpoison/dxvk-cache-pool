/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.rest;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.validators.StateCacheValidator;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorageSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.server.Configuration;
import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author poison
 */
@Path("pool")
public class CachePoolREST implements CacheStorage, CacheStorageSigned {

	@Inject
	private Configuration configuration;
	@Inject
	private CacheStorage cacheStorage;
	@Inject
	private SignatureStorage signatureStorage;
	@Inject
	private CacheStorageSigned cacheStorageSigned;

	@POST
	@Path("cacheDescriptors/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheInfo> getCacheDescriptors(@PathParam("version") int version, Set<String> baseNames) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseNames) {
			throw new IllegalArgumentException("missing executableInfos");
		}
		return cacheStorage.getCacheDescriptors(version, baseNames);
	}

	@POST
	@Path("cacheDescriptor/{version}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheInfo getCacheDescriptor(@PathParam("version") int version, String baseName) {
		return Iterables.getOnlyElement(getCacheDescriptors(version, ImmutableSet.of(baseName)));
	}

	@POST
	@Path("cacheDescriptorsSignees/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Set<StateCacheInfoSignees> getCacheDescriptorsSignees(@PathParam("version") int version, Set<String> baseNames) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseNames) {
			throw new IllegalArgumentException("missing executableInfos");
		}
		return baseNames.parallelStream()
				.map(bN -> cacheStorageSigned.getCacheDescriptorSignees(version, bN))
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
	}

	@POST
	@Path("cacheDescriptorSignees/{version}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheInfoSignees getCacheDescriptorSignees(@PathParam("version") int version, String baseName) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseName) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorageSigned.getCacheDescriptorSignees(version, baseName);
	}

	@POST
	@Path("stateCache/{version}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCache getCache(@PathParam("version") int version, String baseName) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseName) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorage.getCache(version, baseName);
	}

	@POST
	@Path("stateCacheSigned/{version}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheSigned getCacheSigned(@PathParam("version") int version, String baseName) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseName) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorageSigned.getCacheSigned(version, baseName);
	}

	@POST
	@Path("missingCacheEntries")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheEntry> getMissingEntries(StateCacheInfo cacheInfo) {
		StateCacheHeaderInfo.getEntrySize(cacheInfo.getVersion());
		if (null==cacheInfo) {
			throw new IllegalArgumentException("missing cacheInfo");
		}
		return cacheStorage.getMissingEntries(cacheInfo);
	}

	@POST
	@Path("missingCacheEntriesSigned")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheEntrySigned> getMissingEntriesSigned(StateCacheInfo cacheInfo) {
		StateCacheHeaderInfo.getEntrySize(cacheInfo.getVersion());
		if (null==cacheInfo) {
			throw new IllegalArgumentException("missing cacheInfo");
		}
		return cacheStorageSigned.getMissingEntriesSigned(cacheInfo);
	}

	@POST
	@Path("store")
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public void store(StateCache dxvkStateCache) throws IOException {
		if (null==dxvkStateCache) {
			throw new IllegalArgumentException("missing dxvkStateCache");
		}
		new StateCacheValidator().validate(dxvkStateCache);
		// don't trust passed hashes, just rebuild the entries
		final ImmutableSet<StateCacheEntry> entyCopies=dxvkStateCache.getEntries().parallelStream()
				.map(StateCacheEntry::copySafe)
				.collect(ImmutableSet.toImmutableSet());
		dxvkStateCache.setEntries(entyCopies);
		cacheStorage.store(dxvkStateCache);
	}

	@POST
	@Path("storeSigned")
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public void storeSigned(StateCacheSigned cache) throws IOException {
		if (null==cache) {
			throw new IllegalArgumentException("missing cache");
		}
		new StateCacheValidator().validate(cache);
		cacheStorageSigned.storeSigned(cache);
	}

	@GET
	@Path("find/{version}/{subString}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<String> findBaseNames(@PathParam("version") int version, @PathParam("subString") String subString) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (Strings.isNullOrEmpty(subString)) {
			throw new IllegalArgumentException("search string must not be empty");
		}
		return cacheStorage.findBaseNames(version, subString);
	}

	@POST
	@Path("publicKey")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PublicKey getPublicKey(PublicKeyInfo keyInfo) {
		if (null==keyInfo) {
			throw new IllegalArgumentException("missing keyInfo");
		}
		return signatureStorage.getPublicKey(keyInfo);
	}

	@POST
	@Path("publicKeys")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Set<PublicKey> getPublicKeys(Set<PublicKeyInfo> keyInfo) {
		if (null==keyInfo) {
			throw new IllegalArgumentException("missing keyInfo");
		}
		return keyInfo.parallelStream()
				.map(signatureStorage::getPublicKey)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
	}

	@Override
	public void close() throws IOException {
	}

}
