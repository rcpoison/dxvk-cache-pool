/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client.rest;

import javax.ws.rs.client.Client;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;

/**
 *
 * @author poison
 */
final class JerseyClientFactory extends BasePooledObjectFactory<Client> {

	private static final int CONNECT_TIMEOUT=1000;
	private static final int READ_TIMEOUT=30000;

	@Override
	public Client create() throws Exception {
		return new JerseyClientBuilder()
				.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT)
				.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT)
				.build();
	}

	@Override
	public PooledObject<Client> wrap(Client client) {
		return new DefaultPooledObject<>(client);
	}

	@Override
	public void destroyObject(PooledObject<Client> client) throws Exception {
		client.getObject().close();
	}

}
