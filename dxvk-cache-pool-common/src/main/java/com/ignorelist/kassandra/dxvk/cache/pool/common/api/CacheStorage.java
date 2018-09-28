/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface CacheStorage {

	DxvkStateCacheInfo getCacheDescriptor(int version, ExecutableInfo executableInfo);

	DxvkStateCache getCache(int version, ExecutableInfo executableInfo);

	void store(DxvkStateCache cache) throws IOException;

	Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCacheInfo existingCache);

	Set<ExecutableInfo> findExecutables(int version, String subString);

}
