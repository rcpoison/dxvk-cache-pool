/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.IdentityStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import java.util.Set;

/**
 *
 * @author poison
 */
public class PredicateSignature implements Predicate<SignaturePublicKeyInfo> {

	private final IdentityStorage identityStorage;
	private final ImmutableSet<PublicKeyInfo> acceptedPublicKeys;
	private final boolean onlyAcceptVerifiedKeys;

	public PredicateSignature(final IdentityStorage identityStorage, final ImmutableSet<PublicKeyInfo> acceptedPublicKeys, final boolean onlyAcceptVerifiedKeys) {
		this.identityStorage=identityStorage;
		this.acceptedPublicKeys=acceptedPublicKeys;
		this.onlyAcceptVerifiedKeys=onlyAcceptVerifiedKeys;
	}

	@Override
	public boolean apply(SignaturePublicKeyInfo input) {
		boolean accept=true;
		if (onlyAcceptVerifiedKeys) {
			accept&=null!=identityStorage.getIdentity(input.getPublicKeyInfo());
		}
		if (null!=acceptedPublicKeys) {
			accept&=acceptedPublicKeys.contains(input.getPublicKeyInfo());
		}
		return accept;
	}

	public static PredicateSignature buildFrom(final IdentityStorage identityStorage, final PredicateStateCacheEntrySigned predicateStateCacheEntrySigned) {
		if (null!=predicateStateCacheEntrySigned.getAcceptedPublicKeys()) {
			Set<PublicKeyInfo> accepted=predicateStateCacheEntrySigned.getAcceptedPublicKeys().getAcceptedPublicKeys();
			if (null!=accepted&&!accepted.isEmpty()) {
				return new PredicateSignature(identityStorage, ImmutableSet.copyOf(accepted), predicateStateCacheEntrySigned.isOnlyAcceptVerifiedKeys());
			}
		}
		return new PredicateSignature(identityStorage, null, predicateStateCacheEntrySigned.isOnlyAcceptVerifiedKeys());
	}

}
