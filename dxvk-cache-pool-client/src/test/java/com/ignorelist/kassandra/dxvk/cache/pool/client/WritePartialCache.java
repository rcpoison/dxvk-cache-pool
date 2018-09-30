/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;

/**
 *
 * @author poison
 */
public class WritePartialCache {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		StateCache cache=StateCacheIO.parse(new ByteArrayInputStream(TestUtil.readStateCacheData()));
		ImmutableSet<StateCacheEntry> entries=cache.getEntries().stream()
				.limit(cache.getEntries().size()-32)
				.collect(ImmutableSet.toImmutableSet());
		cache.setEntries(entries);
		StateCacheIO.write(Paths.get("/tmp/target/Beat Saber.dxvk-cache"), cache);
	}
	
}
