/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import java.io.Serializable;
import java.util.Arrays;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class PublicKeyInfo implements Serializable, Comparable<PublicKeyInfo> {

	private byte[] hash;

	public PublicKeyInfo() {
	}

	public PublicKeyInfo(byte[] hash) {
		this.hash=hash;
	}

	public PublicKeyInfo(PublicKey publicKey) {
		this(Hashing.sha256().hashBytes(publicKey.getKey()).asBytes());
	}

	@NotNull
	@Size(min=32, max=32)
	@XmlElement(required=true)
	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash=hash;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=37*hash+Arrays.hashCode(this.hash);
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
		final PublicKeyInfo other=(PublicKeyInfo) obj;
		if (!Arrays.equals(this.hash, other.hash)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("hash", BaseEncoding.base16().encode(hash))
				.toString();
	}

	@Override
	public int compareTo(PublicKeyInfo o) {
		return Util.compare(hash, o.getHash());
	}

}
