/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.ignorelist.kassandra.dxvk.cache.pool.client.rest.CachePoolRestClient;
import com.ignorelist.kassandra.dxvk.cache.pool.common.FsScanner;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.ProgressLog;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.IdentityVerification;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.KeyStore;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateMinimumSignatures;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheMeta;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author poison
 */
public class CachePoolMerger {

	private final Configuration configuration;
	private final ProgressLog log=new ProgressLogCli();

	private final LoadingCache<PublicKeyInfo, PublicKey> publicKeyCache=CacheBuilder.newBuilder()
			.<PublicKeyInfo, PublicKey>build(new CacheLoader<PublicKeyInfo, PublicKey>() {
				@Override
				public PublicKey load(PublicKeyInfo key) throws Exception {
					try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
						com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey publicKey=restClient.getPublicKey(key);
						if (null==publicKey) {
							throw new NoSuchElementException();
						}
						return CryptoUtil.decodePublicKey(publicKey);
					}
				}
			});
	private FsScanner scanResult;
	private ImmutableSet<String> availableBaseNames;
	private ImmutableSet<String> remoteAvailableBaseNames;
	private ImmutableMap<String, StateCacheInfo> cacheDescriptorsByBaseName;
	private KeyStore keyStore;
	private PredicateStateCacheEntrySigned predicateStateCacheEntrySigned;

	public CachePoolMerger(Configuration c) {
		this.configuration=c;
	}

	public void verifyProtocolVersion() throws IOException {
		try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
			restClient.verifyProtocolVersion();
		}
	}

	public synchronized void downloadVerifiedKeyData() throws IOException {
		final Path targetPath=configuration.getConfigurationPath().resolve("keystore");
		log.log(ProgressLog.Level.MAIN, "downloading verified keys to: "+targetPath);
		final BaseEncoding base16=BaseEncoding.base16();
		Files.createDirectories(targetPath);
		final ImmutableSet<PublicKeyInfo> localVerifiedKeys=Files.list(targetPath)
				.filter(Files::isRegularFile)
				.map(Path::getFileName)
				.map(Path::toString)
				.filter(Util.SHA_256_HEX_PATTERN.asPredicate())
				.map(base16::decode)
				.map(PublicKeyInfo::new)
				.collect(ImmutableSet.toImmutableSet());
		try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
			final Set<PublicKeyInfo> availableVerifiedKeyInfos=restClient.getVerifiedKeyInfos();
			Sets.SetView<PublicKeyInfo> toDownload=Sets.difference(availableVerifiedKeyInfos, localVerifiedKeys);
			for (PublicKeyInfo publicKeyInfoToDownload : toDownload) {
				try {
					final String fileBaseName=base16.encode(publicKeyInfoToDownload.getHash());
					final PublicKey publicKey=publicKeyCache.get(publicKeyInfoToDownload);
					if (null==publicKey) {
						throw new IllegalStateException("publicKey must not be null");
					}
					final Identity identity=restClient.getIdentity(publicKeyInfoToDownload);
					if (null==identity) {
						throw new IllegalStateException("identity must not be null");
					}
					final IdentityVerification identityVerification=restClient.getIdentityVerification(publicKeyInfoToDownload);
					if (null==identityVerification) {
						throw new IllegalStateException("IdentityVerification must not be null");
					}
					log.log(ProgressLog.Level.SUB, "writing: "+identity.getName()+" <"+identity.getEmail()+">", fileBaseName);
					Files.write(targetPath.resolve(fileBaseName+".gpg"), identityVerification.getPublicKeyGPG());
					Files.write(targetPath.resolve(fileBaseName+".sig"), identityVerification.getPublicKeySignature());
					try (OutputStream out=Files.newOutputStream(targetPath.resolve(fileBaseName))) {
						CryptoUtil.write(out, publicKey);
					}
				} catch (Exception ex) {
					//throw new IOException(ex);
					log.log(ProgressLog.Level.WARNING, "failed downloading entry for: "+publicKeyInfoToDownload);
				}
			}
		}
	}

	public synchronized KeyStore getKeyStore() throws IOException {
		if (null==keyStore) {
			keyStore=new KeyStore(configuration.getConfigurationPath());
		}
		return keyStore;
	}

	private synchronized PredicateStateCacheEntrySigned getCacheEntryPredicate() {
		if (null==predicateStateCacheEntrySigned) {
			predicateStateCacheEntrySigned=new PredicateStateCacheEntrySigned();
			predicateStateCacheEntrySigned.setOnlyAcceptVerifiedKeys(configuration.isOnlyVerified());
			PredicateMinimumSignatures minimumSignatures=new PredicateMinimumSignatures(configuration.getMinimumSignatures());
			predicateStateCacheEntrySigned.setMinimumSignatures(minimumSignatures);
		}
		return predicateStateCacheEntrySigned;
	}

	public synchronized FsScanner getScanResult() throws IOException {
		if (null==scanResult) {
			log.log(ProgressLog.Level.MAIN, "scanning directories");
			scanResult=FsScanner.scan(configuration.getCacheTargetPath(), configuration.getGamePaths(), configuration.isScanRecursive());
			log.log(ProgressLog.Level.SUB, "scanned "+scanResult.getVisitedFiles()+" files");
		}
		return scanResult;
	}

	public synchronized ImmutableSet<String> getAvailableBaseNames() throws IOException {
		if (null==availableBaseNames) {
			availableBaseNames=ImmutableList.of(getScanResult().getExecutables(), getScanResult().getStateCaches())
					.stream()
					.flatMap(Collection::stream)
					.map(Util::baseName)
					.collect(ImmutableSet.toImmutableSet());
		}
		return availableBaseNames;
	}

	public synchronized ImmutableSet<String> getRemoteAvailableBaseNames() throws IOException {
		if (null==remoteAvailableBaseNames) {
			try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
				log.log(ProgressLog.Level.MAIN, "looking up remote base names for "+getAvailableBaseNames().size()+" possible games");
				remoteAvailableBaseNames=ImmutableSet.copyOf(restClient.getAvilableBaseNames(StateCacheHeaderInfo.getLatestVersion(), getAvailableBaseNames()));
				log.log(ProgressLog.Level.SUB, "found "+remoteAvailableBaseNames.size()+" matching base names");
			}
		}
		return remoteAvailableBaseNames;
	}

	public synchronized ImmutableMap<String, StateCacheInfo> getCacheDescriptorsByBaseNames() throws IOException {
		// TODO: we don't actually need the descriptors anymore since we're not diffing against the remote
		if (null==cacheDescriptorsByBaseName) {
			try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
				log.log(ProgressLog.Level.MAIN, "looking up remote caches for "+getAvailableBaseNames().size()+" possible games");
				final Set<StateCacheInfo> cacheDescriptors=restClient.getCacheDescriptors(StateCacheHeaderInfo.getLatestVersion(), getAvailableBaseNames());
				cacheDescriptorsByBaseName=Maps.uniqueIndex(cacheDescriptors, StateCacheMeta::getBaseName);
				//cacheDescriptorsByBaseNameUnsigned=ImmutableMap.copyOf(Maps.transformValues(cacheDescriptorsByBaseName, StateCacheInfoSignees::toUnsigned));
				log.log(ProgressLog.Level.SUB, "found "+cacheDescriptorsByBaseName.size()+" matching caches");
				if (configuration.isVerbose()) {
					cacheDescriptorsByBaseName.values().forEach(d -> {
						log.log(ProgressLog.Level.SUB, d.getBaseName(), "("+d.getEntries().size()+" entries)");
					});
				}

			}
		}
		return cacheDescriptorsByBaseName;
	}

	public synchronized void merge() throws IOException {
		getKeyStore();
		prepareWinePrefixes();
		downloadNew();
		mergeExisting();
		uploadUnknown();
	}

	/**
	 * create symlinks in wine prefixes to the target dir
	 *
	 * @throws IOException
	 */
	public synchronized void prepareWinePrefixes() throws IOException {
		final ImmutableSet<Path> wineRoots=getScanResult().getWineRoots();
		log.log(ProgressLog.Level.MAIN, "preparing wine prefixes");
		for (Path wineDriveC : wineRoots) {
			final Path symLink=wineDriveC.resolve(Configuration.WINE_PREFIX_SYMLINK);
			if (!Files.isSymbolicLink(symLink)) {
				if (Files.isDirectory(symLink, LinkOption.NOFOLLOW_LINKS)||Files.isRegularFile(symLink, LinkOption.NOFOLLOW_LINKS)) {
					log.log(ProgressLog.Level.WARNING, symLink.toString(), "exists and is a directory/file instead of a symlink. dxvk-cache-pool will not work for this wine prefix.");
				} else {
					log.log(ProgressLog.Level.SUB, "creating symlink from "+symLink+" to "+configuration.getCacheTargetPath());
					Files.createSymbolicLink(symLink, configuration.getCacheTargetPath());
				}
			}
		}
	}

	/**
	 * Download and write entries for which we have no local .dxvk-cache in the target directory
	 *
	 * @throws IOException
	 */
	public synchronized void downloadNew() throws IOException {
		if (!getCacheDescriptorsByBaseNames().isEmpty()) {
			// create new caches
			final ImmutableMap<String, Path> baseNameToCacheTarget=getScanResult().getBaseNameToCacheTarget();
			// remote cache entries which have no corresponsing .dxvk-cache file in the local target directory
			final Map<String, StateCacheInfo> entriesWithoutLocalCache=Maps.filterKeys(getCacheDescriptorsByBaseNames(), Predicates.not(baseNameToCacheTarget::containsKey));
			log.log(ProgressLog.Level.MAIN, "writing "+entriesWithoutLocalCache.size()+" new caches");
			if (!entriesWithoutLocalCache.isEmpty()) {
				try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
					for (StateCacheInfo cacheInfo : entriesWithoutLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path targetPath=Util.cacheFileForBaseName(configuration.getCacheTargetPath(), baseName);
						final StateCacheSigned cacheSigned=restClient.getCacheSigned(StateCacheHeaderInfo.getLatestVersion(), baseName, getCacheEntryPredicate());
						log.log(ProgressLog.Level.SUB, baseName, "downloaded "+cacheSigned.getEntries().size()+" cache entries");
						if (cacheSigned.getEntries().isEmpty()) {
							continue;
						}
						final int totalSignatureCount=StateCacheEntrySigned.countTotalSignatures(cacheSigned.getEntries());
						log.log(ProgressLog.Level.SUB, baseName, "verifying "+totalSignatureCount+" signatures for "+cacheSigned.getEntries().size()+" entries");
						if (!cacheSigned.verifyAllSignaturesValid()) {
							throw new IllegalStateException("signatures could not be verified!");
						}
						log.log(ProgressLog.Level.SUB, baseName, "writing to "+targetPath);
						final StateCache cacheUnsigned=cacheSigned.toUnsigned();
						StateCacheIO.writeAtomic(targetPath, cacheUnsigned);
						copyToReference(targetPath, baseName);
					}
				}
			}
		}
	}

	/**
	 * Merge existing local and remote caches
	 *
	 * @throws IOException
	 */
	public synchronized void mergeExisting() throws IOException {
		if (!getCacheDescriptorsByBaseNames().isEmpty()) {

			final ImmutableMap<String, Path> baseNameToCacheTarget=getScanResult().getBaseNameToCacheTarget();
			// remote cache entries which have a corresponsing .dxvk-cache file in the local target directory
			final Map<String, StateCacheInfo> entriesLocalCache=Maps.filterKeys(getCacheDescriptorsByBaseNames(), baseNameToCacheTarget::containsKey);
			log.log(ProgressLog.Level.MAIN, "updating "+entriesLocalCache.size()+" caches");
			if (!entriesLocalCache.isEmpty()) {
				try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
					for (StateCacheInfo cacheInfo : entriesLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path cacheFile=baseNameToCacheTarget.get(baseName);
						final StateCache localCache=StateCacheIO.parse(cacheFile);
						final StateCache localReferenceCache=readReference(localCache.getVersion(), baseName);

						//final StateCacheInfo cacheInfoUnsigned=cacheDescriptorsByBaseNameUnsigned.get(baseName);
						//final StateCacheInfo cacheInfoUnsigned=cacheInfo.toUnsigned();
						final StateCache locallyBuilt=localCache.diff(localReferenceCache);
						if (!locallyBuilt.getEntries().isEmpty()) {
							log.log(ProgressLog.Level.SUB, baseName, "signing "+locallyBuilt.getEntries().size()+" locally built entries");
							final StateCacheSigned locallyBuiltSigned=locallyBuilt.sign(getKeyStore().getPrivateKey(), getKeyStore().getPublicKey());
							log.log(ProgressLog.Level.SUB, baseName, "sending "+locallyBuilt.getEntries().size()+" locally built entries to remote");
							restClient.storeSigned(locallyBuiltSigned);
						}

						final int localCacheEntriesSize=localCache.getEntries().size();
						final StateCacheInfo localCacheInfo=localCache.toInfo();
						localCacheInfo.setPredicateStateCacheEntrySigned(getCacheEntryPredicate());
						final Set<StateCacheEntrySigned> missingEntries=restClient.getMissingEntriesSigned(localCacheInfo);
						if (missingEntries.isEmpty()) {
							log.log(ProgressLog.Level.SUB, baseName, "is up to date ("+localCacheEntriesSize+" entries)");
						} else {
							final int totalSignatureCount=StateCacheEntrySigned.countTotalSignatures(missingEntries);
							log.log(ProgressLog.Level.SUB, baseName, "verifying "+totalSignatureCount+" signatures for "+missingEntries.size()+" entries");
							final ImmutableSet<StateCacheEntry> verifiedMissingEntries=missingEntries.parallelStream()
									.filter(e -> e.verifyAllSignaturesValid(publicKeyCache::getUnchecked)) // TODO: optimize, single request
									.map(StateCacheEntrySigned::getCacheEntry)
									.collect(ImmutableSet.toImmutableSet());
							log.log(ProgressLog.Level.SUB, baseName, "patching ("+localCacheEntriesSize+" existing entries, adding "+missingEntries.size()+" entries)");
							localCache.patch(verifiedMissingEntries);
							StateCacheIO.writeAtomic(cacheFile, localCache);
						}
						copyToReference(cacheFile, baseName);
					}
				}
			}

		}
	}

	/**
	 * Upload caches which have no entry on the remote. Also copies them to the target directory so they're available if the env var
	 * is set.
	 *
	 * @throws IOException
	 */
	public synchronized void uploadUnknown() throws IOException {
		// upload unkown caches
		ImmutableMap<String, StateCacheInfo> descriptors=getCacheDescriptorsByBaseNames();
		ImmutableListMultimap<String, Path> cachePathsByBaseName=Multimaps.index(getScanResult().getStateCaches(), Util::baseName);
		ListMultimap<String, Path> pathsToUpload=Multimaps.filterKeys(cachePathsByBaseName, Predicates.not(descriptors::containsKey));
		log.log(ProgressLog.Level.MAIN, "found "+pathsToUpload.keySet().size()+" candidates for upload");
		try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
			for (Map.Entry<String, Collection<Path>> entry : pathsToUpload.asMap().entrySet()) {
				final String baseName=entry.getKey();
				final StateCache cache=readMerged(ImmutableSet.copyOf(entry.getValue()));
				log.log(ProgressLog.Level.SUB, baseName, "signing");
				final StateCacheSigned cacheSigned=cache.sign(getKeyStore().getPrivateKey(), getKeyStore().getPublicKey());
				log.log(ProgressLog.Level.SUB, baseName, "uploading");
				restClient.storeSigned(cacheSigned);

				final Path targetPath=Util.cacheFileForBaseName(configuration.getCacheTargetPath(), baseName);

				if (!Files.exists(targetPath)) {
					log.log(ProgressLog.Level.SUB, baseName, "does not yet exist in target directory, copying to "+targetPath);
					StateCacheIO.writeAtomic(targetPath, cache);
				}
				copyToReference(targetPath, baseName);
			}
		}
	}

	private Path buildReferencePath(final String baseName) throws IOException {
		final Path referencePath=configuration.getCacheReferencePath().resolve(baseName+Util.DXVK_CACHE_EXT+".gz");
		return referencePath;
	}

	private void copyToReference(final Path source, final String baseName) throws IOException {
		final Path referencePath=buildReferencePath(baseName);
		final Path referencePathTmp=buildReferencePath(baseName).resolveSibling(baseName+Util.DXVK_CACHE_EXT+".gz.tmp");
		Util.copyCompressed(source, referencePathTmp);
		Files.move(referencePathTmp, referencePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}

	private StateCache readReference(final int version, final String baseName) throws IOException {
		final Path referencePath=buildReferencePath(baseName);
		try (InputStream in=new BufferedInputStream(new GZIPInputStream(Files.newInputStream(referencePath)))) {
			return StateCacheIO.parse(in);
		} catch (IOException ioe) {
			log.log(ProgressLog.Level.SUB, baseName, "couldn't find reference cache, assuming generated locally");
			StateCache stateCache=new StateCache();
			stateCache.setVersion(version);
			stateCache.setEntrySize(StateCacheHeaderInfo.getEntrySize(version));
			stateCache.setBaseName(baseName);
			stateCache.setEntries(ImmutableSet.of());
			return stateCache;
		}
	}

	private static StateCache readMerged(Set<Path> paths) throws IOException {
		StateCache cache=null;
		for (Path path : paths) {
			StateCache parsed=StateCacheIO.parse(path);
			if (null==cache) {
				cache=parsed;
			} else {
				cache.patch(parsed);
			}
		}
		return cache;
	}
}
