/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.DxvkStateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author poison
 */
public class CacheStorageFSNGTest {

	private static final String BASE_NAME="Beat Saber";
	private static Path storagePath;
	private static DxvkStateCache cache;

	public CacheStorageFSNGTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		storagePath=Paths.get(System.getProperty("java.io.tmpdir")).resolve("dxvk-cache-pool").resolve(UUID.randomUUID().toString());
		cache=DxvkStateCacheIO.parse(new ByteArrayInputStream(TestUtil.readStateCacheData()));
		cache.setBaseName(BASE_NAME);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@BeforeMethod
	public void setUpMethod() throws Exception {
	}

	@AfterMethod
	public void tearDownMethod() throws Exception {
	}

	@Test
	public void testInit() throws Exception {
		try (CacheStorageFS instance=new CacheStorageFS(storagePath)) {
			instance.init();
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testFindBaseNames() throws IOException {
		try (CacheStorageFS instance=new CacheStorageFS(storagePath)) {
			Set<String> findBaseNames=instance.findBaseNames(cache.getVersion(), "beat");
			assertEquals(findBaseNames, ImmutableSet.of(BASE_NAME));
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetMissingEntries() throws IOException {
		DxvkStateCacheInfo existingCache=cache.copy().toInfo();

		try (CacheStorageFS instance=new CacheStorageFS(storagePath)) {
			Set<DxvkStateCacheEntry> missingEntries=instance.getMissingEntries(existingCache);
			assertTrue(missingEntries.isEmpty());

			DxvkStateCacheInfo empty=cache.copy().toInfo();
			empty.setEntries(ImmutableSet.of());
			Set<DxvkStateCacheEntry> missingEntriesForEmpty=instance.getMissingEntries(empty);
			System.err.println(missingEntriesForEmpty.size());
			assertEquals(missingEntriesForEmpty, cache.getEntries());
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetCacheDescriptor() throws IOException {
		try (CacheStorageFS instance=new CacheStorageFS(storagePath)) {
			DxvkStateCacheInfo result=instance.getCacheDescriptor(cache.getVersion(), BASE_NAME);
			assertEquals(result, cache.toInfo());
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetCache() throws IOException {
		try (CacheStorageFS instance=new CacheStorageFS(storagePath)) {
			DxvkStateCache result=instance.getCache(cache.getVersion(), BASE_NAME);
			assertEquals(result, cache);
		}
	}

	@Test
	public void testStore() throws Exception {
		try (CacheStorageFS instance=new CacheStorageFS(storagePath)) {
			instance.store(cache);
			DxvkStateCache retrieved=instance.getCache(cache.getVersion(), BASE_NAME);
			assertEquals(retrieved, cache);
		}
	}

}