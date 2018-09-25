package com.ignorelist.kassandra.dxvk.cache.pool.server;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Injection binder
 *
 * @author poison
 */
public class ServerBinder extends AbstractBinder {

	private final Configuration configuration;

	public ServerBinder(Configuration configuration) {
		this.configuration=configuration;
	}

	@Override
	protected void configure() {
		bind(configuration).to(Configuration.class);
	}

}
