/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
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
public class KeyStoreNGTest {

	private static Path storagePath;

	public KeyStoreNGTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		storagePath=Paths.get(System.getProperty("java.io.tmpdir")).resolve("dxvk-cache-pool-keystore").resolve(UUID.randomUUID().toString());
		Files.createDirectories(storagePath);
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

	/**
	 * Test of getPrivateKey method, of class KeyStore.
	 */
	@Test
	public void testIO() throws Exception {
		final KeyStore keyStoreNew=new KeyStore(storagePath);
		PrivateKey privateKey=keyStoreNew.getPrivateKey();
		assertNotNull(privateKey);
		PublicKey publicKey=keyStoreNew.getPublicKey();
		assertNotNull(publicKey);
		assertNotNull(publicKey.getKeyInfo());
		java.security.PublicKey publicKeyInternal=keyStoreNew.getPublicKeyInternal();
		assertNotNull(publicKeyInternal);

		final KeyStore keyStore=new KeyStore(storagePath);
		PrivateKey privateKeyLoaded=keyStore.getPrivateKey();
		assertEquals(privateKeyLoaded, privateKey);
		assertEquals(privateKeyLoaded.getEncoded(), privateKey.getEncoded());

		PublicKey publicKeyLoaded=keyStore.getPublicKey();
		assertEquals(publicKeyLoaded, publicKey);

		java.security.PublicKey publicKeyInternalLoaded=keyStore.getPublicKeyInternal();
		assertEquals(publicKeyInternalLoaded, publicKeyInternal);
		assertEquals(publicKeyInternalLoaded.getEncoded(), publicKeyInternal.getEncoded());
	}

}
