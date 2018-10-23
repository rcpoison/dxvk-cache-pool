/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import java.io.Serializable;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class SignaturePublicKeyInfo implements Serializable {
	
	private Signature signature;
	private PublicKeyInfo publicKeyInfo;

	public SignaturePublicKeyInfo() {
	}

	public SignaturePublicKeyInfo(Signature signature, PublicKeyInfo publicKeyInfo) {
		this.signature=signature;
		this.publicKeyInfo=publicKeyInfo;
	}

	@NotNull
	@XmlElement(required=true)
	public Signature getSignature() {
		return signature;
	}

	public void setSignature(Signature signature) {
		this.signature=signature;
	}

	@NotNull
	@XmlElement(required=true)
	public PublicKeyInfo getPublicKeyInfo() {
		return publicKeyInfo;
	}

	public void setPublicKeyInfo(PublicKeyInfo publicKeyInfo) {
		this.publicKeyInfo=publicKeyInfo;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=73*hash+Objects.hashCode(this.signature);
		hash=73*hash+Objects.hashCode(this.publicKeyInfo);
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
		if (!(obj instanceof SignaturePublicKeyInfo)) {
			return false;
		}
		final SignaturePublicKeyInfo other=(SignaturePublicKeyInfo) obj;
		if (!Objects.equals(this.signature, other.signature)) {
			return false;
		}
		if (!Objects.equals(this.publicKeyInfo, other.publicKeyInfo)) {
			return false;
		}
		return true;
	}
	
	
	
}
