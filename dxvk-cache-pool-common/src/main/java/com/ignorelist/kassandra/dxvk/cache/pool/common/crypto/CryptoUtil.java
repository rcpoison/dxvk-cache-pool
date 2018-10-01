/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 *
 * @author poison
 */
public class CryptoUtil {

	private static final String ALGO_RAND="SHA1PRNG";
	private static final String ALGO_KEY="EC";
	private static final String ALGO_SIGNATURE="SHA1withECDSA";

	public static KeyPair generate() throws NoSuchAlgorithmException {
		final SecureRandom random=SecureRandom.getInstance(ALGO_RAND);
		final KeyPairGenerator keyGen=KeyPairGenerator.getInstance(ALGO_KEY);
		keyGen.initialize(256, random);
		return keyGen.generateKeyPair();
	}

	public static void write(final OutputStream out, final PublicKey publicKey) throws IOException {
		final X509EncodedKeySpec encodedKeySpec=new X509EncodedKeySpec(publicKey.getEncoded());
		out.write(encodedKeySpec.getEncoded());
	}

	public static PublicKey decodePublicKey(final byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		final KeyFactory instance=KeyFactory.getInstance(ALGO_KEY);
		final X509EncodedKeySpec keySpec=new X509EncodedKeySpec(key);
		return instance.generatePublic(keySpec);
	}

	public static PublicKey readPublicKey(final InputStream in) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		final byte[] key=ByteStreams.toByteArray(in);
		return decodePublicKey(key);
	}

	public static PrivateKey decodePrivateKey(final byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		final KeyFactory instance=KeyFactory.getInstance(ALGO_KEY);
		final PKCS8EncodedKeySpec keySpec=new PKCS8EncodedKeySpec(key);
		return instance.generatePrivate(keySpec);
	}

	public static PrivateKey readPrivateKey(final InputStream in) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		final byte[] key=ByteStreams.toByteArray(in);
		return decodePrivateKey(key);
	}

	public static void write(final OutputStream out, final PrivateKey privateKey) throws IOException {
		final PKCS8EncodedKeySpec encodedKeySpec=new PKCS8EncodedKeySpec(privateKey.getEncoded());
		out.write(encodedKeySpec.getEncoded());
	}

	public static byte[] sign(final byte[] message, final PrivateKey privateKey) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		final Signature dsa=Signature.getInstance(ALGO_SIGNATURE);
		dsa.initSign(privateKey);
		dsa.update(message);
		return dsa.sign();
	}

	public static boolean verify(final byte[] message, final PublicKey publicKey, final byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		final Signature dsa=Signature.getInstance(ALGO_SIGNATURE);
		dsa.initVerify(publicKey);
		dsa.update(message);
		return dsa.verify(signature);
	}
}
