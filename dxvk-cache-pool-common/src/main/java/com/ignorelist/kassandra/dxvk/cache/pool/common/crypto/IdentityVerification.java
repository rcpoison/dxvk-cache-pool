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
public class IdentityVerification implements Serializable {

	private byte[] publicKeySignature;
	private byte[] publicKeyGPG;

	public byte[] getPublicKeySignature() {
		return publicKeySignature;
	}

	@NotNull
	@XmlElement(required=true)
	public void setPublicKeySignature(byte[] publicKeySignature) {
		this.publicKeySignature=publicKeySignature;
	}

	@NotNull
	@XmlElement(required=true)
	public byte[] getPublicKeyGPG() {
		return publicKeyGPG;
	}

	public void setPublicKeyGPG(byte[] publicKeyGPG) {
		this.publicKeyGPG=publicKeyGPG;
	}

	@Override
	public int hashCode() {
		int hash=5;
		hash=29*hash+Arrays.hashCode(this.publicKeySignature);
		hash=29*hash+Arrays.hashCode(this.publicKeyGPG);
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
		final IdentityVerification other=(IdentityVerification) obj;
		if (!Arrays.equals(this.publicKeySignature, other.publicKeySignature)) {
			return false;
		}
		if (!Arrays.equals(this.publicKeyGPG, other.publicKeyGPG)) {
			return false;
		}
		return true;
	}

}
