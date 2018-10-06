/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
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
public class StateCacheSigned implements Serializable, StateCacheMeta {

	@NotNull
	private String baseName;
	private int version;
	private int entrySize;
	private Set<PublicKey> publicKeys;
	private Set<StateCacheEntrySigned> entries;

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

	public Set<StateCacheEntrySigned> getEntries() {
		return entries;
	}

	public void setEntries(Set<StateCacheEntrySigned> entries) {
		this.entries=entries;
	}

	public Set<PublicKey> getPublicKeys() {
		return publicKeys;
	}

	public void setPublicKeys(Set<PublicKey> publicKeys) {
		this.publicKeys=publicKeys;
	}

	public ImmutableMap<PublicKeyInfo, java.security.PublicKey> buildUsedPublicKeyMap() {
		return getPublicKeys().stream()
				.collect(ImmutableMap.toImmutableMap(PublicKey::getKeyInfo, v -> {
					try {
						return CryptoUtil.decodePublicKey(v);
					} catch (Exception ex) {
						throw new IllegalStateException(ex);
					}
				}));
	}

	public boolean verifyAllSignaturesValid() {
		final ImmutableMap<PublicKeyInfo, java.security.PublicKey> keysByInfo=buildUsedPublicKeyMap();
		return getEntries().parallelStream()
				.allMatch(e -> e.verifyAllSignaturesValid(keysByInfo::get));
	}

	public StateCache toUnsigned() {
		StateCache cache=new StateCache();
		copyShallowTo(cache);
		final ImmutableSet<StateCacheEntry> entries=getEntries().stream()
				.map(StateCacheEntrySigned::getCacheEntry)
				.collect(ImmutableSet.toImmutableSet());
		cache.setEntries(entries);
		return cache;
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
		final StateCacheSigned other=(StateCacheSigned) obj;
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
