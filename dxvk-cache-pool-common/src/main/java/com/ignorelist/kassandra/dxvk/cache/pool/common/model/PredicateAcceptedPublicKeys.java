/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Predicate;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class PredicateAcceptedPublicKeys implements Serializable, Predicate<StateCacheEntrySigned> {

	private Set<PublicKeyInfo> acceptedPublicKeys;

	public PredicateAcceptedPublicKeys() {
	}

	public PredicateAcceptedPublicKeys(Set<PublicKeyInfo> acceptedPublicKeys) {
		this.acceptedPublicKeys=acceptedPublicKeys;
	}

	public Set<PublicKeyInfo> getAcceptedPublicKeys() {
		return acceptedPublicKeys;
	}

	public void setAcceptedPublicKeys(Set<PublicKeyInfo> acceptedPublicKeys) {
		this.acceptedPublicKeys=acceptedPublicKeys;
	}

	@Override
	public boolean apply(StateCacheEntrySigned entry) {
		if (null!=acceptedPublicKeys&&!acceptedPublicKeys.isEmpty()) {
			return null!=entry.getSignatures()&&!Collections.disjoint(acceptedPublicKeys, entry.getSignatures());
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=89*hash+Objects.hashCode(this.acceptedPublicKeys);
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
		final PredicateAcceptedPublicKeys other=(PredicateAcceptedPublicKeys) obj;
		if (!Objects.equals(this.acceptedPublicKeys, other.acceptedPublicKeys)) {
			return false;
		}
		return true;
	}

}
