/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.crypto;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class SignatureCount implements Serializable, Comparable<SignatureCount> {

	private static final Comparator<SignatureCount> DEFAULT_COMPARATOR=Comparator
			.comparingInt(SignatureCount::getSignatureCount)
			.thenComparingInt(SignatureCount::getEntryCount);

	private int signatureCount;
	private int entryCount;

	public SignatureCount() {
	}

	public SignatureCount(int signature, int count) {
		this.signatureCount=signature;
		this.entryCount=count;
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

	public int getEntryCount() {
		return entryCount;
	}

	public void setEntryCount(int entryCount) {
		this.entryCount=entryCount;
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=71*hash+this.signatureCount;
		hash=71*hash+this.entryCount;
		return hash;
	}

	public static Set<SignatureCount> build(Multiset<Integer> stats) {
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
		if (this.entryCount!=other.entryCount) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(SignatureCount o) {
		return DEFAULT_COMPARATOR.compare(this, o);
	}

}
