/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 *
 * @author poison
 */
public class Configuration {

	public static final String WINE_PREFIX_SYMLINK="dxvk-cache-pool";

	private String host="http://kassandra.ignorelist.com:16969/";
	private Path cacheTargetPath;
	private Set<Path> gamePaths;
	private boolean verbose=false;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host=host;
	}

	public synchronized Path getCacheTargetPath() throws IOException {
		if (null==cacheTargetPath) {
			Path xdgCacheHome=null;
			try {
				xdgCacheHome=Util.getEnvPath("XDG_CACHE_HOME");
				if (!Files.isDirectory(xdgCacheHome)) {
					xdgCacheHome=null;
				}
			} catch (Exception e) {
			}
			Path t=null;
			if (null!=xdgCacheHome) {
				t=xdgCacheHome.resolve(WINE_PREFIX_SYMLINK);
			} else {
				t=Paths.get(System.getProperty("user.home"), ".cache", WINE_PREFIX_SYMLINK);
			}
			Files.createDirectories(t);
			cacheTargetPath=t;
		}
		return cacheTargetPath;
	}

	public Set<Path> getGamePaths() {
		return gamePaths;
	}

	public void setGamePaths(Set<Path> gamePaths) {
		this.gamePaths=gamePaths;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose=verbose;
	}

}
