/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class BaseName implements Serializable {

	private String baseName;

	public BaseName() {
	}

	public BaseName(String baseName) {
		this.baseName=baseName;
	}

	public String getBaseName() {
		return baseName;
	}

	public void setBaseName(String baseName) {
		this.baseName=baseName;
	}

	@Override
	public int hashCode() {
		int hash=7;
		hash=29*hash+Objects.hashCode(this.baseName);
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
		final BaseName other=(BaseName) obj;
		if (!Objects.equals(this.baseName, other.baseName)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return baseName;
	}

}
