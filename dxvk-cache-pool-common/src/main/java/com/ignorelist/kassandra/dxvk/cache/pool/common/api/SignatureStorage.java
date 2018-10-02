/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignaturePublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntryInfo;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface SignatureStorage {

	void addSignee(final StateCacheEntryInfo entryInfo, final SignaturePublicKeyInfo signaturePublicKeyInfo);

	Set<SignaturePublicKeyInfo> getSignatures(final StateCacheEntryInfo entryInfo);

	Set<PublicKeyInfo> getSignedBy(final StateCacheEntryInfo entryInfo);

	void storePublicKey(final PublicKey publicKey) throws IOException;

	PublicKey getPublicKey(final PublicKeyInfo keyInfo);

	Identity getIdentity(final PublicKeyInfo keyInfo);

}
