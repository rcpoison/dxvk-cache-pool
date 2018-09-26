/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Equivalence;
import com.google.common.collect.Sets;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheDescriptor;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfoEquivalenceRelativePath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheStorageFS implements CacheStorage {

	private final Equivalence<ExecutableInfo> equivalence=new ExecutableInfoEquivalenceRelativePath();

	private final Path storageRoot;
	private ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheDescriptor> storageCache;

	public CacheStorageFS(Path storageRoot) {
		this.storageRoot=storageRoot;
	}
	
	private synchronized ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheDescriptor> getStorageCache() {
		if (null==storageCache) {
			ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheDescriptor> m=new ConcurrentHashMap<>();
			//Files.walk(storageRoot);
		}
		return storageCache;
	}

	@Override
	public Set<DxvkStateCacheDescriptor> getCacheDescriptors() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public DxvkStateCacheDescriptor getCacheDescriptor(ExecutableInfo executableInfo) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public DxvkStateCache getCache(ExecutableInfo executableInfo) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void store(final DxvkStateCache cache) throws IOException {
		final ExecutableInfo executableInfo=cache.getExecutableInfo();
		final Path targetPath=storageRoot.resolve(executableInfo.getRelativePath());
		final Equivalence.Wrapper<ExecutableInfo> executableInfoWrapper=equivalence.wrap(executableInfo);
		DxvkStateCacheDescriptor descriptor=getStorageCache().computeIfAbsent(executableInfoWrapper, w -> {
			DxvkStateCacheDescriptor d=new DxvkStateCacheDescriptor();
			d.setVersion(cache.getVersion());
			d.setEntrySize(cache.getEntrySize());
			d.setExecutableInfo(executableInfo);
			d.setEntries(Sets.newConcurrentHashSet());
			return d;
		});

		Files.createDirectories(targetPath);
		cache.getEntries().parallelStream();
	}

	private Path buildTargetPath(DxvkStateCache cache) {
		final ExecutableInfo executableInfo=cache.getExecutableInfo();
		final Path targetPath=storageRoot.resolve(Integer.toString(cache.getVersion())).resolve(Integer.toString(cache.getEntrySize())).resolve(executableInfo.getRelativePath());
		return targetPath;
	}

}
