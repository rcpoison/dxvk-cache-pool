/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface CacheStorageSigned {

	StateCacheInfoSignees getCacheDescriptorSignees(int version, String baseName);

	StateCacheSigned getCacheSigned(final int version, final String baseName);

	StateCacheSigned getCacheSigned(final int version, final String baseName, final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned);

	Set<StateCacheEntrySigned> getMissingEntriesSigned(final StateCacheInfo existingCache);

	void storeSigned(StateCacheSigned cache) throws IOException;

	Set<StateCacheInfoSignees> getCacheDescriptorsSignees(int version, Set<String> baseNames);

}
