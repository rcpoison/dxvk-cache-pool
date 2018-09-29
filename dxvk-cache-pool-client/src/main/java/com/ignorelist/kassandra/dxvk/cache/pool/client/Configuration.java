/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import java.nio.file.Path;
import java.util.Set;

/**
 *
 * @author poison
 */
public class Configuration {

	private String host="http://localhost:16969";
	private Path cacheTargetPath;
	private Set<Path> gamePaths;
	private boolean verbose=false;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host=host;
	}

	public Path getCacheTargetPath() {
		return cacheTargetPath;
	}

	public void setCacheTargetPath(Path cacheTargetPath) {
		this.cacheTargetPath=cacheTargetPath;
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
