/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateAcceptedPublicKeys;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateMinimumSignatures;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
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
public class CacheStorageSignedFacadeNGTest {

	private static final int VERSION=2;
	private static final String BASE_NAME="Beat Saber";

	private static StateCache cache;
	private static KeyPair keyPair0;
	private static KeyPair keyPair1;
	private static PublicKeyInfo publicKeyInfo0;
	private static PublicKeyInfo publicKeyInfo1;
	private static StateCacheSigned cacheSigned0;
	private static StateCacheSigned cacheSigned1;
	private static StateCacheSigned cacheSignedInvalid;
	private static Path storageRoot;
	private static CacheStorageFS cacheStorageShared;
	private static SignatureStorageFS SignatureStorageShared;
	private static CacheStorageSignedFacade cacheStorageSignedShared;

	private static final PredicateStateCacheEntrySigned predicateSignedOnce=new PredicateStateCacheEntrySigned(null, new PredicateMinimumSignatures(1));
	private static final PredicateStateCacheEntrySigned predicateSignedTwice=new PredicateStateCacheEntrySigned(null, new PredicateMinimumSignatures(2));
	private static PredicateStateCacheEntrySigned predicateSignedKey1;

	public CacheStorageSignedFacadeNGTest() throws Exception {

	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		storageRoot=Paths.get(System.getProperty("java.io.tmpdir")).resolve("dxvk-cache-pool-signed").resolve(UUID.randomUUID().toString());
		cache=StateCacheIO.parse(new ByteArrayInputStream(TestUtil.readStateCacheData()));
		cache.setBaseName(BASE_NAME);
		keyPair0=CryptoUtil.generate();
		keyPair1=CryptoUtil.generate();
		cacheSigned0=cache.sign(keyPair0.getPrivate(), new PublicKey(keyPair0.getPublic()));

		cacheSigned1=cache.sign(keyPair1.getPrivate(), new PublicKey(keyPair1.getPublic()));
		cacheSignedInvalid=cache.sign(keyPair1.getPrivate(), new PublicKey(keyPair0.getPublic()));
		cacheStorageShared=new CacheStorageFS(storageRoot.resolve("cache"));
		SignatureStorageShared=new SignatureStorageFS(storageRoot.resolve("signatures"));
		cacheStorageSignedShared=new CacheStorageSignedFacade(cacheStorageShared, SignatureStorageShared);
		publicKeyInfo0=new PublicKeyInfo(new PublicKey(keyPair0.getPublic()));
		publicKeyInfo1=new PublicKeyInfo(new PublicKey(keyPair1.getPublic()));
		predicateSignedKey1=new PredicateStateCacheEntrySigned(new PredicateAcceptedPublicKeys(ImmutableSet.of(publicKeyInfo1)), null);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		cacheStorageShared.close();
		SignatureStorageShared.close();
	}

	@BeforeMethod
	public void setUpMethod() throws Exception {
	}

	@AfterMethod
	public void tearDownMethod() throws Exception {
	}

	private void verifyNoResults() {

	}

	@Test
	public void testStoreSignedinvalid() throws Exception {
		cacheStorageSignedShared.storeSigned(cacheSignedInvalid);
		StateCache cacheFetched=cacheStorageShared.getCache(VERSION, BASE_NAME);
		assertTrue(cacheFetched.getEntries().isEmpty());

		StateCacheSigned cacheSigned=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME);
		assertTrue(cacheSigned.getEntries().isEmpty());

