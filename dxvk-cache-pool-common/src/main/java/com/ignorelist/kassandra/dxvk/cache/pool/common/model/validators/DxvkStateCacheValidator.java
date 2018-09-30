/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model.validators;

import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;

/**
 *
 * @author poison
 */
public class DxvkStateCacheValidator {

	public void validate(StateCache cache) {
		final int entrySize=StateCacheHeaderInfo.getEntrySize(cache.getVersion());
		if (entrySize!=cache.getEntrySize()) {
			throw new IllegalArgumentException("expected entrySize:"+entrySize+", got:"+cache.getEntrySize());
		}
		boolean allValidLength=cache.getEntries().stream()
				.mapToInt(e -> e.getEntry().length)
				.allMatch(l -> l==entrySize);
		if (!allValidLength) {
			throw new IllegalArgumentException("illegal entry size found");
		}

		if (!Util.isSafeBaseName(cache.getBaseName())) {
			throw new IllegalArgumentException("illegal baseName: "+cache.getBaseName());
		}

	}
}
