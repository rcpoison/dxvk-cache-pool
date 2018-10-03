/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model.validators;

import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheMeta;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;

/**
 *
 * @author poison
 */
public class StateCacheValidator {

	public void validateMeta(StateCacheMeta cache) {
		final int entrySize=StateCacheHeaderInfo.getEntrySize(cache.getVersion());
		if (entrySize!=cache.getEntrySize()) {
			throw new IllegalArgumentException("expected entrySize:"+entrySize+", got:"+cache.getEntrySize());
		}
		if (!Util.isSafeBaseName(cache.getBaseName())) {
			throw new IllegalArgumentException("illegal baseName: "+cache.getBaseName());
		}
	}

	public void validate(StateCache cache) {
		validateMeta(cache);
		final int entrySize=StateCacheHeaderInfo.getEntrySize(cache.getVersion());
		boolean allValidLength=cache.getEntries().stream()
				.mapToInt(e -> e.getEntry().length)
				.allMatch(l -> l==entrySize);
		if (!allValidLength) {
			throw new IllegalArgumentException("illegal entry size found");
		}

	}

	public void validate(StateCacheSigned cache) {
		validateMeta(cache);
		final int entrySize=StateCacheHeaderInfo.getEntrySize(cache.getVersion());
		boolean allValidLength=cache.getEntries().stream()
				.map(StateCacheEntrySigned::getCacheEntry)
				.mapToInt(e -> e.getEntry().length)
				.allMatch(l -> l==entrySize);
		if (!allValidLength) {
			throw new IllegalArgumentException("illegal entry size found");
		}

	}
}
