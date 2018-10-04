/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheInfoSignees implements StateCacheMeta, Serializable {

	@NotNull
	private String baseName;
	private int version;
	private int entrySize;
	private Long lastModified;
	private Set<StateCacheEntryInfoSignees> entries;

	@NotNull
	@Size(min=1, max=256)
	@XmlElement(required=true)
	@Override
	public String getBaseName() {
		return baseName;
	}

	@Override
	public void setBaseName(String executableInfo) {
		this.baseName=executableInfo;
	}

	@XmlElement(required=true)
	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version=version;
	}

	@Override
	public int getEntrySize() {
		return entrySize;
	}

	@Override
	public void setEntrySize(int entrySize) {
		this.entrySize=entrySize;
	}

	public Long getLastModified() {
		return lastModified;
	}

	public void setLastModified(Long lastModified) {
		this.lastModified=lastModified;
	}

	public Set<StateCacheEntryInfoSignees> getEntries() {
		return entries;
	}

	public void setEntries(Set<StateCacheEntryInfoSignees> entries) {
		this.entries=entries;
	}

	public StateCacheInfo toUnsigned() {
		StateCacheInfo cacheInfo=new StateCacheInfo();
		copyShallowTo(cacheInfo);
		cacheInfo.setLastModified(lastModified);
		if (null!=entries) {
			ImmutableSet<StateCacheEntryInfo> entriesUnsigned=entries.stream()
					.map(StateCacheEntryInfoSignees::getEntryInfo)
					.filter(Predicates.notNull())
					.collect(ImmutableSet.toImmutableSet());
			cacheInfo.setEntries(entriesUnsigned);
		} else {
			cacheInfo.setEntries(ImmutableSet.of());
		}
		return cacheInfo;
	}

	@Override
	public int hashCode() {
		int hash=5;
		hash=59*hash+Objects.hashCode(this.baseName);
		hash=59*hash+this.version;
		hash=59*hash+Objects.hashCode(this.entries);
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
		final StateCacheInfoSignees other=(StateCacheInfoSignees) obj;
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
