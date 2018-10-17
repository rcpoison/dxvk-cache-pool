/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;

/**
 *
 * @author poison
 */
public class PredicatePublicKeyInfo implements Predicate<PublicKeyInfo> {

	private final IdentityStorage identityStorage;
	private final ImmutableSet<PublicKeyInfo> acceptedPublicKeys;
	private final boolean onlyAcceptVerifiedKeys;

	public PredicatePublicKeyInfo(final IdentityStorage identityStorage, final ImmutableSet<PublicKeyInfo> acceptedPublicKeys, final boolean onlyAcceptVerifiedKeys) {
		this.identityStorage=identityStorage;
		this.acceptedPublicKeys=acceptedPublicKeys;
		this.onlyAcceptVerifiedKeys=onlyAcceptVerifiedKeys;
	}

	@Override
	public boolean apply(PublicKeyInfo input) {
		boolean accept=true;
		if (onlyAcceptVerifiedKeys) {
			accept&=null!=identityStorage.getIdentity(input);
		}
		if (null!=acceptedPublicKeys) {
			accept&=acceptedPublicKeys.contains(input);
		}
		return accept;
	}

}
