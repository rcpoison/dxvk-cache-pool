/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityWithVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface IdentityStorage extends Closeable {

	Identity getIdentity(final PublicKeyInfo keyInfo);

	IdentityVerification getIdentityVerification(PublicKeyInfo publicKeyInfo);

	void storeIdentity(IdentityWithVerification identityWithVerification) throws IOException;

	Set<PublicKeyInfo> getVerifiedKeyInfos();

}
