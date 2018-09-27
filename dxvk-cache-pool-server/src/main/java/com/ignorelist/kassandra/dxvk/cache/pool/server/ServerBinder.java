package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.CacheStorage;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Injection binder
 *
 * @author poison
 */
public class ServerBinder extends AbstractBinder {

	private final Configuration configuration;
	private final CacheStorage cacheStorage;

	public ServerBinder(Configuration configuration, CacheStorage cacheStorage) {
		this.configuration=configuration;
		this.cacheStorage=cacheStorage;
	}

	@Override
	protected void configure() {
		bind(configuration).to(Configuration.class);
		bind(cacheStorage).to(CacheStorage.class);
	}

}
