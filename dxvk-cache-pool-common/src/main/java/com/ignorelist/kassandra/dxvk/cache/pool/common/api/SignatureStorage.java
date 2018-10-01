/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Signature;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface SignatureStorage {

	Identity getIdentity(final PublicKeyInfo keyInfo);

	PublicKey getPublicKey(final PublicKeyInfo keyInfo);

	Set<Signature> getSignatures(final StateCacheEntryInfo entryInfo);

	Set<PublicKeyInfo> getSignedBy(final StateCacheEntryInfo entryInfo);

}
