package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorageSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.CacheStorageSignedFacade;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Injection binder
 *
 * @author poison
 */
public class ServerBinder extends AbstractBinder {

	private final Configuration configuration;
	private final CacheStorage cacheStorage;
	private final SignatureStorage signatureStorage;
	private final CacheStorageSigned cacheStorageSigned;
	private final ForkJoinPool forkJoinPool;
	private final ScheduledExecutorService scheduledExecutorService;

	public ServerBinder(final Configuration configuration, final CacheStorage cacheStorage, final SignatureStorage signatureStorage, final ForkJoinPool forkJoinPool, final ScheduledExecutorService scheduledExecutorService) {
		this.configuration=configuration;
		this.cacheStorage=cacheStorage;
		this.signatureStorage=signatureStorage;
		cacheStorageSigned=new CacheStorageSignedFacade(cacheStorage, signatureStorage);
		this.forkJoinPool=forkJoinPool;
		this.scheduledExecutorService=scheduledExecutorService;
	}

	@Override
	protected void configure() {
		bind(configuration).to(Configuration.class);
		bind(cacheStorage).to(CacheStorage.class);
		bind(signatureStorage).to(SignatureStorage.class);
		bind(cacheStorageSigned).to(CacheStorageSigned.class);
		bind(forkJoinPool).to(ForkJoinPool.class);
		bind(scheduledExecutorService).to(ExecutorService.class);
		bind(scheduledExecutorService).to(ScheduledExecutorService.class);
	}

}
