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
public class IdentityWithVerification implements Serializable {

	private Identity identity;
	private IdentityVerification identityVerification;

	public IdentityWithVerification() {
	}

	public IdentityWithVerification(Identity identity, IdentityVerification identityVerification) {
		this.identity=identity;
		this.identityVerification=identityVerification;
	}

	@NotNull
	@XmlElement(required=true)
	public Identity getIdentity() {
		return identity;
	}

	public void setIdentity(Identity identity) {
		this.identity=identity;
	}

	@NotNull
	@XmlElement(required=true)
	public IdentityVerification getIdentityVerification() {
		return identityVerification;
	}

	public void setIdentityVerification(IdentityVerification identityVerification) {
		this.identityVerification=identityVerification;
	}

	@Override
	public int hashCode() {
		int hash=5;
		hash=23*hash+Objects.hashCode(this.identity);
		hash=23*hash+Objects.hashCode(this.identityVerification);
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
		final IdentityWithVerification other=(IdentityWithVerification) obj;
		if (!Objects.equals(this.identity, other.identity)) {
			return false;
		}
		if (!Objects.equals(this.identityVerification, other.identityVerification)) {
			return false;
		}
		return true;
	}

}
