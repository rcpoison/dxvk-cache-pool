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

	private final IdentityStorage identityStorage;

	public IdentifiedFirstOrdering(SignatureStorage signatureStorage) {
		this.identityStorage=signatureStorage;
	}

	@Override
	public int compare(PublicKeyInfo left, PublicKeyInfo right) {
		return ComparisonChain.start()
				.compare(identityStorage.getIdentity(left), identityStorage.getIdentity(right), natural().nullsLast())
				.compare(left, right)
				.result();

	}

}
