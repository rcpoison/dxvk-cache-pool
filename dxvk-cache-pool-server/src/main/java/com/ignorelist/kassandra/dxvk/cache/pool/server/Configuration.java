/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 *
 * @author poison
 */
public class Configuration {

	private int port=16969;
	private Path storage=Paths.get(System.getProperty("user.home"), ".local", "share", "dxvk-cache-pool-server", "storage");
	private Set<Integer> versions=StateCacheHeaderInfo.getKnownVersions();

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port=port;
	}

	public Path getStorage() {
		return storage;
	}

	public void setStorage(Path storage) {
		this.storage=storage;
	}

	public Set<Integer> getVersions() {
		return versions;
	}

	public void setVersions(Set<Integer> versions) {
		this.versions=versions;
	}

}
