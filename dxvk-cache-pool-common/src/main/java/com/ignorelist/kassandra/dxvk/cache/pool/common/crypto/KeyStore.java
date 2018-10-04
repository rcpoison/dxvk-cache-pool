/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author poison
 */
public class KeyStore {

	private static final Path FILENAME_PRIVATE=Paths.get("ec");
	private static final Path FILENAME_PUBLIC=Paths.get("ec.pub");
	private static final Path CONFIG_SUBDIR=Paths.get("dxvk-cache-pool");

	private final Path configDirectory;
	private PrivateKey privateKey;
	private java.security.PublicKey publicKeyCrypto;
	private com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey publicKey;

	public KeyStore(Path directory) throws IOException {
		this.configDirectory=directory;
		init();
	}

	public KeyStore() throws IOException {
		Path configHome=Util.getEnvPath("XDG_CONFIG_HOME");
		if (null==configHome) {
			configHome=Paths.get(System.getProperty("user.home"), ".config");
		}
		configDirectory=configHome.resolve(CONFIG_SUBDIR);
		init();
	}

	private void init() throws IOException {
		Files.createDirectories(configDirectory);
		final Path privFile=configDirectory.resolve(FILENAME_PRIVATE);
		final Path pubFile=configDirectory.resolve(FILENAME_PUBLIC);
		if (!Files.isRegularFile(privFile)) {
			try {
				final KeyPair generated=CryptoUtil.generate();

				try (OutputStream outPriv=Files.newOutputStream(privFile)) {
					CryptoUtil.write(outPriv, generated.getPrivate());
				}
				try (OutputStream outPub=Files.newOutputStream(pubFile)) {
					CryptoUtil.write(outPub, generated.getPublic());
				}
				privateKey=generated.getPrivate();
				publicKeyCrypto=generated.getPublic();

			} catch (IOException ex) {
				throw ex;
			} catch (Exception ex) {
				Logger.getLogger(KeyStore.class.getName()).log(Level.SEVERE, null, ex);
				throw new IOException(ex);
			}
		} else {
			try {
				try (InputStream inPriv=Files.newInputStream(privFile)) {
					privateKey=CryptoUtil.readPrivateKey(inPriv);
				}
				try (InputStream inPub=Files.newInputStream(pubFile)) {
					publicKeyCrypto=CryptoUtil.readPublicKey(inPub);
				}
			} catch (IOException ex) {
				throw ex;
			} catch (Exception ex) {
				Logger.getLogger(KeyStore.class.getName()).log(Level.SEVERE, null, ex);
				throw new IOException(ex);
			}
		}
		publicKey=new com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey(publicKeyCrypto);

	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public java.security.PublicKey getPublicKeyInternal() {
		return publicKeyCrypto;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

}
