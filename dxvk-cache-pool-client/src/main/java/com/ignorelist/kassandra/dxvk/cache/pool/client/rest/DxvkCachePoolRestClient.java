/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client.rest;

import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author poison
 */
public class DxvkCachePoolRestClient extends AbstractRestClient {

	private static final String PATH="pool";

	private static final GenericType<Set<ExecutableInfo>> TYPE_EXECUTABLE_INFO_SET=new GenericType<Set<ExecutableInfo>>() {
	};
	private static final GenericType<Set<DxvkStateCacheInfo>> TYPE_CACHE_INFO_SET=new GenericType<Set<DxvkStateCacheInfo>>() {
	};
	private static final GenericType<Set<DxvkStateCacheEntry>> TYPE_CACHE_ENTRY_SET=new GenericType<Set<DxvkStateCacheEntry>>() {
	};

	public DxvkCachePoolRestClient(String baseUrl) {
		super(baseUrl);
	}

	@Override
	protected WebTarget getWebTarget() {
		return super.getWebTarget().path(PATH);
	}

	public Set<DxvkStateCacheInfo> getCacheDescriptors(int version, Set<ExecutableInfo> executableInfos) {
		return getWebTarget()
				.path("cacheDescriptors")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(executableInfos, MediaType.APPLICATION_JSON), TYPE_CACHE_INFO_SET);
	}

	public DxvkStateCache getStateCache(int version, ExecutableInfo executableInfo) {
		return getWebTarget()
				.path("stateCache")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(executableInfo, MediaType.APPLICATION_JSON), DxvkStateCache.class);
	}

	public Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCacheInfo cacheInfo) {
		return getWebTarget()
				.path("missingCacheEntries")
				.request(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(cacheInfo, MediaType.APPLICATION_JSON), TYPE_CACHE_ENTRY_SET);
	}

	public void store(DxvkStateCache dxvkStateCache) {
		getWebTarget()
				.path("store")
				.request()
				.post(Entity.entity(dxvkStateCache, MediaType.APPLICATION_JSON));
	}

}
