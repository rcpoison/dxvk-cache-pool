/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.google.common.collect.Ordering;
import java.io.Serializable;
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
			.thenComparing(Identity::getPublicKeyInfo, Ordering.natural().nullsLast());

	private PublicKeyInfo publicKeyInfo;
	private String email;
	private String name;

	public Identity() {
	}

	@NotNull
	@XmlElement(required=true)
	public PublicKeyInfo getPublicKeyInfo() {
		return publicKeyInfo;
	}

	public void setPublicKeyInfo(PublicKeyInfo publicKeyInfo) {
		this.publicKeyInfo=publicKeyInfo;
	}

	@Email
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email=email;
	}

	@NotNull
	@XmlElement(required=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name=name;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=89*hash+Objects.hashCode(this.publicKeyInfo);
		hash=89*hash+Objects.hashCode(this.email);
		hash=89*hash+Objects.hashCode(this.name);
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
		if (!Objects.equals(this.publicKeyInfo, other.publicKeyInfo)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Identity o) {
		return COMPARATOR.compare(this, o);
	}

}
