/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Signature;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheEntry implements Serializable {

	private StateCacheEntryInfo entryInfo;
	private byte[] entry;

	public StateCacheEntry() {
	}

	public StateCacheEntry(StateCacheEntryInfo entryInfo, byte[] entry) {
		this.entryInfo=entryInfo;
		this.entry=entry;
	}

	public StateCacheEntry(byte[] entry) {
		entryInfo=new StateCacheEntryInfo(entryHash(entry));
		this.entry=entry;
	}

	private static byte[] entryHash(byte[] entry) {
		return Hashing.sha256().hashBytes(entry).asBytes();
	}

	public StateCacheEntryInfo getEntryInfo() {
		return entryInfo;
	}

	public void setEntryInfo(StateCacheEntryInfo entryInfo) {
		this.entryInfo=entryInfo;
	}

	public byte[] getEntry() {
		return entry;
	}

	public void setEntry(byte[] entry) {
		this.entry=entry;
	}

	/**
	 * wrap in signed cache entry
	 *
	 * @param privateKey the private key
	 * @param publicKeyInfo the public keys info
	 * @return this wrapped in signed
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws NoSuchAlgorithmException
	 */
	public StateCacheEntrySigned sign(final PrivateKey privateKey, final PublicKeyInfo publicKeyInfo) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		final Signature signature=new Signature(CryptoUtil.sign(getEntry(), privateKey));
		final SignaturePublicKeyInfo signaturePublicKeyInfo=new SignaturePublicKeyInfo(signature, publicKeyInfo);
		return new StateCacheEntrySigned(this, ImmutableSet.of(signaturePublicKeyInfo));
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=37*hash+Objects.hashCode(this.entryInfo);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this==obj) {
			return true;
		}
		if (obj==null) {
			return false;
		}
		if (getClass()!=obj.getClass()) {
			return false;
		}
		final StateCacheEntry other=(StateCacheEntry) obj;
		if (!Objects.equals(this.entryInfo, other.entryInfo)) {
			return false;
		}
		return true;
	}

}
