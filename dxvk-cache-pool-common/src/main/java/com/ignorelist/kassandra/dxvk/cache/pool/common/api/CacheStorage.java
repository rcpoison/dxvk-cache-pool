/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface CacheStorage extends Closeable {

	DxvkStateCacheInfo getCacheDescriptor(int version, String baseName);

	DxvkStateCache getCache(int version, String baseName);

	void store(DxvkStateCache cache) throws IOException;

	Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCacheInfo existingCache);

	Set<String> findExecutables(int version, String subString);

}
