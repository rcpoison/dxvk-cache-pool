/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client.rest;

import java.io.Closeable;
import java.io.IOException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 *
 * @author poison
 */
abstract public class AbstractRestClient implements Closeable {

	private static final ObjectPool<Client> CLIENT_POOL=new GenericObjectPool<>(new JerseyClientFactory());
	private final String baseUrl;
	private final Client client;
	private final WebTarget webTarget;

	public AbstractRestClient(String baseUrl) {
		this.baseUrl=baseUrl;
		try {
			client=CLIENT_POOL.borrowObject();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		webTarget=client.target(baseUrl);
	}

	protected WebTarget getWebTarget() {
		return webTarget;
	}

	@Override
	public void close() throws IOException {
		try {
			CLIENT_POOL.returnObject(client);
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

}
