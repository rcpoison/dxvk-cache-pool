/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Predicate;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import java.util.Objects;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class SignaturePredicate implements Predicate<StateCacheEntrySigned> {

	private Set<PublicKeyInfo> acceptedPublicKeys;
	private Integer minimumSignatures;

	public SignaturePredicate() {
	}

	public SignaturePredicate(Set<PublicKeyInfo> acceptedPublicKeys, Integer minimumSignatures) {
		this.acceptedPublicKeys=acceptedPublicKeys;
		this.minimumSignatures=minimumSignatures;
	}

	public Set<PublicKeyInfo> getAcceptedPublicKeys() {
		return acceptedPublicKeys;
	}

	public void setAcceptedPublicKeys(Set<PublicKeyInfo> acceptedPublicKeys) {
		this.acceptedPublicKeys=acceptedPublicKeys;
	}

	public Integer getMinimumSignatures() {
		return minimumSignatures;
	}

	public void setMinimumSignatures(Integer minimumSignatures) {
		this.minimumSignatures=minimumSignatures;
	}

	@Override
	public boolean apply(StateCacheEntrySigned entry) {
		if (null!=acceptedPublicKeys&&!acceptedPublicKeys.isEmpty()) {
			return entry.getSignatures().containsAll(acceptedPublicKeys);
		}
		if (null!=minimumSignatures&&0!=minimumSignatures) {
			return entry.getSignatures().size()>=minimumSignatures;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=37*hash+Objects.hashCode(this.acceptedPublicKeys);
		hash=37*hash+Objects.hashCode(this.minimumSignatures);
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
		final SignaturePredicate other=(SignaturePredicate) obj;
		if (!Objects.equals(this.acceptedPublicKeys, other.acceptedPublicKeys)) {
			return false;
		}
		if (!Objects.equals(this.minimumSignatures, other.minimumSignatures)) {
			return false;
		}
		return true;
	}

}
