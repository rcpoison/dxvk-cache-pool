/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.hash.Hashing;
import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class DxvkStateCacheEntry implements Serializable {

	private DxvkStateCacheEntryInfo descriptor;
	private byte[] entry;

	public DxvkStateCacheEntry() {
	}

	public DxvkStateCacheEntry(DxvkStateCacheEntryInfo descriptor, byte[] entry) {
		this.descriptor=descriptor;
		this.entry=entry;
	}

	public DxvkStateCacheEntry(byte[] entry) {
		descriptor=new DxvkStateCacheEntryInfo(entryHash(entry));
		this.entry=entry;
	}

	private static byte[] entryHash(byte[] entry) {
		return Hashing.sha256().hashBytes(entry).asBytes();
	}

	public DxvkStateCacheEntryInfo getDescriptor() {
		return descriptor;
	}

	public void setDescriptor(DxvkStateCacheEntryInfo descriptor) {
		this.descriptor=descriptor;
	}

	public byte[] getEntry() {
		return entry;
	}

	public void setEntry(byte[] entry) {
		this.entry=entry;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=37*hash+Objects.hashCode(this.descriptor);
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
		final DxvkStateCacheEntry other=(DxvkStateCacheEntry) obj;
		if (!Objects.equals(this.descriptor, other.descriptor)) {
			return false;
		}
		return true;
	}

}
