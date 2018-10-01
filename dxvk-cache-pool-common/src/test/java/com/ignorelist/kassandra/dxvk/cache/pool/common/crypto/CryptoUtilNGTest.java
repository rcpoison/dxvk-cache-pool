/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
public class CryptoUtilNGTest {

	private static KeyPair keyPair;

	public CryptoUtilNGTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		keyPair=CryptoUtil.generate();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		keyPair=null;
	}

	@BeforeMethod
	public void setUpMethod() throws Exception {
	}

	@AfterMethod
	public void tearDownMethod() throws Exception {
	}

	@Test
	public void testWriteReadPublicKey() throws Exception {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		PublicKey publicKey=keyPair.getPublic();
		CryptoUtil.write(out, publicKey);

		ByteArrayInputStream in=new ByteArrayInputStream(out.toByteArray());
		PublicKey roundTripPublicKey=CryptoUtil.readPublicKey(in);
		assertEquals(roundTripPublicKey.getEncoded(), publicKey.getEncoded());
	}

	@Test
	public void testWriteReadPrivateKey() throws Exception {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		PrivateKey privateKey=keyPair.getPrivate();
		CryptoUtil.write(out, privateKey);

		ByteArrayInputStream in=new ByteArrayInputStream(out.toByteArray());
		PrivateKey roundTripPrivateKey=CryptoUtil.readPrivateKey(in);
		assertEquals(roundTripPrivateKey.getEncoded(), privateKey.getEncoded());
	}

	@Test
	public void testSign() throws Exception {
		byte[] message="Hello Cruel World".getBytes(Charsets.UTF_8);
		PrivateKey privateKey=keyPair.getPrivate();
		byte[] signature=CryptoUtil.sign(message, privateKey);
		assertTrue(CryptoUtil.verify(message, keyPair.getPublic(), signature));
	}

	@Test
	public void testSignWrongMessage() throws Exception {
		byte[] message="Hello Cruel World".getBytes(Charsets.UTF_8);
		PrivateKey privateKey=keyPair.getPrivate();
		byte[] signature=CryptoUtil.sign(message, privateKey);
		byte[] wrongMessage="Hello World".getBytes(Charsets.UTF_8);
		assertFalse(CryptoUtil.verify(wrongMessage, keyPair.getPublic(), signature));
	}

	@Test
	public void testSignWrongKey() throws Exception {
		byte[] message="Hello Cruel World".getBytes(Charsets.UTF_8);
		PrivateKey privateKey=keyPair.getPrivate();
		byte[] signature=CryptoUtil.sign(message, privateKey);
		assertFalse(CryptoUtil.verify(message, CryptoUtil.generate().getPublic(), signature));
	}

}
