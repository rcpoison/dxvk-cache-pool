/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.Striped;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.IdentityStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityWithVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author poison
 */
public class IdentityStorageFS implements IdentityStorage {

	private static final Logger LOG=Logger.getLogger(IdentityStorageFS.class.getName());
	private static final BaseEncoding BASE16=BaseEncoding.base16();

	private final ObjectMapper objectMapper;
	private final Path storageRoot;
	private final Interner<PublicKeyInfo> publicKeyInfoInterner;
	private final Striped<ReadWriteLock> storageLock=Striped.lazyWeakReadWriteLock(8);

	private ConcurrentMap<PublicKeyInfo, Identity> storageCache;

	public IdentityStorageFS(final Path storageRoot) throws IOException {
		this(storageRoot, Interners.newWeakInterner());
	}

	public IdentityStorageFS(final Path storageRoot, final Interner<PublicKeyInfo> publicKeyInfoInterner) throws IOException {
		this.storageRoot=storageRoot;
		Files.createDirectories(storageRoot);
		this.publicKeyInfoInterner=publicKeyInfoInterner;

		objectMapper=new ObjectMapper();
		JaxbAnnotationModule module=new JaxbAnnotationModule();
		objectMapper.registerModule(module);
	}

	private Lock getReadLock(PublicKeyInfo keyInfo) {
		final ReadWriteLock lock=storageLock.get(keyInfo);
		final Lock readLock=lock.readLock();
		return readLock;
	}

	private Lock getWriteLock(PublicKeyInfo keyInfo) {
		final ReadWriteLock lock=storageLock.get(keyInfo);
		final Lock writeLock=lock.writeLock();
		return writeLock;
	}

	private synchronized ConcurrentMap<PublicKeyInfo, Identity> readStorage() throws IOException {
		ImmutableSet<Path> files=Files.list(storageRoot)
				.filter(p -> Util.SHA_256_HEX_PATTERN.matcher(p.getFileName().toString()).matches())
				.filter(Files::isRegularFile)
				.collect(ImmutableSet.toImmutableSet());
		return files.parallelStream()
				.map(this::readIdentity)
				.collect(Collectors.toConcurrentMap(Identity::getPublicKey, Functions.identity()));
	}

	private Identity readIdentity(final Path file) {
		try {
			final Identity identitiy=objectMapper.readValue(file.toFile(), Identity.class);
			PublicKeyInfo interned=publicKeyInfoInterner.intern(identitiy.getPublicKey());
			identitiy.setPublicKeyInfo(interned);
			return identitiy;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void writeIdentity(final Path file, Identity identity) throws IOException {
		objectMapper.writeValue(file.toFile(), identity);
	}

	private synchronized ConcurrentMap<PublicKeyInfo, Identity> getStorageCache() throws IOException {
		if (null==storageCache) {
			storageCache=readStorage();
		}
		return storageCache;
	}

	public void init() throws IOException {
		getStorageCache();
	}

	private static String keyHashString(PublicKeyInfo keyInfo) {
		return BASE16.encode(keyInfo.getHash());
	}

	private Path buildFileNameIdentity(PublicKeyInfo keyInfo) {
		return storageRoot.resolve(keyHashString(keyInfo)+".json");
	}

	private Path buildFileNameGPG(PublicKeyInfo keyInfo) {
		return storageRoot.resolve(keyHashString(keyInfo)+".key");
	}

	private Path buildFileNameSignature(PublicKeyInfo keyInfo) {
		return storageRoot.resolve(keyHashString(keyInfo)+".sig");
	}

	@Override
	public Identity getIdentity(PublicKeyInfo keyInfo) {
		try {
			return getStorageCache().get(keyInfo);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, null, ex);
			return null;
		}
	}

	@Override
	public IdentityVerification getIdentityVerification(PublicKeyInfo publicKeyInfo) {
		try {
			if (getStorageCache().containsKey(publicKeyInfo)) {
				final Lock readLock=getReadLock(publicKeyInfo);
				readLock.lock();
				try {
					IdentityVerification iV=new IdentityVerification();
					iV.setPublicKeySignature(Files.readAllBytes(buildFileNameSignature(publicKeyInfo)));
					iV.setPublicKeyGPG(Files.readAllBytes(buildFileNameGPG(publicKeyInfo)));
				} finally {
					readLock.unlock();
				}
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, null, e);
		}
		return null;
	}

	@Override
	public void storeIdentity(IdentityWithVerification identityWithVerification) throws IOException {
		final Identity identity=identityWithVerification.getIdentity();
		final PublicKeyInfo publicKeyInfo=publicKeyInfoInterner.intern(identity.getPublicKey());
		if (!getStorageCache().containsKey(publicKeyInfo)) {
			identity.setPublicKeyInfo(publicKeyInfo);
			final Lock writeLock=getWriteLock(publicKeyInfo);
			try {
				writeIdentity(buildFileNameIdentity(publicKeyInfo), identity);
				final IdentityVerification identityVerification=identityWithVerification.getIdentityVerification();
				Files.write(buildFileNameSignature(publicKeyInfo), identityVerification.getPublicKeySignature());
				Files.write(buildFileNameGPG(publicKeyInfo), identityVerification.getPublicKeyGPG());
				getStorageCache().put(publicKeyInfo, identity);
			} finally {
				writeLock.unlock();
			}
		}
	}

}
