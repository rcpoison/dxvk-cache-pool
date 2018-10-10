/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
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
	private static StateCache cache;
	private static ForkJoinPool forkJoinPool;

	public CacheStorageFSNGTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		forkJoinPool=new ForkJoinPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
		storagePath=Paths.get(System.getProperty("java.io.tmpdir")).resolve("dxvk-cache-pool").resolve(UUID.randomUUID().toString());
		cache=StateCacheIO.parse(new ByteArrayInputStream(TestUtil.readStateCacheData()));
		cache.setBaseName(BASE_NAME);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		MoreExecutors.shutdownAndAwaitTermination(forkJoinPool, 1, TimeUnit.MINUTES);
	}

	@BeforeMethod
	public void setUpMethod() throws Exception {
	}

	@AfterMethod
	public void tearDownMethod() throws Exception {
	}

	private static CacheStorageFS buildCacheStorage() {
		return new CacheStorageFS(storagePath, forkJoinPool);
	}

	@Test
	public void testInit() throws Exception {
		try (CacheStorageFS instance=buildCacheStorage()) {
			instance.init();
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testFindBaseNames() throws IOException {
		try (CacheStorageFS instance=buildCacheStorage()) {
			Set<String> findBaseNames=instance.findBaseNames(cache.getVersion(), "beat");
			assertEquals(findBaseNames, ImmutableSet.of(BASE_NAME));
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetMissingEntriesMissingNone() throws IOException {
		StateCacheInfo existingCache=cache.copy().toInfo();
		try (CacheStorageFS instance=buildCacheStorage()) {
			Set<StateCacheEntry> missingEntries=instance.getMissingEntries(existingCache);
			assertTrue(missingEntries.isEmpty());

		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetMissingEntriesMissingAll() throws IOException {
		try (CacheStorageFS instance=buildCacheStorage()) {
			StateCacheInfo empty=cache.copy().toInfo();
			empty.setEntries(ImmutableSet.of());
			Set<StateCacheEntry> missingEntriesForEmpty=instance.getMissingEntries(empty);
			assertEntrySize(missingEntriesForEmpty);
			assertEquals(missingEntriesForEmpty, cache.getEntries());
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetMissingEntriesMissingPartial() throws IOException {
		StateCacheInfo existingCache=cache.copy().toInfo();
		Set<StateCacheEntryInfo> entries=new HashSet<>(existingCache.getEntries());
		Iterator<StateCacheEntryInfo> iterator=entries.iterator();
		StateCacheEntryInfo missing=iterator.next();
		iterator.remove();
		existingCache.setEntries(entries);
		try (CacheStorageFS instance=buildCacheStorage()) {
			final Set<StateCacheEntry> missingEntries=instance.getMissingEntries(existingCache);
			assertEntrySize(missingEntries);
			Set<StateCacheEntryInfo> missingEntryInfos=missingEntries.stream()
					.map(StateCacheEntry::getEntryInfo)
					.collect(ImmutableSet.toImmutableSet());
			assertEquals(missingEntryInfos, ImmutableSet.of(missing));
			assertEquals(missingEntryInfos.size(), 1);
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetCacheDescriptor() throws IOException {
		try (CacheStorageFS instance=buildCacheStorage()) {
			StateCacheInfo result=instance.getCacheDescriptor(cache.getVersion(), BASE_NAME);
			assertEquals(result, cache.toInfo());
			assertEquals(result.getVersion(), cache.getVersion());
			assertEquals(result.getEntrySize(), cache.getEntrySize());
		}
	}

	@Test(dependsOnMethods={"testStore"})
	public void testGetCache() throws IOException {
		try (CacheStorageFS instance=buildCacheStorage()) {
			StateCache result=instance.getCache(cache.getVersion(), BASE_NAME);
			assertEquals(result, cache);
			assertEquals(result.getVersion(), cache.getVersion());
			assertEquals(result.getEntrySize(), cache.getEntrySize());
			assertEntrySize(cache.getEntries());
		}
	}

	private void assertEntrySize(Set<StateCacheEntry> cacheEntrys) {
		boolean collectEntrySize=cacheEntrys.stream()
				.allMatch(e -> cache.getEntrySize()==e.getEntry().length);
		assertTrue(collectEntrySize, "collectEntrySize");
	}

	@Test
	public void testStore() throws Exception {
		try (CacheStorageFS instance=buildCacheStorage()) {
			instance.store(cache);
			StateCache retrieved=instance.getCache(cache.getVersion(), BASE_NAME);
			assertEquals(retrieved, cache);
		}
	}

}
