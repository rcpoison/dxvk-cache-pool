/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.google.common.collect.Ordering;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class Identity implements Serializable, Comparable<Identity> {

	private static final Comparator<Identity> COMPARATOR=Comparator.comparing(Identity::getEmail, Ordering.natural().nullsLast())
			.thenComparing(Identity::getName, Ordering.natural().nullsLast())
			.thenComparing(Identity::getPublicKey, Ordering.natural().nullsLast());

	private PublicKey publicKey;
	private byte[] publicKeySignature;
	private byte[] publicKeyGPG;
	private String email;
	private String name;

	public Identity() {
	}

	@NotNull
	@XmlElement(required=true)
	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey=publicKey;
	}

	@Email
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email=email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name=name;
	}

	public byte[] getPublicKeySignature() {
		return publicKeySignature;
	}

	public void setPublicKeySignature(byte[] publicKeySignature) {
		this.publicKeySignature=publicKeySignature;
	}

	public byte[] getPublicKeyGPG() {
		return publicKeyGPG;
	}

	public void setPublicKeyGPG(byte[] publicKeyGPG) {
		this.publicKeyGPG=publicKeyGPG;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=37*hash+Objects.hashCode(this.publicKey);
		hash=37*hash+Arrays.hashCode(this.publicKeySignature);
		hash=37*hash+Arrays.hashCode(this.publicKeyGPG);
		hash=37*hash+Objects.hashCode(this.email);
		hash=37*hash+Objects.hashCode(this.name);
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
		final Identity other=(Identity) obj;
		if (!Objects.equals(this.email, other.email)) {
			return false;
		}
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		if (!Objects.equals(this.publicKey, other.publicKey)) {
			return false;
		}
		if (!Arrays.equals(this.publicKeySignature, other.publicKeySignature)) {
			return false;
		}
		if (!Arrays.equals(this.publicKeyGPG, other.publicKeyGPG)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Identity o) {
		return COMPARATOR.compare(this, o);
	}

}
