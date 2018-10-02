/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheEntrySigned implements Serializable {

	private static final Logger LOG=Logger.getLogger(StateCacheEntrySigned.class.getName());

	private StateCacheEntry cacheEntry;
	private Set<SignaturePublicKeyInfo> signatures;

	public StateCacheEntrySigned() {
	}

	public StateCacheEntrySigned(StateCacheEntry cacheEntry, Set<SignaturePublicKeyInfo> signatures) {
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

	/**
	 * get valid signatures
	 *
	 * @param keyAccessor
	 * @return
	 */
	public ImmutableSet<SignaturePublicKeyInfo> verify(Function<PublicKeyInfo, PublicKey> keyAccessor) {
		if (null==getSignatures()) {
			return ImmutableSet.of();
		}
		ImmutableSet.Builder<SignaturePublicKeyInfo> validSignatures=ImmutableSet.<SignaturePublicKeyInfo>builder();
		for (SignaturePublicKeyInfo signature : getSignatures()) {
			final PublicKeyInfo publicKeyInfo=signature.getPublicKeyInfo();
			final PublicKey publicKey=keyAccessor.apply(publicKeyInfo);
			if (null==publicKey) {
				LOG.warning("public key not found for: "+publicKeyInfo);
				continue;
			}
			try {
				if (CryptoUtil.verify(getCacheEntry().getEntry(), publicKey, signature.getSignature().getSignature())) {
					validSignatures.add(signature);
				}
			} catch (Exception ex) {
				Logger.getLogger(StateCacheEntrySigned.class.getName()).log(Level.SEVERE, "failed to verify: "+publicKeyInfo, ex);
			}
		}
		return validSignatures.build();
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
		final StateCacheEntrySigned other=(StateCacheEntrySigned) obj;
		if (!Objects.equals(this.cacheEntry, other.cacheEntry)) {
			return false;
		}
		return true;
	}

}
