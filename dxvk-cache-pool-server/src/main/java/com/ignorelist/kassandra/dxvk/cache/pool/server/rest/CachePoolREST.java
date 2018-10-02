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
public class CachePoolREST implements CacheStorage {

	@Inject
	private Configuration configuration;
	@Inject
	private CacheStorage cacheStorage;

	@POST
	@Path("cacheDescriptors/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Set<StateCacheInfo> getCacheDescriptors(@PathParam("version") int version, Set<String> baseNames) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseNames) {
			throw new IllegalArgumentException("missing executableInfos");
		}
		return baseNames.parallelStream()
				.map(bN -> cacheStorage.getCacheDescriptor(version, bN))
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
	}

	@POST
	@Path("cacheDescriptor/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheInfo getCacheDescriptor(@PathParam("version") int version, String baseName) {
		return Iterables.getOnlyElement(getCacheDescriptors(version, ImmutableSet.of(baseName)));
	}

	@POST
	@Path("stateCache/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
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
	@Path("missingCacheEntries")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheEntry> getMissingEntries(StateCacheInfo cacheInfo) {
		if (null==cacheInfo) {
			throw new IllegalArgumentException("missing cacheInfo");
		}
		StateCacheHeaderInfo.getEntrySize(cacheInfo.getVersion());
		return cacheStorage.getMissingEntries(cacheInfo);
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
		ImmutableSet<StateCacheEntry> entyCopies=dxvkStateCache.getEntries().parallelStream()
				.map(StateCacheEntry::copySafe)
				.collect(ImmutableSet.toImmutableSet());
		dxvkStateCache.setEntries(entyCopies);
		cacheStorage.store(dxvkStateCache);
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

	@Override
	public void close() throws IOException {
	}

}
