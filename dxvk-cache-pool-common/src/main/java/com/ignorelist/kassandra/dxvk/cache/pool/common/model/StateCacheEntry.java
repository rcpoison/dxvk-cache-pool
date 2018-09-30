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
public class StateCacheEntry implements Serializable {

	private StateCacheEntryInfo entryInfo;
	private byte[] entry;

	public StateCacheEntry() {
	}

	public StateCacheEntry(StateCacheEntryInfo entryInfo, byte[] entry) {
		this.entryInfo=entryInfo;
		this.entry=entry;
	}

	public StateCacheEntry(byte[] entry) {
		entryInfo=new StateCacheEntryInfo(entryHash(entry));
		this.entry=entry;
	}

	private static byte[] entryHash(byte[] entry) {
		return Hashing.sha256().hashBytes(entry).asBytes();
	}

	public StateCacheEntryInfo getEntryInfo() {
		return entryInfo;
	}

	public void setEntryInfo(StateCacheEntryInfo entryInfo) {
		this.entryInfo=entryInfo;
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
		hash=37*hash+Objects.hashCode(this.entryInfo);
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
		final StateCacheEntry other=(StateCacheEntry) obj;
		if (!Objects.equals(this.entryInfo, other.entryInfo)) {
			return false;
		}
		return true;
	}

}
