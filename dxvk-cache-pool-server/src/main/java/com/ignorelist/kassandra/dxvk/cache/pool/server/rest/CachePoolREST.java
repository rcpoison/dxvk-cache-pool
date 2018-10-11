/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.rest;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.validators.StateCacheValidator;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorageSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.IdentityStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.SignatureStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityWithVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignatureCount;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.server.Configuration;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author poison
 */
@Path("pool")
public class CachePoolREST implements CacheStorage, CacheStorageSigned, IdentityStorage {

	private static final Logger LOG=Logger.getLogger(CachePoolREST.class.getName());

	private static final int PROTOCOL_VERSION=1;

	@Inject
	private Configuration configuration;
	@Inject
	private CacheStorage cacheStorage;
	@Inject
	private SignatureStorage signatureStorage;
	@Inject
	private CacheStorageSigned cacheStorageSigned;
	@Inject
	private ExecutorService executorService;

	@GET
	@Path("protocolVersion")
	@Produces(MediaType.TEXT_PLAIN)
	public String getProtocolVersion() {
		return Integer.toString(PROTOCOL_VERSION);
	}

	@POST
	@Path("cacheDescriptors/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheInfo> getCacheDescriptors(@PathParam("version") int version, Set<String> baseNames) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseNames) {
			throw new IllegalArgumentException("missing executableInfos");
		}
		return cacheStorage.getCacheDescriptors(version, baseNames);
	}

	@GET
	@Path("cacheDescriptor/{version}/{baseName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheInfo getCacheDescriptor(@PathParam("version") int version, @PathParam("baseName") String baseName) {
		return Iterables.getOnlyElement(getCacheDescriptors(version, ImmutableSet.of(baseName)));
	}

	@POST
	@Path("cacheDescriptorsSignees/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheInfoSignees> getCacheDescriptorsSignees(@PathParam("version") int version, Set<String> baseNames) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseNames) {
			throw new IllegalArgumentException("missing executableInfos");
		}
		return cacheStorageSigned.getCacheDescriptorsSignees(version, baseNames);
	}

	@GET
	@Path("cacheDescriptorSignees/{version}/{baseName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheInfoSignees getCacheDescriptorSignees(@PathParam("version") int version, @PathParam("baseName") String baseName) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseName) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorageSigned.getCacheDescriptorSignees(version, baseName);
	}

	@GET
	@Path("stateCache/{version}/{baseName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCache getCache(@PathParam("version") int version, @PathParam("baseName") String baseName) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseName) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorage.getCache(version, baseName);
	}

	@GET
	@Path("stateCacheSigned/{version}/{baseName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheSigned getCacheSigned(@PathParam("version") int version, @PathParam("baseName") String baseName) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseName) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorageSigned.getCacheSigned(version, baseName);
	}

	@POST
	@Path("stateCacheSigned/{version}/{baseName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public StateCacheSigned getCacheSigned(@PathParam("version") int version, @PathParam("baseName") String baseName, PredicateStateCacheEntrySigned predicateStateCacheEntrySigned) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (null==baseName) {
			throw new IllegalArgumentException("missing executableInfo");
		}
		return cacheStorageSigned.getCacheSigned(version, baseName, predicateStateCacheEntrySigned);
	}

	@POST
	@Path("missingCacheEntries")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheEntry> getMissingEntries(StateCacheInfo cacheInfo) {
		StateCacheHeaderInfo.getEntrySize(cacheInfo.getVersion());
		if (null==cacheInfo) {
			throw new IllegalArgumentException("missing cacheInfo");
		}
		return cacheStorage.getMissingEntries(cacheInfo);
	}

	@POST
	@Path("missingCacheEntriesSigned")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<StateCacheEntrySigned> getMissingEntriesSigned(StateCacheInfo cacheInfo) {
		StateCacheHeaderInfo.getEntrySize(cacheInfo.getVersion());
		if (null==cacheInfo) {
			throw new IllegalArgumentException("missing cacheInfo");
		}
		return cacheStorageSigned.getMissingEntriesSigned(cacheInfo);
	}

	@POST
	@Path("store")
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public void store(StateCache dxvkStateCache) throws IOException {
		if (null==dxvkStateCache) {
			throw new IllegalArgumentException("missing dxvkStateCache");
		}
		new StateCacheValidator().validate(dxvkStateCache);
		// don't trust passed hashes, just rebuild the entries
		final ImmutableSet<StateCacheEntry> entyCopies=dxvkStateCache.getEntries().parallelStream()
				.map(StateCacheEntry::copySafe)
				.collect(ImmutableSet.toImmutableSet());
		dxvkStateCache.setEntries(entyCopies);
		cacheStorage.store(dxvkStateCache);
	}

	@POST
	@Path("storeSigned")
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public void storeSigned(StateCacheSigned cache) throws IOException {
		if (null==cache) {
			throw new IllegalArgumentException("missing cache");
		}
		new StateCacheValidator().validate(cache);
		executorService.submit(() -> {
			try {
				cacheStorageSigned.storeSigned(cache);
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, null, ex);
			}
		});
	}

	@GET
	@Path("find/{version}/{subString}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<String> findBaseNames(@PathParam("version") int version, @PathParam("subString") String subString) {
		StateCacheHeaderInfo.getEntrySize(version);
		if (Strings.isNullOrEmpty(subString)) {
			throw new IllegalArgumentException("search string must not be empty");
		}
		return cacheStorage.findBaseNames(version, subString);
	}

	@POST
	@Path("publicKey")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PublicKey getPublicKey(PublicKeyInfo keyInfo) {
		if (null==keyInfo) {
			throw new IllegalArgumentException("missing keyInfo");
		}
		return signatureStorage.getPublicKey(keyInfo);
	}

	@POST
	@Path("publicKeys")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Set<PublicKey> getPublicKeys(Set<PublicKeyInfo> keyInfos) {
		if (null==keyInfos) {
			throw new IllegalArgumentException("missing keyInfo");
		}
		return keyInfos.parallelStream()
				.map(signatureStorage::getPublicKey)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
	}

	@POST
	@Path("identity")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Identity getIdentity(PublicKeyInfo keyInfo) {
		return signatureStorage.getIdentity(keyInfo);
	}

	@POST
	@Path("identityVerification")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public IdentityVerification getIdentityVerification(PublicKeyInfo publicKeyInfo) {
		return signatureStorage.getIdentityVerification(publicKeyInfo);
	}

	@GET
	@Path("verifiedKeyInfos")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<PublicKeyInfo> getVerifiedKeyInfos() {
		return signatureStorage.getVerifiedKeyInfos();
	}

	@GET
	@Path("signatureCounts/{version}/{baseName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Set<SignatureCount> getSignatureCounts(@PathParam("version") int version, @PathParam("baseName") String baseName) {
		return cacheStorageSigned.getSignatureCounts(version, baseName);
	}

	@Override
	public void storeIdentity(IdentityWithVerification identityWithVerification) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //TODO: implement
	}

	@Override
	public void close() throws IOException {
	}

}
