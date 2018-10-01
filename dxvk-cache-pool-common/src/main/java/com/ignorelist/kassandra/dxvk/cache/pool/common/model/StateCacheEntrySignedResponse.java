/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Function;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheEntrySignedResponse implements Serializable {

	private StateCacheEntry cacheEntry;
	private Set<SignaturePublicKeyInfo> signatures;

	public StateCacheEntrySignedResponse() {
	}

	public StateCacheEntrySignedResponse(StateCacheEntry cacheEntry, Set<SignaturePublicKeyInfo> signatures) {
		this.cacheEntry=cacheEntry;
		this.signatures=signatures;
	}

	@NotNull
	@XmlElement(required=true)
	public StateCacheEntry getCacheEntry() {
		return cacheEntry;
	}

	public void setCacheEntry(StateCacheEntry cacheEntry) {
		this.cacheEntry=cacheEntry;
	}

	public Set<SignaturePublicKeyInfo> getSignatures() {
		return signatures;
	}

	public void setSignatures(Set<SignaturePublicKeyInfo> signatures) {
		this.signatures=signatures;
	}
	
	public boolean verify(Function<PublicKeyInfo, PublicKey> keyAccessor) {
		for (SignaturePublicKeyInfo signature : getSignatures()) {
			final PublicKey publicKey=keyAccessor.apply(signature.getPublicKeyInfo());
			if (null==publicKey) {
				return false;
			}
		}
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=71*hash+Objects.hashCode(this.cacheEntry);
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
		final StateCacheEntrySignedResponse other=(StateCacheEntrySignedResponse) obj;
		if (!Objects.equals(this.cacheEntry, other.cacheEntry)) {
			return false;
		}
		return true;
	}

}
