/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Set;
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
		System.err.println("privateKey length: "+keyPair.getPrivate().getEncoded().length);
		System.err.println("publicKey length: "+keyPair.getPublic().getEncoded().length);
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

	@Test
	public void stats() throws UnsupportedOperationException, IOException {
		byte[] stateCacheData=TestUtil.readStateCacheData();
		StateCache cache=StateCacheIO.parse(new ByteArrayInputStream(stateCacheData));
		Stopwatch stopwatch=Stopwatch.createStarted();
		StateCacheSigned signedCache=cache.sign(keyPair.getPrivate(), new com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey(keyPair.getPublic()));
		long millis=stopwatch.elapsed().toMillis();
		int totalSize=signedCache.getEntries().stream()
				.map(StateCacheEntrySigned::getSignatures)
				.flatMap(Set::stream)
				.map(SignaturePublicKeyInfo::getSignature)
				.map(Signature::getSignature)
				.mapToInt(s -> s.length)
				.sum();
		System.err.println("signed "+cache.getEntries().size()+" messages in "+millis+"ms, total signatures size: "+totalSize);
		stopwatch.reset();
		stopwatch.start();
		assertTrue(signedCache.verifyAllSignaturesValid());
		stopwatch.stop();
		System.err.println("verified "+cache.getEntries().size()+" messages in "+stopwatch.elapsed().toMillis()+"ms");
	}

}
