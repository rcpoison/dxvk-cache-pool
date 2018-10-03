/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

/**
 *
 * @author poison
 */
public interface StateCacheMeta {

	int getEntrySize();

	String getBaseName();

	int getVersion();

	void setBaseName(String baseName);

	void setEntrySize(int entrySize);

	void setVersion(int version);

	/**
	 * copy without entries
	 *
	 * @param cache
	 */
	default void copyShallowTo(StateCacheMeta cache) {
		cache.setVersion(getVersion());
		cache.setEntrySize(getEntrySize());
		cache.setBaseName(getBaseName());
	}

}
