/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import java.io.Serializable;
import java.util.Arrays;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheEntryInfo implements Serializable {

	private byte[] hash;

	public StateCacheEntryInfo() {
	}

	public StateCacheEntryInfo(byte[] hash) {
		this.hash=hash;
	}

	@NotNull
	@Size(min=32, max=32)
	@XmlAttribute(required=true)
	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash=hash;
	}

	@Override
	public int hashCode() {
		int hash=5;
		hash=53*hash+Arrays.hashCode(this.hash);
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
		final StateCacheEntryInfo other=(StateCacheEntryInfo) obj;
		if (!Arrays.equals(this.hash, other.hash)) {
			return false;
		}
		return true;
	}

}
