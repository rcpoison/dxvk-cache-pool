/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author poison
 */
public class SignatureStorageFSNGTest {

	private static final String BASE_NAME="Beat Saber";
	private static Path storageRoot;
	private static StateCache cache;
	private static KeyPair keyPair;
	private static StateCacheSigned cacheSigned;
	private static SignatureStorageFS storageShared;

	public SignatureStorageFSNGTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		storageRoot=Paths.get(System.getProperty("java.io.tmpdir")).resolve("dxvk-cache-pool-signatures").resolve(UUID.randomUUID().toString());
		cache=StateCacheIO.parse(new ByteArrayInputStream(TestUtil.readStateCacheData()));
		cache.setBaseName(BASE_NAME);
		keyPair=CryptoUtil.generate();
		cacheSigned=cache.sign(keyPair.getPrivate(), new PublicKey(keyPair.getPublic()));
		storageShared=buildSignatureStorage();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		storageShared.close();
	}

	@BeforeMethod
	public void setUpMethod() throws Exception {
	}

	@AfterMethod
	public void tearDownMethod() throws Exception {
	}

	private static SignatureStorageFS buildSignatureStorage() throws IOException {
		return new SignatureStorageFS(storageRoot);
	}

	@DataProvider(parallel=true)
	private Iterator<Object[]> entryInfoSignee() throws Exception {
		return Iterables.transform(
				cacheSigned.getEntries(),
				e -> new Object[]{
					e.getCacheEntry().getEntryInfo(),
					Iterables.getOnlyElement(e.getSignatures())
				}
		).iterator();
	}

	/**
	 * Test of getSignedBy method, of class SignatureStorageFS.
	 */
	@Test(dependsOnMethods={"testAddSignee"})
	public void testGetSignedBy() throws Exception {
		try (SignatureStorageFS storage=buildSignatureStorage()) {
			for (StateCacheEntrySigned entry : cacheSigned.getEntries()) {
				final StateCacheEntryInfo entryInfo=entry.getCacheEntry().getEntryInfo();
				final ImmutableSet<PublicKeyInfo> publicKeyInfos=entry.getSignatures().stream()
						.map(SignaturePublicKeyInfo::getPublicKeyInfo)
						.collect(ImmutableSet.toImmutableSet());
				Set<PublicKeyInfo> storedPublicKeyInfos=storage.getSignedBy(entryInfo);
				Assert.assertEquals(storedPublicKeyInfos, publicKeyInfos);
			}
		}
	}

	/**
	 * Test of addSignee method, of class SignatureStorageFS.
	 */
	@Test(dataProvider="entryInfoSignee")
	public void testAddSignee(StateCacheEntryInfo entryInfo, SignaturePublicKeyInfo signaturePublicKeyInfo) throws Exception {
		storageShared.addSignee(entryInfo, signaturePublicKeyInfo);
		Assert.assertTrue(storageShared.getSignedBy(entryInfo).contains(signaturePublicKeyInfo.getPublicKeyInfo()));
	}

	/**
	 * Test of getSignatures method, of class SignatureStorageFS.
	 */
	@Test(dependsOnMethods={"testAddSignee"})
	public void testGetSignatures() throws Exception {
		try (SignatureStorageFS storage=buildSignatureStorage()) {
			storage.init();
			Stopwatch stopwatch=Stopwatch.createStarted();
			for (StateCacheEntrySigned entry : cacheSigned.getEntries()) {
				final StateCacheEntryInfo entryInfo=entry.getCacheEntry().getEntryInfo();
				final Set<SignaturePublicKeyInfo> signatures=entry.getSignatures();
				final Set<SignaturePublicKeyInfo> storedSignatures=storage.getSignatures(entryInfo);
				Assert.assertEquals(storedSignatures, signatures);
			}
			stopwatch.stop();
			System.err.println("read all signatures in "+stopwatch.elapsed().toMillis()+"ms");
		}
	}

	/**
	 * Test of getPublicKey method, of class SignatureStorageFS.
	 */
	@Test(dependsOnMethods={"testStorePublicKey"})
	public void testGetPublicKey() throws Exception {
		try (SignatureStorageFS storage=buildSignatureStorage()) {
			final PublicKey expected=new PublicKey(keyPair.getPublic());
			final PublicKey storedKey=storage.getPublicKey(expected.getKeyInfo());
			Assert.assertEquals(storedKey, expected);
		}
	}

	/**
	 * Test of storePublicKey method, of class SignatureStorageFS.
	 */
	@Test
	public void testStorePublicKey() throws Exception {
		try (SignatureStorageFS storage=buildSignatureStorage()) {
			final PublicKey publicKey=new PublicKey(keyPair.getPublic());
			storage.storePublicKey(publicKey);
			final PublicKey storedKey=storage.getPublicKey(publicKey.getKeyInfo());
			Assert.assertEquals(storedKey, publicKey);
		}
	}

}
