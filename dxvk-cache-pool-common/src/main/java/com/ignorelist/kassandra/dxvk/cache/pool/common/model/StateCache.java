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
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import java.io.Serializable;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author poison
 */
public class StateCache implements StateCacheMeta, Serializable {

	private static final Logger LOG=Logger.getLogger(StateCache.class.getName());

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

	@Override
	public void setBaseName(String baseName) {
		this.baseName=baseName;
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
	 * @param cache
	 * @return
	 */
	public void copyShallowTo(StateCacheMeta cache) {
		cache.setVersion(getVersion());
		cache.setEntrySize(getEntrySize());
		cache.setBaseName(getBaseName());
	}

	/**
	 * copy without entries
	 *
	 * @return
	 */
	public StateCache copyShallow() {
		StateCache cache=new StateCache();
		copyShallowTo(cache);
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

	public StateCacheSigned sign(final PrivateKey privateKey, final PublicKey publicKey) {
		StateCacheSigned cacheSigned=new StateCacheSigned();
		copyShallowTo(cacheSigned);
		cacheSigned.setPublicKeys(ImmutableSet.of(publicKey));
		if (null!=entries) {
			ImmutableSet<StateCacheEntrySigned> signedEntries=getEntries().parallelStream()
					.map(e -> {
						try {
							return e.sign(privateKey, publicKey);
						} catch (Exception ex) {
							LOG.log(Level.SEVERE, null, ex);
							throw new IllegalStateException("failed to sign entry", ex);
						}
					})
					.collect(ImmutableSet.toImmutableSet());
			cacheSigned.setEntries(signedEntries);
		} else {
			cacheSigned.setEntries(ImmutableSet.of());
		}
		return cacheSigned;
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
