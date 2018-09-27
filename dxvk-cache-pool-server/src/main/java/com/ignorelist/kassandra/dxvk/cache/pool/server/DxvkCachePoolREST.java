/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.DxvkStateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.validators.DxvkStateCacheValidator;
import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.CacheStorage;
import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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
public class DxvkCachePoolREST {

	@Inject
	private Configuration configuration;
	@Inject
	private CacheStorage cacheStorage;

	@POST
	@Path("cacheDescriptors/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Set<DxvkStateCacheInfo> getCacheDescriptors(@PathParam("version") int version, Set<ExecutableInfo> executableInfos) {
		DxvkStateCacheIO.getEntrySize(version);
		if (null==executableInfos) {
			throw new IllegalArgumentException("missing executableInfos");
		}
		return executableInfos.parallelStream()
				.filter(i -> Util.PREDICATE_EXE.apply(i.getPath()))
				.filter(i -> null!=i.getPath().getParent())
				.map(cacheStorage::getCacheDescriptor)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
	}

	@POST
	@Path("stateCache/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DxvkStateCache getStateCache(@PathParam("version") int version, ExecutableInfo executableInfo) {
		DxvkStateCacheIO.getEntrySize(version);
		if (null==executableInfo) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorage.getCache(executableInfo);
	}

	@POST
	@Path("missingCacheEntries")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCacheInfo cacheInfo) {
		if (null==cacheInfo) {
			throw new IllegalArgumentException("missing cacheInfo");
		}
		DxvkStateCacheIO.getEntrySize(cacheInfo.getVersion());
		return cacheStorage.getMissingEntries(cacheInfo);
	}

	@POST
	@Path("store")
	@Consumes(MediaType.APPLICATION_JSON)
	public void store(DxvkStateCache dxvkStateCache) throws IOException {
		if (null==dxvkStateCache) {
			throw new IllegalArgumentException("missing dxvkStateCache");
		}
		new DxvkStateCacheValidator().validate(dxvkStateCache);
		// don't trust passed hashes, just rebuild the entries
		ImmutableSet<DxvkStateCacheEntry> entyCopies=dxvkStateCache.getEntries().parallelStream()
				.map(DxvkStateCacheEntry::getEntry)
				.map(DxvkStateCacheEntry::new)
				.collect(ImmutableSet.toImmutableSet());
		dxvkStateCache.setEntries(entyCopies);
		cacheStorage.store(dxvkStateCache);
	}

}
