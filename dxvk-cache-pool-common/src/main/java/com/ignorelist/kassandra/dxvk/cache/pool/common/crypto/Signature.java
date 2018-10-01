/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import java.util.Arrays;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class Signature {

	private byte[] signature;

	public Signature() {
	}

	public Signature(byte[] signature) {
		this.signature=signature;
	}

	@NotNull
	@XmlAttribute(required=true)
	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature=signature;
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=17*hash+Arrays.hashCode(this.signature);
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
		final Signature other=(Signature) obj;
		if (!Arrays.equals(this.signature, other.signature)) {
			return false;
		}
		return true;
	}

}
