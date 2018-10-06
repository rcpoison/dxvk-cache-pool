/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityWithVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
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
public class IdentityStorageFSNGTest {

	private static Path storageRoot;
	private static KeyPair keyPair0;
	private static KeyPair keyPair1;
	private static PublicKey publicKey0;
	private static PublicKey publicKey1;

	public IdentityStorageFSNGTest() throws Exception {
		storageRoot=Paths.get(System.getProperty("java.io.tmpdir")).resolve("dxvk-cache-pool-identity").resolve(UUID.randomUUID().toString());
		keyPair0=CryptoUtil.generate();
		keyPair1=CryptoUtil.generate();
		publicKey0=new PublicKey(keyPair0.getPublic());
		publicKey1=new PublicKey(keyPair1.getPublic());
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
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

	private IdentityWithVerification buildIdentity(PublicKey key) {

		Identity identity=new Identity();
		identity.setPublicKeyInfo(key.getKeyInfo());
		identity.setEmail("who@cares.net");
		identity.setName("Who Cares");

		IdentityVerification iv=new IdentityVerification();
		iv.setPublicKeyGPG(key.getKey()); // just garbage for testing! Not a proper example!
		iv.setPublicKeySignature(key.getKeyInfo().getHash()); // just garbage for testing! Not a proper example!

		IdentityWithVerification idv=new IdentityWithVerification();
		idv.setIdentity(identity);
		idv.setIdentityVerification(iv);
		return idv;
	}

	@Test
	public void testStoreIdentity() throws Exception {
		final IdentityWithVerification idv0=buildIdentity(publicKey0);
		final IdentityWithVerification idv1=buildIdentity(publicKey1);

		IdentityStorageFS instance=new IdentityStorageFS(storageRoot);
		instance.storeIdentity(idv0);
		instance.storeIdentity(idv1);
		final PublicKeyInfo publicKey=idv0.getIdentity().getPublicKeyInfo();
		assertEquals(instance.getIdentity(publicKey), idv0.getIdentity());
		assertEquals(instance.getIdentityVerification(publicKey), idv0.getIdentityVerification());
	}

	@Test(dependsOnMethods={"testStoreIdentity"})
	public void testGetIdentity() throws Exception {
		final IdentityWithVerification idv1=buildIdentity(publicKey1);
		PublicKeyInfo keyInfo=idv1.getIdentity().getPublicKeyInfo();
		IdentityStorageFS instance=new IdentityStorageFS(storageRoot);
		Identity expResult=idv1.getIdentity();
		Identity result=instance.getIdentity(keyInfo);
		assertEquals(result, expResult);
	}

	@Test(dependsOnMethods={"testStoreIdentity"})
	public void testGetIdentityVerification() throws Exception {
		final IdentityWithVerification idv1=buildIdentity(publicKey1);
		PublicKeyInfo keyInfo=idv1.getIdentity().getPublicKeyInfo();
		IdentityStorageFS instance=new IdentityStorageFS(storageRoot);
		IdentityVerification expResult=idv1.getIdentityVerification();
		IdentityVerification result=instance.getIdentityVerification(keyInfo);
		assertEquals(result, expResult);
	}

	@Test(dependsOnMethods={"testStoreIdentity"})
	public void testGetVerifiedKeyInfos() throws Exception {
		IdentityStorageFS instance=new IdentityStorageFS(storageRoot);
		instance.init();
		Set<PublicKeyInfo> expResult=ImmutableSet.of(publicKey0.getKeyInfo(), publicKey1.getKeyInfo());
		Set<PublicKeyInfo> result=instance.getVerifiedKeyInfos();
		assertEquals(result, expResult);
	}

}
