/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;

/**
 *
 * @author poison
 */
public class IdentifiedFirstOrdering extends Ordering<PublicKeyInfo> {

	private final SignatureStorage signatureStorage;

	public IdentifiedFirstOrdering(SignatureStorage signatureStorage) {
		this.signatureStorage=signatureStorage;
	}

	@Override
	public int compare(PublicKeyInfo left, PublicKeyInfo right) {
		return ComparisonChain.start()
				.compare(signatureStorage.getIdentity(left), signatureStorage.getIdentity(right), natural().nullsLast())
				.compare(left, right)
				.result();
		
	}

}
