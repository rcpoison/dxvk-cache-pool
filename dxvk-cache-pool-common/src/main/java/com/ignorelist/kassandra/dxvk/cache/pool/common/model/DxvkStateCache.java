/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author poison
 */
public class DxvkStateCache implements DxvkStateCacheMeta, Serializable {

	private ExecutableInfo executableInfo;
	private int version;
	private int entrySize;
	private Set<DxvkStateCacheEntry> entries;

	public DxvkStateCache() {
	}

	@Override
	public ExecutableInfo getExecutableInfo() {
		return executableInfo;
	}

	public void setExecutableInfo(ExecutableInfo executableInfo) {
		this.executableInfo=executableInfo;
	}

	@Override
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version=version;
	}

	@Override
	public int getEntrySize() {
		return entrySize;
	}

	public void setEntrySize(int entrySize) {
		this.entrySize=entrySize;
	}

	public Set<DxvkStateCacheEntry> getEntries() {
		return entries;
	}

	public void setEntries(Set<DxvkStateCacheEntry> entries) {
		this.entries=entries;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=89*hash+Objects.hashCode(this.executableInfo);
		hash=89*hash+this.version;
		hash=89*hash+Objects.hashCode(this.entries);
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
		final DxvkStateCache other=(DxvkStateCache) obj;
		if (this.version!=other.version) {
			return false;
		}
		if (!Objects.equals(this.executableInfo, other.executableInfo)) {
			return false;
		}
		if (!Objects.equals(this.entries, other.entries)) {
			return false;
		}
		return true;
	}

}
