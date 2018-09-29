/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.DxvkStateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
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
		DxvkStateCache cache=DxvkStateCacheIO.parse(new ByteArrayInputStream(TestUtil.readStateCacheData()));
		ImmutableSet<DxvkStateCacheEntry> entries=cache.getEntries().stream()
				.limit(100)
				.collect(ImmutableSet.toImmutableSet());
		cache.setEntries(entries);
		DxvkStateCacheIO.write(Paths.get("/tmp/target/witcher3.dxvk-cache"), cache);
	}
	
}
