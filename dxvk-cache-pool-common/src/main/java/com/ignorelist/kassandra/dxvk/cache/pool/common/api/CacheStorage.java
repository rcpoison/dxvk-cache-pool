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
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface CacheStorage extends Closeable {

	DxvkStateCacheInfo getCacheDescriptor(int version, ExecutableInfo executableInfo);

	DxvkStateCache getCache(int version, ExecutableInfo executableInfo);

	void store(DxvkStateCache cache) throws IOException;

	Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCacheInfo existingCache);

	Set<ExecutableInfo> findExecutables(int version, String subString);

	/**
	 * Provides view of merged DxvkStateCacheEntryInfo's for all executables matching the passed baseName (case insensitive)
	 *
	 * @param version DXVK state cache version
	 * @param baseName base name (no directory or suffix)
	 * @return view of merged DxvkStateCacheEntryInfo's for all executables matching the passed baseName
	 */
	DxvkStateCacheInfo getCacheDescriptorForBaseName(final int version, final String baseName);

}
