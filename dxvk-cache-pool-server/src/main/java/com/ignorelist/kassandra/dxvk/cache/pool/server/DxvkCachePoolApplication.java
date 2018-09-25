package com.ignorelist.kassandra.dxvk.cache.pool.server;

import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author poison
 */
public class DxvkCachePoolApplication extends ResourceConfig {

	public DxvkCachePoolApplication(final Configuration configuration) {
		register(DxvkCachePoolREST.class);
		register(new ServerBinder(configuration));
	}

}
