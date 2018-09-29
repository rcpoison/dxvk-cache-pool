/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author poison
 */
public class DxvkStateCache implements DxvkStateCacheMeta, Serializable {

	private String baseName;
	private int version;
	private int entrySize;
	private Set<DxvkStateCacheEntry> entries;

	public DxvkStateCache() {
	}

	@Override
	public String getBaseName() {
		return baseName;
	}

	public void setBaseName(String baseName) {
		this.baseName=baseName;
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

	public DxvkStateCacheInfo toInfo() {
		DxvkStateCacheInfo info=new DxvkStateCacheInfo();
		info.setVersion(getVersion());
		info.setEntrySize(getEntrySize());
		info.setBaseName(getBaseName());
		if (null!=getEntries()) {
			final ImmutableSet<DxvkStateCacheEntryInfo> entryInfos=getEntries().stream()
					.map(DxvkStateCacheEntry::getDescriptor)
					.collect(ImmutableSet.toImmutableSet());
			info.setEntries(entryInfos);
		}
		return info;
	}

	/**
	 * copy without entries
	 *
	 * @return
	 */
	public DxvkStateCache copyShallow() {
		DxvkStateCache cache=new DxvkStateCache();
		cache.setVersion(getVersion());
		cache.setEntrySize(getEntrySize());
		cache.setBaseName(getBaseName());
		return cache;
	}

	public DxvkStateCache copy() {
		DxvkStateCache cache=copyShallow();
		if (null!=getEntries()) {
			cache.setEntries(ImmutableSet.copyOf(getEntries()));
		}
		return cache;
	}

	public void patch(Set<DxvkStateCacheEntry> other) {
		if (null==other||other.isEmpty()) {
			return;
		}
		ImmutableSet<DxvkStateCacheEntry> combined=ImmutableSet.<DxvkStateCacheEntry>builder()
				.addAll(getEntries())
				.addAll(other)
				.build();
		setEntries(combined);
	}

	public void patch(DxvkStateCache other) {
		patch(other.getEntries());
	}

	/**
	 * get entries contained in this instance but missing in the passed instance
	 *
	 * @param other instance to check for missing entries
	 * @return entries contained in this instance but missing in the passed instance
	 */
	public Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCache other) {
		return ImmutableSet.copyOf(Sets.difference(getEntries(), other.getEntries()));
	}

	/**
	 * get instance with entries contained in this instance but missing in the passed instance
	 *
	 * @param other instance to check for missing entries
	 * @return instance with entries contained in this instance but missing in the passed instance
	 */
	public DxvkStateCache diff(DxvkStateCache other) {
		DxvkStateCache diff=copyShallow();
		diff.setEntries(getMissingEntries(other));
		return diff;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=89*hash+Objects.hashCode(this.baseName);
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
		if (!Objects.equals(this.baseName, other.baseName)) {
			return false;
		}
		if (!Objects.equals(this.entries, other.entries)) {
			return false;
		}
		return true;
	}

}
