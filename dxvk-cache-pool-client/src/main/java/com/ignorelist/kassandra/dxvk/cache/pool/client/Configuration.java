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
	public static final Path CONFIG_SUBDIR=Paths.get("dxvk-cache-pool");

	private String host="http://kassandra.ignorelist.com:16969/";
	private Path cacheTargetPath;
	private Path configurationPath;
	private Path cacheReferencePath;
	private Set<Path> gamePaths;
	private boolean onlyVerified=false;
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

	public synchronized Path getConfigurationPath() throws IOException {
		if (null==configurationPath) {
			Path configHome=Util.getEnvPath("XDG_CONFIG_HOME");
			if (null==configHome) {
				configHome=Paths.get(System.getProperty("user.home"), ".config");
			}
			configurationPath=configHome.resolve(CONFIG_SUBDIR);
			Files.createDirectories(configurationPath);
		}
		return configurationPath;
	}

	public synchronized Path getCacheReferencePath() throws IOException {
		if (null==cacheReferencePath) {
			cacheReferencePath=getConfigurationPath().resolve("reference");
			Files.createDirectories(cacheReferencePath);
		}
		return cacheReferencePath;
	}

	public Set<Path> getGamePaths() {
		return gamePaths;
	}

	public void setGamePaths(Set<Path> gamePaths) {
		this.gamePaths=gamePaths;
	}

	public boolean isOnlyVerified() {
		return onlyVerified;
	}

	public void setOnlyVerified(boolean onlyVerified) {
		this.onlyVerified=onlyVerified;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose=verbose;
	}

}
