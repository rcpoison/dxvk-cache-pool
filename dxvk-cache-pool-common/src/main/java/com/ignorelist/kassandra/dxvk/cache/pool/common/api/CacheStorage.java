/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheMeta;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface CacheStorage extends Closeable {

	StateCacheInfo getCacheDescriptor(int version, String baseName);

	StateCache getCache(int version, String baseName);

	void store(StateCache cache) throws IOException;

	Set<StateCacheEntry> getMissingEntries(StateCacheInfo existingCache);

	Set<String> findBaseNames(int version, String subString);

	Set<StateCacheInfo> getCacheDescriptors(int version, Set<String> baseNames);

	Set<StateCacheEntry> getCacheEntries(final StateCacheMeta cacheMeta, final Set<StateCacheEntryInfo> cacheEntryInfos);

	Set<String> getAvilableBaseNames(final int version, final Set<String> baseNames);

}
