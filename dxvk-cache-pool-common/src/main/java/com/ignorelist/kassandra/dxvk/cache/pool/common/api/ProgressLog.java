/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

/**
 *
 * @author poison
 */
public interface ProgressLog {

	enum Level {
		MAIN,
		SUB,
		WANING,
		ERROR
	}

	void log(Level level, CharSequence message);

	void log(Level level, CharSequence prefix, CharSequence message);

}
