/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;

/**
 *
 * @author poison
 */
public final class StateCacheHeaderInfo {

	private static final ImmutableMap<Integer, Integer> STATE_HEADER_VERSION_SIZE=ImmutableMap.<Integer, Integer>builder()
			.put(2, 1824)
			.build();

	private StateCacheHeaderInfo() {
	}

	public static Integer getEntrySize(int version) {
		return Optional.ofNullable(STATE_HEADER_VERSION_SIZE.get(version)).orElseThrow(() -> new IllegalArgumentException("unknown version: "+version));
	}

	public static ImmutableSet<Integer> getKnownVersions() {
		return STATE_HEADER_VERSION_SIZE.keySet();
	}

	public static int getLatestVersion() {
		return STATE_HEADER_VERSION_SIZE.keySet().stream()
				.mapToInt(Integer::intValue)
				.max()
				.getAsInt();
	}

}
