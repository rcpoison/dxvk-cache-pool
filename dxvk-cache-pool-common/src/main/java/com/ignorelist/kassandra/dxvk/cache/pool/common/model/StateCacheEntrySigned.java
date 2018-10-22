/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheEntrySigned implements Serializable, StateCacheEntrySignees {

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
	public ImmutableSet<SignaturePublicKeyInfo> verifiedSignatures(final Function<PublicKeyInfo, PublicKey> keyAccessor) {
		if (null==getSignatures()) {
			return ImmutableSet.of();
		}
		ImmutableSet.Builder<SignaturePublicKeyInfo> validSignatures=ImmutableSet.<SignaturePublicKeyInfo>builder();
		for (SignaturePublicKeyInfo signature : getSignatures()) {
			final PublicKeyInfo publicKeyInfo=signature.getPublicKeyInfo();
			PublicKey publicKey=null;
			try {
				publicKey=keyAccessor.apply(publicKeyInfo);
			} catch (Exception e) {
				LOG.log(Level.FINE, "failed loading public key", e);
			}
			if (null==publicKey) {
				LOG.log(Level.WARNING, "public key not found for: {0}", publicKeyInfo);
				continue;
			}
			try {
				if (CryptoUtil.verify(getCacheEntry().getEntry(), publicKey, signature.getSignature().getSignature())) {
					validSignatures.add(signature);
				}
			} catch (Exception ex) {
				LOG.log(Level.WARNING, "failed to verify: "+publicKeyInfo, ex);
			}
		}
		return validSignatures.build();
	}

	public boolean verifyAllSignaturesValid(final Function<PublicKeyInfo, PublicKey> keyAccessor) {
		final ImmutableSet<SignaturePublicKeyInfo> verifiedSignatures=verifiedSignatures(keyAccessor);
		return Objects.equals(getSignatures(), verifiedSignatures);
	}

	public StateCacheEntrySigned copySafe() {
		StateCacheEntrySigned copy=new StateCacheEntrySigned();
		copy.setCacheEntry(getCacheEntry().copySafe());
		copy.setSignatures(getSignatures());
		return copy;
	}

	public StateCacheEntrySigned copyFilteredSignatures(Predicate<SignaturePublicKeyInfo> predicateSignature) {
		StateCacheEntrySigned copy=new StateCacheEntrySigned();
		copy.setCacheEntry(getCacheEntry());
		copy.setSignatures(ImmutableSet.copyOf(Iterables.filter(getSignatures(), predicateSignature)));
		return copy;
	}

	public static ImmutableSet<PublicKeyInfo> getUsedPublicKeyInfos(final Set<StateCacheEntrySigned> signedEntries) {
		if (null==signedEntries) {
			return ImmutableSet.of();
		}
		return signedEntries.parallelStream()
				.map(StateCacheEntrySigned::getSignatures)
				.filter(Predicates.notNull())
				.flatMap(Set::stream)
				.filter(Predicates.notNull())
				.map(SignaturePublicKeyInfo::getPublicKeyInfo)
				.collect(ImmutableSet.toImmutableSet());
	}

	@XmlTransient
	@Override
	public Set<PublicKeyInfo> getPublicKeyInfos() {
		return getSignatures().stream()
				.map(SignaturePublicKeyInfo::getPublicKeyInfo)
				.collect(ImmutableSet.toImmutableSet());
	}

	@XmlTransient
	@Override
	public int getSignatureCount() {
		return null==signatures ? 0 : signatures.size();
	}

	public static int countTotalSignatures(final Collection<StateCacheEntrySigned> signedEntries) {
		return signedEntries.stream()
				.mapToInt(StateCacheEntrySigned::getSignatureCount)
				.sum();
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
