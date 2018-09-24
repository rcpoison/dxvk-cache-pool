/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import java.io.Serializable;
import java.util.Arrays;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class DxvkStateCacheEntryDescriptor implements Serializable {

	private int version;
	private byte[] hash;

	public DxvkStateCacheEntryDescriptor() {
	}

	public DxvkStateCacheEntryDescriptor(int version, byte[] hash) {
		this.version=version;
		this.hash=hash;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version=version;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash=hash;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=41*hash+this.version;
		hash=41*hash+Arrays.hashCode(this.hash);
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
		final DxvkStateCacheEntryDescriptor other=(DxvkStateCacheEntryDescriptor) obj;
		if (this.version!=other.version) {
			return false;
		}
		if (!Arrays.equals(this.hash, other.hash)) {
			return false;
		}
		return true;
	}

}
