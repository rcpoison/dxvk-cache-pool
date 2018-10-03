/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class PredicateStateCacheEntrySigned implements Serializable, Predicate<StateCacheEntrySigned> {

	public static final Predicate<StateCacheEntrySigned> DEFAULT_PREDICATE=new PredicateMinimumSignatures(2);

	private PredicateAcceptedPublicKeys acceptedPublicKeys;
	private PredicateMinimumSignatures minimumSignatures;

	public PredicateStateCacheEntrySigned() {
	}

	public PredicateStateCacheEntrySigned(PredicateAcceptedPublicKeys acceptedPublicKeys, PredicateMinimumSignatures minimumSignatures) {
		this.acceptedPublicKeys=acceptedPublicKeys;
		this.minimumSignatures=minimumSignatures;
	}

	public PredicateAcceptedPublicKeys getAcceptedPublicKeys() {
		return acceptedPublicKeys;
	}

	public void setAcceptedPublicKeys(PredicateAcceptedPublicKeys acceptedPublicKeys) {
		this.acceptedPublicKeys=acceptedPublicKeys;
	}

	public PredicateMinimumSignatures getMinimumSignatures() {
		return minimumSignatures;
	}

	public void setMinimumSignatures(PredicateMinimumSignatures minimumSignatures) {
		this.minimumSignatures=minimumSignatures;
	}

	@Override
	public boolean apply(StateCacheEntrySigned input) {
		if (null==acceptedPublicKeys&&null==minimumSignatures) {
			return DEFAULT_PREDICATE.apply(input);
		}
		return Predicates
				.and(
						null==acceptedPublicKeys ? Predicates.alwaysTrue() : acceptedPublicKeys,
						null==minimumSignatures ? Predicates.alwaysTrue() : minimumSignatures)
				.apply(input);
	}

}
