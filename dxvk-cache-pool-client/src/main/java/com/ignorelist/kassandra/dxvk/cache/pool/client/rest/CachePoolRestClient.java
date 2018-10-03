/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client.rest;

import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorageSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import java.io.IOException;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author poison
 */
public class CachePoolRestClient extends AbstractRestClient implements CacheStorage, CacheStorageSigned {

	private static final String PATH="pool";

	private static final GenericType<Set<String>> TYPE_STRING_SETn=new GenericType<Set<String>>() {
	};
	private static final GenericType<Set<StateCacheInfo>> TYPE_CACHE_INFO_SET=new GenericType<Set<StateCacheInfo>>() {
	};
	private static final GenericType<Set<StateCacheInfoSignees>> TYPE_CACHE_INFO_SIGNEES_SET=new GenericType<Set<StateCacheInfoSignees>>() {
	};
	private static final GenericType<Set<StateCacheEntry>> TYPE_CACHE_ENTRY_SET=new GenericType<Set<StateCacheEntry>>() {
	};
	private static final GenericType<Set<StateCacheEntrySigned>> TYPE_CACHE_ENTRY_SIGNED_SET=new GenericType<Set<StateCacheEntrySigned>>() {
	};

	public CachePoolRestClient(String baseUrl) {
		super(baseUrl);
	}

	@Override
	protected WebTarget getWebTarget() {
		return super.getWebTarget().path(PATH);
	}

	@Override
	public Set<StateCacheInfo> getCacheDescriptors(int version, Set<String> baseNames) {
		return getWebTarget()
				.path("cacheDescriptors")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(baseNames), TYPE_CACHE_INFO_SET);
	}

	@Override
	public StateCacheInfo getCacheDescriptor(int version, String baseName) {
		return getWebTarget()
				.path("cacheDescriptor")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.text(baseName), StateCacheInfo.class);
	}

	@Override
	public StateCache getCache(int version, String baseName) {
		return getWebTarget()
				.path("stateCache")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.text(baseName), StateCache.class);
	}

	@Override
	public Set<StateCacheEntry> getMissingEntries(StateCacheInfo cacheInfo) {
		return getWebTarget()
				.path("missingCacheEntries")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(cacheInfo), TYPE_CACHE_ENTRY_SET);
	}

	@Override
	public void store(StateCache dxvkStateCache) {
		getWebTarget()
				.path("store")
				.request()
				.post(Entity.json(dxvkStateCache));
	}

	@Override
	public Set<String> findBaseNames(int version, String subString) {
		return getWebTarget()
				.path("cacheDescriptors")
				.path(Integer.toString(version))
				.path(subString)
				.request(MediaType.APPLICATION_JSON)
				.get(TYPE_STRING_SETn);
	}

	@Override
	public StateCacheInfoSignees getCacheDescriptorSignees(int version, String baseName) {
		return getWebTarget()
				.path("cacheDescriptorSignees")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.text(baseName), StateCacheInfoSignees.class);
	}

	@Override
	public Set<StateCacheInfoSignees> getCacheDescriptorsSignees(int version, Set<String> baseNames) {
		return getWebTarget()
				.path("cacheDescriptorsSignees")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(baseNames), TYPE_CACHE_INFO_SIGNEES_SET);
	}

	@Override
	public StateCacheSigned getCacheSigned(int version, String baseName) {
		return getWebTarget()
				.path("stateCacheSigned")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.text(baseName), StateCacheSigned.class);
	}

	@Override
	public Set<StateCacheEntrySigned> getMissingEntriesSigned(StateCacheInfo cacheInfo) {
		return getWebTarget()
				.path("missingCacheEntriesSigned")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(cacheInfo), TYPE_CACHE_ENTRY_SIGNED_SET);
	}

	@Override
	public void storeSigned(StateCacheSigned cache) throws IOException {
		getWebTarget()
				.path("store")
				.request()
				.post(Entity.json(cache));
	}

}