		StateCacheSigned cacheSignedVerifiedOnce=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME, predicateSignedOnce);
		assertTrue(cacheSignedVerifiedOnce.getEntries().isEmpty());

		StateCacheInfoSignees cacheDescriptorSignees=cacheStorageSignedShared.getCacheDescriptorSignees(VERSION, BASE_NAME);
		boolean noSignatures=cacheDescriptorSignees.getEntries().stream()
				.allMatch(e -> e.getPublicKeyInfos().isEmpty());
		assertTrue(noSignatures);
	}

	@Test(dependsOnMethods={"testStoreSignedinvalid"})
	public void testStoreSignedOnce() throws Exception {
		cacheStorageSignedShared.storeSigned(cacheSigned0);
		StateCache cacheFetched=cacheStorageShared.getCache(VERSION, BASE_NAME);
		assertEquals(cacheFetched, cache);

		StateCacheSigned cacheSigned=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME, predicateSignedOnce);
		assertEquals(cacheSigned, cacheSigned0);
		assertEquals(cacheSigned, cacheSigned1);

		StateCacheSigned cacheSignedBy1=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME, predicateSignedKey1);
		assertTrue(cacheSignedBy1.getEntries().isEmpty());

		StateCacheSigned cacheSignedVerifiedTwice=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME, predicateSignedTwice);
		assertTrue(cacheSignedVerifiedTwice.getEntries().isEmpty());

		StateCacheInfoSignees cacheDescriptorSignees=cacheStorageSignedShared.getCacheDescriptorSignees(VERSION, BASE_NAME);
		boolean oneSignatures=cacheDescriptorSignees.getEntries().stream()
				.allMatch(e -> e.getPublicKeyInfos().contains(publicKeyInfo0)&&1==e.getPublicKeyInfos().size());
		assertTrue(oneSignatures);
	}

	@Test(dependsOnMethods={"testStoreSignedOnce"})
	public void testStoreSignedTwice() throws Exception {
		cacheStorageSignedShared.storeSigned(cacheSigned1);
		StateCache cacheFetched=cacheStorageShared.getCache(VERSION, BASE_NAME);
		assertEquals(cacheFetched, cache);

		StateCacheSigned cacheSigned=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME, predicateSignedTwice);
		assertEquals(cacheSigned, cacheSigned0);
		assertEquals(cacheSigned, cacheSigned1);
	}

	@Test(dependsOnMethods={"testStoreSignedTwice"})
	public void testGetCacheDescriptorSignees() {
		ImmutableSet<PublicKeyInfo> publicKeyInfos=ImmutableSet.of(publicKeyInfo0, publicKeyInfo1);
		Set<StateCacheInfoSignees> cacheDescriptorsSignees=cacheStorageSignedShared.getCacheDescriptorsSignees(VERSION, ImmutableSet.of(BASE_NAME));
		cacheDescriptorsSignees.stream()
				.map(StateCacheInfoSignees::getEntries)
				.flatMap(Set::stream)
				.map(StateCacheEntryInfoSignees::getPublicKeyInfos)
				.forEach(kI -> assertEquals(kI, publicKeyInfos));
	}

	@Test(dependsOnMethods={"testStoreSignedTwice"})
	public void testGetCacheSigned() {
		StateCacheSigned cacheSigned=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME, predicateSignedTwice);
		ImmutableSet<PublicKeyInfo> foundPublicKeyInfos=cacheSigned.getEntries().stream()
				.map(StateCacheEntrySigned::getSignatures)
				.flatMap(Set::stream)
				.map(SignaturePublicKeyInfo::getPublicKeyInfo)
				.collect(ImmutableSet.toImmutableSet());
		ImmutableSet<PublicKeyInfo> publicKeyInfos=ImmutableSet.of(publicKeyInfo0, publicKeyInfo1);
		assertEquals(foundPublicKeyInfos, publicKeyInfos);

		ImmutableMap<PublicKeyInfo, PublicKey> foundPubKeyIndex=Maps.uniqueIndex(cacheSigned.getPublicKeys(), PublicKey::getKeyInfo);
		assertEquals(foundPubKeyIndex.keySet(), publicKeyInfos);
	}

	@Test(dependsOnMethods={"testStoreSignedTwice"})
	public void testGetCacheSignedFiltered() {
		StateCacheSigned cacheSigned=cacheStorageSignedShared.getCacheSigned(VERSION, BASE_NAME, predicateSignedKey1);
		cacheSigned.getEntries().stream()
				.map(StateCacheEntrySigned::getSignatures)
				.map(this::fromSignaturePublicKeyInfos)
				.forEach(s -> assertTrue(s.contains(publicKeyInfo1)));
	}

	private Set<PublicKeyInfo> fromSignaturePublicKeyInfos(Set<SignaturePublicKeyInfo> signaturePublicKeyInfos) {
		return signaturePublicKeyInfos.stream()
				.map(SignaturePublicKeyInfo::getPublicKeyInfo)
				.collect(ImmutableSet.toImmutableSet());
	}

	@Test(dependsOnMethods={"testStoreSignedTwice"})
	public void testGetMissingEntriesSigned() {
		Set<StateCacheEntrySigned> missingEntriesSignedNoneMissing=cacheStorageSignedShared.getMissingEntriesSigned(cacheSigned0.toUnsigned().toInfo());
		assertTrue(missingEntriesSignedNoneMissing.isEmpty());

		StateCacheInfo stateCacheInfo=new StateCacheInfo();
		cacheSigned0.copyShallowTo(stateCacheInfo);
		Set<StateCacheEntrySigned> missingEntriesSignedAllMIssing=cacheStorageSignedShared.getMissingEntriesSigned(stateCacheInfo);
		assertEquals(missingEntriesSignedAllMIssing, cacheSigned0.getEntries());
	}

}
