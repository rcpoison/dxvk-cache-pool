/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;

/**
 *
 * @author poison
 */
public interface IdentityStorage {
	

	Identity getIdentity(final PublicKeyInfo keyInfo);
	
}
