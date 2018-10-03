/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheInfo implements StateCacheMeta, Serializable {

	public static final Comparator<StateCacheInfo> COMPARATOR_EXE_NAME=Comparator
			.comparing(StateCacheInfo::getBaseName, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(StateCacheInfo::getLastModified, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparingInt(StateCacheInfo::getVersion);

	@NotNull
	private String baseName;
	private int version;
	private int entrySize;
	private Long lastModified;
	private Set<StateCacheEntryInfo> entries;
	private PredicateStateCacheEntrySigned predicateStateCacheEntrySigned;

	public StateCacheInfo() {
	}

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

	public Set<StateCacheEntryInfo> getEntries() {
		return entries;
	}

	public void setEntries(Set<StateCacheEntryInfo> entries) {
		this.entries=entries;
	}

	public Long getLastModified() {
		return lastModified;
	}

	public void setLastModified(Long lastModified) {
		this.lastModified=lastModified;
	}

	public PredicateStateCacheEntrySigned getPredicateStateCacheEntrySigned() {
		return predicateStateCacheEntrySigned;
	}

	public void setPredicateStateCacheEntrySigned(PredicateStateCacheEntrySigned predicateStateCacheEntrySigned) {
		this.predicateStateCacheEntrySigned=predicateStateCacheEntrySigned;
	}

	@XmlTransient
	public Instant getLastModifiedInstant() {
		if (null==lastModified) {
			return null;
		}
		return Instant.ofEpochMilli(lastModified);
	}

	/**
	 * get entries contained in this instance but missing in the passed instance
	 *
	 * @param other instance to check for missing entries
	 * @return entries contained in this instance but missing in the passed instance
	 */
	public ImmutableSet<StateCacheEntryInfo> getMissingEntries(StateCacheInfo other) {
		final Set<StateCacheEntryInfo> otherEntries=null==other.getEntries() ? ImmutableSet.of() : other.getEntries();
		return ImmutableSet.copyOf(Sets.difference(getEntries(), otherEntries));
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=97*hash+Objects.hashCode(this.baseName);
		hash=97*hash+this.version;
		hash=97*hash+Objects.hashCode(this.entries);
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
		final StateCacheInfo other=(StateCacheInfo) obj;
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

	@Override
	public String toString() {
		return "DxvkStateCacheInfo{"+"executableInfo="+baseName+", version="+version+", entrySize="+entrySize+", lastModified="+lastModified+'}';
	}

}
