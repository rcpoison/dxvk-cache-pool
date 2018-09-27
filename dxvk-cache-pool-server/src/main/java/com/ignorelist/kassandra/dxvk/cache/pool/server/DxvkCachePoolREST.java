/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.FsScanner;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.CacheStorage;
import java.nio.file.Paths;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author poison
 */
@Path("pool")
public class DxvkCachePoolREST {

	@Inject
	private Configuration configuration;
	@Inject
	private CacheStorage cacheStorage;

	@GET
	@Path("executableInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<ExecutableInfo> executableInfos() {
		FsScanner fsScanner=FsScanner.scan(ImmutableSet.of(Paths.get("/home/poison/.wine/")));
		return fsScanner.getExecutables();
	}

	@POST
	@Path("executableInfo")
	public void executableInfos(Set<ExecutableInfo> executableInfos) {
		executableInfos.forEach(System.err::println);
	}

}
