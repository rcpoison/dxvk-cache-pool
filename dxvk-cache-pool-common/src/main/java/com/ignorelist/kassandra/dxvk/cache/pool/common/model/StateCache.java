/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author poison
 */
public class StateCache implements StateCacheMeta, Serializable {

	private String baseName;
	private int version;
	private int entrySize;
	private Set<StateCacheEntry> entries;

	public StateCache() {
	}

	@NotNull
	@Size(min=1, max=256)
	@XmlElement(required=true)
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

	public Set<StateCacheEntry> getEntries() {
		return entries;
	}

	public void setEntries(Set<StateCacheEntry> entries) {
		this.entries=entries;
	}

	public StateCacheInfo toInfo() {
		StateCacheInfo info=new StateCacheInfo();
		info.setVersion(getVersion());
		info.setEntrySize(getEntrySize());
		info.setBaseName(getBaseName());
		if (null!=getEntries()) {
			final ImmutableSet<StateCacheEntryInfo> entryInfos=getEntries().stream()
					.map(StateCacheEntry::getEntryInfo)
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
	public StateCache copyShallow() {
		StateCache cache=new StateCache();
		cache.setVersion(getVersion());
		cache.setEntrySize(getEntrySize());
		cache.setBaseName(getBaseName());
		return cache;
	}

	public StateCache copy() {
		StateCache cache=copyShallow();
		if (null!=getEntries()) {
			cache.setEntries(ImmutableSet.copyOf(getEntries()));
		}
		return cache;
	}

	public void patch(Set<StateCacheEntry> other) {
		if (null==other||other.isEmpty()) {
			return;
		}
		ImmutableSet<StateCacheEntry> combined=ImmutableSet.<StateCacheEntry>builder()
				.addAll(getEntries())
				.addAll(other)
				.build();
		setEntries(combined);
	}

	public void patch(StateCache other) {
		patch(other.getEntries());
	}

	/**
	 * get entries contained in this instance but missing in the passed instance
	 *
	 * @param other instance to check for missing entries
	 * @return entries contained in this instance but missing in the passed instance
	 */
	public Set<StateCacheEntry> getMissingEntries(StateCache other) {
		return ImmutableSet.copyOf(Sets.difference(getEntries(), other.getEntries()));
	}

	/**
	 * get instance with entries contained in this instance but missing in the passed instance
	 *
	 * @param other instance to check for missing entries
	 * @return instance with entries contained in this instance but missing in the passed instance
	 */
	public StateCache diff(StateCache other) {
		StateCache diff=copyShallow();
		diff.setEntries(getMissingEntries(other));
		return diff;
	}

	/**
	 * get instance with entries contained in this instance but missing in the passed instance
	 *
	 * @param other instance to check for missing entries
	 * @return instance with entries contained in this instance but missing in the passed instance
	 */
	public StateCache diff(StateCacheInfo other) {
		StateCacheInfo info=toInfo();
		ImmutableSet<StateCacheEntryInfo> missingEntryInfos=info.getMissingEntries(other);
		ImmutableMap<StateCacheEntryInfo, StateCacheEntry> indexByInfo=Maps.uniqueIndex(getEntries(), StateCacheEntry::getEntryInfo);
		ImmutableSet<StateCacheEntry> missingEntries=missingEntryInfos.stream()
				.map(indexByInfo::get)
				.collect(ImmutableSet.toImmutableSet());
		StateCache diff=copyShallow();
		diff.setEntries(missingEntries);
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
		final StateCache other=(StateCache) obj;
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
