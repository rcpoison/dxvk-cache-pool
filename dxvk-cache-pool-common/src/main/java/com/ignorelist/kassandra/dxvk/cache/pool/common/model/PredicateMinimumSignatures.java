/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Predicate;
import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class PredicateMinimumSignatures implements Serializable, Predicate<StateCacheEntrySignees> {

	private Integer minimumSignatures;

	public PredicateMinimumSignatures() {
	}

	public PredicateMinimumSignatures(Integer minimumSignatures) {
		this.minimumSignatures=minimumSignatures;
	}

	public Integer getMinimumSignatures() {
		return minimumSignatures;
	}

	public void setMinimumSignatures(Integer minimumSignatures) {
		this.minimumSignatures=minimumSignatures;
	}

	@Override
	public boolean apply(StateCacheEntrySignees entry) {
		if (null!=minimumSignatures&&0!=minimumSignatures) {
			return entry.getSignatureCount()>=minimumSignatures;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=71*hash+Objects.hashCode(this.minimumSignatures);
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
		if (!(obj instanceof PredicateMinimumSignatures)) {
			return false;
		}
		final PredicateMinimumSignatures other=(PredicateMinimumSignatures) obj;
		if (!Objects.equals(this.minimumSignatures, other.minimumSignatures)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "PredicateMinimumSignatures{"+"minimumSignatures="+minimumSignatures+'}';
	}

}
