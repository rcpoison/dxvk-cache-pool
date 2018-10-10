/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.rest;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import java.io.Serializable;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class SignatureCount implements Serializable {

	private int signatureCount;
	private int occurences;

	public SignatureCount() {
	}

	public SignatureCount(int signature, int count) {
		this.signatureCount=signature;
		this.occurences=count;
	}

	public SignatureCount(Multiset.Entry<Integer> e) {
		this(e.getElement(), e.getCount());
	}

	public int getSignatureCount() {
		return signatureCount;
	}

	public void setSignatureCount(int signatureCount) {
		this.signatureCount=signatureCount;
	}

	public int getOccurences() {
		return occurences;
	}

	public void setOccurences(int occurences) {
		this.occurences=occurences;
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=71*hash+this.signatureCount;
		hash=71*hash+this.occurences;
		return hash;
	}

	public static Set<SignatureCount> build(SortedMultiset<Integer> stats) {
		return stats.entrySet().stream()
				.map(SignatureCount::new)
				.collect(ImmutableSet.toImmutableSet());
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
		final SignatureCount other=(SignatureCount) obj;
		if (this.signatureCount!=other.signatureCount) {
			return false;
		}
		if (this.occurences!=other.occurences) {
			return false;
		}
		return true;
	}

}
