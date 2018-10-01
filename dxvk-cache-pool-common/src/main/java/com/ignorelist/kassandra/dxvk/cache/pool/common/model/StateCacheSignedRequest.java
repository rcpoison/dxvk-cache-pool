/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
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
public class StateCacheSignedRequest implements Serializable, StateCacheMeta {

	private String baseName;
	private int version;
	private int entrySize;
	private PublicKey publicKey;
	private Set<StateCacheEntrySignedRequest> entries;

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

	@NotNull
	@XmlElement(required=true)
	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey=publicKey;
	}

	@NotNull
	@XmlElement(required=true)
	public Set<StateCacheEntrySignedRequest> getEntries() {
		return entries;
	}

	public void setEntries(Set<StateCacheEntrySignedRequest> entries) {
		this.entries=entries;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=89*hash+Objects.hashCode(this.baseName);
		hash=89*hash+this.version;
		hash=89*hash+Objects.hashCode(this.publicKey);
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
		final StateCacheSignedRequest other=(StateCacheSignedRequest) obj;
		if (this.version!=other.version) {
			return false;
		}
		if (!Objects.equals(this.baseName, other.baseName)) {
			return false;
		}
		if (!Objects.equals(this.publicKey, other.publicKey)) {
			return false;
		}
		if (!Objects.equals(this.entries, other.entries)) {
			return false;
		}
		return true;
	}

}
