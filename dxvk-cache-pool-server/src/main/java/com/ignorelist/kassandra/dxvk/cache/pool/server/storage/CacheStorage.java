/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheDescriptor;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface CacheStorage {

	Set<DxvkStateCacheDescriptor> getCacheDescriptors();

	DxvkStateCacheDescriptor getCacheDescriptor(ExecutableInfo executableInfo);

	DxvkStateCache getCache(ExecutableInfo executableInfo);

	void store(DxvkStateCache cache) throws IOException;

}
