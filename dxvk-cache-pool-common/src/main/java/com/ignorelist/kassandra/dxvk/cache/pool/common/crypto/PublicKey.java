/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import java.io.Serializable;
import java.util.Arrays;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class PublicKey implements Serializable {

	private byte[] key;

	public PublicKey() {
	}

	public PublicKey(byte[] key) {
		this.key=key;
	}

	@NotNull
	@XmlElement(required=true)
	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key=key;
	}

	public PublicKeyInfo toInfo() {
		return new PublicKeyInfo(this);
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=41*hash+Arrays.hashCode(this.key);
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
		final PublicKey other=(PublicKey) obj;
		if (!Arrays.equals(this.key, other.key)) {
			return false;
		}
		return true;
	}

}
