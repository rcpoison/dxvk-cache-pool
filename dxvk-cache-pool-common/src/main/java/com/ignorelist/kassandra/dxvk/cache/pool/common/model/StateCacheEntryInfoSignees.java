/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class StateCacheEntryInfoSignees implements Serializable, StateCacheEntrySignees {

	private StateCacheEntryInfo entryInfo;
	private Set<PublicKeyInfo> publicKeyInfos;

	public StateCacheEntryInfoSignees() {
	}

	public StateCacheEntryInfoSignees(StateCacheEntryInfo entryInfo, Set<PublicKeyInfo> publicKeyInfos) {
		this.entryInfo=entryInfo;
		this.publicKeyInfos=publicKeyInfos;
	}

	@NotNull
	@XmlElement(required=true)
	public StateCacheEntryInfo getEntryInfo() {
		return entryInfo;
	}

	public void setEntryInfo(StateCacheEntryInfo entryInfo) {
		this.entryInfo=entryInfo;
	}

	@Override
	public Set<PublicKeyInfo> getPublicKeyInfos() {
		return publicKeyInfos;
	}

	public void setPublicKeyInfos(Set<PublicKeyInfo> publicKeyInfos) {
		this.publicKeyInfos=publicKeyInfos;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=47*hash+Objects.hashCode(this.entryInfo);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this==obj) {
			return true;
		}
		if (obj==null) {
			return false;
		}
		if (getClass()!=obj.getClass()) {
			return false;
		}
		final StateCacheEntryInfoSignees other=(StateCacheEntryInfoSignees) obj;
		if (!Objects.equals(this.entryInfo, other.entryInfo)) {
			return false;
		}
		return true;
	}

}
