/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.ignorelist.kassandra.dxvk.cache.pool.client.rest.DxvkCachePoolRestClient;
import com.ignorelist.kassandra.dxvk.cache.pool.common.DxvkStateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 *
 * @author poison
 */
public class DxvkCachePoolClient {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		final Path path=Paths.get("/usr/local/games/SteamLibrary/Steam/SteamApps/common/Beat Saber/Beat Saber.dxvk-cache");
		DxvkStateCache cache=DxvkStateCacheIO.parse(path);
		try (DxvkCachePoolRestClient restClient=new DxvkCachePoolRestClient("http://localhost:16969")) {
			restClient.store(cache);
			DxvkStateCache stateCache=restClient.getCache(cache.getVersion(), cache.getExecutableInfo());
			System.err.println(Objects.equals(cache, stateCache));
		}
		
	}

}
