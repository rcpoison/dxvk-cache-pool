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
	private Set<Path> paths;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host=host;
	}

	public Set<Path> getPaths() {
		return paths;
	}

	public void setPaths(Set<Path> paths) {
		this.paths=paths;
	}

}
