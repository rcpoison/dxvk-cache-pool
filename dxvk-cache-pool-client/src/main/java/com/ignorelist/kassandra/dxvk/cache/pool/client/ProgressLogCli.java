/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.ignorelist.kassandra.dxvk.cache.pool.common.api.ProgressLog;

/**
 *
 * @author poison
 */
public class ProgressLogCli implements ProgressLog {

	@Override
	public void log(Level level, CharSequence message) {
		log(level, null, message);
	}

	@Override
	public void log(Level level, CharSequence prefix, CharSequence message) {
		if (Level.SUB==level) {
			System.err.print(" -> ");
		} else if (Level.WARNING==level) {
			System.err.print("Warning: ");
		} else if (Level.ERROR==level) {
			System.err.print("Error: ");
		}
		if (null!=prefix) {
			System.err.print(prefix);
			System.err.print(": ");
		}
		System.err.println(message);
	}

}
