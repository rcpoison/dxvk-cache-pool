/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Signature;
import java.io.Serializable;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheEntrySignedRequest implements Serializable {

	private StateCacheEntry cacheEntry;
	private Signature signature;

	public StateCacheEntrySignedRequest() {
	}

	public StateCacheEntrySignedRequest(StateCacheEntry cacheEntry, Signature signature) {
		this.cacheEntry=cacheEntry;
		this.signature=signature;
	}

	@NotNull
	@XmlElement(required=true)
	public StateCacheEntry getCacheEntry() {
		return cacheEntry;
	}

	public void setCacheEntry(StateCacheEntry cacheEntry) {
		this.cacheEntry=cacheEntry;
	}

	@NotNull
	@XmlElement(required=true)
	public Signature getSignature() {
		return signature;
	}

	public void setSignature(Signature signature) {
		this.signature=signature;
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=53*hash+Objects.hashCode(this.cacheEntry);
		hash=53*hash+Objects.hashCode(this.signature);
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
		final StateCacheEntrySignedRequest other=(StateCacheEntrySignedRequest) obj;
		if (!Objects.equals(this.cacheEntry, other.cacheEntry)) {
			return false;
		}
		if (!Objects.equals(this.signature, other.signature)) {
			return false;
		}
		return true;
	}

}
