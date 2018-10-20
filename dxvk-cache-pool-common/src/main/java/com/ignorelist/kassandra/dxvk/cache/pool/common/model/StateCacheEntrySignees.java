/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import java.util.Set;

/**
 *
 * @author poison
 */
public interface StateCacheEntrySignees {

	Set<PublicKeyInfo> getPublicKeyInfos();

	int getSignatureCount();

}
