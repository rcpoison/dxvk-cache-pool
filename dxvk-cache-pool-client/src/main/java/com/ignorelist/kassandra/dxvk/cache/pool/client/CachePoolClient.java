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
import com.ignorelist.kassandra.dxvk.cache.pool.client.rest.CachePoolRestClient;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.FsScanner;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.ProgressLog;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.KeyStore;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.PredicateStateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntrySigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheMeta;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheSigned;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author poison
 */
public class CachePoolClient {

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
	private ImmutableMap<String, StateCacheInfoSignees> cacheDescriptorsByBaseName;
	//private ImmutableMap<String, StateCacheInfo> cacheDescriptorsByBaseNameUnsigned;
	private KeyStore keyStore;
	private PredicateStateCacheEntrySigned predicateStateCacheEntrySigned;

	public CachePoolClient(Configuration c) {
		this.configuration=c;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		Options options=buildOptions();
		CommandLineParser parser=new DefaultParser();
		CommandLine commandLine=null;
		Configuration c=new Configuration();
		try {
			commandLine=parser.parse(options, args);
			if (commandLine.hasOption('h')) {
				printHelp(options);
				System.exit(0);
			}

			if (commandLine.hasOption("host")) {
				c.setHost(commandLine.getOptionValue("host"));
			}
			if (commandLine.hasOption("only-verified")) {
				c.setOnlyVerified(true);
			}
			if (commandLine.hasOption("verbose")) {
				c.setVerbose(true);
			}
			if (commandLine.hasOption("non-recursive")) {
				c.setScanRecursive(false);
			}

			final ImmutableSet<Path> paths=commandLine.getArgList().stream()
					.map(Paths::get)
					.map(p -> {
						try {
							return p.toRealPath();
						} catch (IOException ex) {
							System.err.println("directory could not be resolved: "+ex.getMessage());
							return null;
						}
					})
					.filter(Predicates.notNull())
					.collect(ImmutableSet.toImmutableSet());
			c.setGamePaths(paths);
		} catch (ParseException pe) {
			System.err.println(pe.getMessage());
			System.err.println();
			printHelp(options);
			System.exit(1);
		}
		final String expectedStateCachePath="c:/"+Configuration.WINE_PREFIX_SYMLINK;
		// check env for DXVK_STATE_CACHE_PATH
		if (!Objects.equals(System.getenv("DXVK_STATE_CACHE_PATH"), expectedStateCachePath)) {
			//System.err.println("!warning: DXVK_STATE_CACHE_PATH is set to: '"+System.getenv("DXVK_STATE_CACHE_PATH")+"', expected: '"+expectedStateCachePath+"'. Wine will not use the caches in "+c.getCacheTargetPath());
		}
		System.err.println("target directory is: "+c.getCacheTargetPath());
		CachePoolClient client=new CachePoolClient(c);
		if (commandLine.hasOption("init-keys")) {
			client.getKeyStore();
			System.exit(0);
		}
		client.merge();
	}

	private synchronized KeyStore getKeyStore() throws IOException {
		if (null==keyStore) {
			keyStore=new KeyStore(configuration.getConfigurationPath());
		}
		return keyStore;
	}

	private synchronized PredicateStateCacheEntrySigned getCacheEntryPredicate() {
		if (null==predicateStateCacheEntrySigned) {
			predicateStateCacheEntrySigned=new PredicateStateCacheEntrySigned();
			predicateStateCacheEntrySigned.setOnlyAcceptVerifiedKeys(configuration.isOnlyVerified());
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

	public synchronized ImmutableMap<String, StateCacheInfoSignees> getCacheDescriptorsByBaseNames() throws IOException {
		// TODO: we don't actually need the descriptors anymore since we're not diffing against the remote
		if (null==cacheDescriptorsByBaseName) {
			try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
				log.log(ProgressLog.Level.MAIN, "looking up remote caches for "+getAvailableBaseNames().size()+" possible games");
				Set<StateCacheInfoSignees> cacheDescriptors=restClient.getCacheDescriptorsSignees(StateCacheHeaderInfo.getLatestVersion(), getAvailableBaseNames());
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
			final Map<String, StateCacheInfoSignees> entriesWithoutLocalCache=Maps.filterKeys(getCacheDescriptorsByBaseNames(), Predicates.not(baseNameToCacheTarget::containsKey));
			log.log(ProgressLog.Level.MAIN, "writing "+entriesWithoutLocalCache.size()+" new caches");
			if (!entriesWithoutLocalCache.isEmpty()) {
				try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
					for (StateCacheInfoSignees cacheInfo : entriesWithoutLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path targetPath=Util.cacheFileForBaseName(configuration.getCacheTargetPath(), baseName);
						log.log(ProgressLog.Level.SUB, baseName, "writing to "+targetPath);
						final StateCacheSigned cacheSigned=restClient.getCacheSigned(StateCacheHeaderInfo.getLatestVersion(), baseName, getCacheEntryPredicate());
						if (!cacheSigned.verifyAllSignaturesValid()) {
							throw new IllegalStateException("signatures could not be verified!");
						}
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
			final Map<String, StateCacheInfoSignees> entriesLocalCache=Maps.filterKeys(getCacheDescriptorsByBaseNames(), baseNameToCacheTarget::containsKey);
			log.log(ProgressLog.Level.MAIN, "updating "+entriesLocalCache.size()+" caches");
			if (!entriesLocalCache.isEmpty()) {
				try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
					for (StateCacheInfoSignees cacheInfo : entriesLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path cacheFile=baseNameToCacheTarget.get(baseName);
						final StateCache localCache=StateCacheIO.parse(cacheFile);
						final StateCache localReferenceCache=readReference(localCache.getVersion(), baseName);

						//final StateCacheInfo cacheInfoUnsigned=cacheDescriptorsByBaseNameUnsigned.get(baseName);
						//final StateCacheInfo cacheInfoUnsigned=cacheInfo.toUnsigned();
						final StateCache locallyBuilt=localCache.diff(localReferenceCache);
						if (!locallyBuilt.getEntries().isEmpty()) {
							log.log(ProgressLog.Level.SUB, baseName, "sending "+locallyBuilt.getEntries().size()+" locally built entries to remote");
							final StateCacheSigned locallyBuiltSigned=locallyBuilt.sign(getKeyStore().getPrivateKey(), getKeyStore().getPublicKey());
							restClient.storeSigned(locallyBuiltSigned);
						}

						final int localCacheEntriesSize=localCache.getEntries().size();
						final StateCacheInfo localCacheInfo=localCache.toInfo();
						localCacheInfo.setPredicateStateCacheEntrySigned(getCacheEntryPredicate());
						final Set<StateCacheEntrySigned> missingEntries=restClient.getMissingEntriesSigned(localCacheInfo);
						if (missingEntries.isEmpty()) {
							log.log(ProgressLog.Level.SUB, baseName, "is up to date ("+localCacheEntriesSize+" entries)");
						} else {
							log.log(ProgressLog.Level.SUB, baseName, "patching ("+localCacheEntriesSize+" existing entries, adding "+missingEntries.size()+" entries)");
							final ImmutableSet<StateCacheEntry> verifiedMissingEntries=missingEntries.parallelStream()
									.filter(e -> e.verifyAllSignaturesValid(publicKeyCache::getUnchecked))
									.map(StateCacheEntrySigned::getCacheEntry)
									.collect(ImmutableSet.toImmutableSet());
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
		ImmutableMap<String, StateCacheInfoSignees> descriptors=getCacheDescriptorsByBaseNames();
		ImmutableListMultimap<String, Path> cachePathsByBaseName=Multimaps.index(getScanResult().getStateCaches(), Util::baseName);
		ListMultimap<String, Path> pathsToUpload=Multimaps.filterKeys(cachePathsByBaseName, Predicates.not(descriptors::containsKey));
		log.log(ProgressLog.Level.MAIN, "found "+pathsToUpload.keySet().size()+" candidates for upload");
		try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
			for (Map.Entry<String, Collection<Path>> entry : pathsToUpload.asMap().entrySet()) {
				final String baseName=entry.getKey();
				log.log(ProgressLog.Level.SUB, baseName, "uploading");
				final StateCache cache=readMerged(ImmutableSet.copyOf(entry.getValue()));
				final StateCacheSigned cacheSigned=cache.sign(getKeyStore().getPrivateKey(), getKeyStore().getPublicKey());
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

	private static void printHelp(Options options) throws IOException {
		HelpFormatter formatter=new HelpFormatter();
		formatter.printHelp("dvxk-cache-client  directory...", options, true);
	}

	private static Options buildOptions() {
		Options options=new Options();
		options.addOption("h", "help", false, "show this help");
		//options.addOption(Option.builder("t").longOpt("target").numberOfArgs(1).argName("path").desc("Target path to store caches").build());
		options.addOption(Option.builder().longOpt("host").numberOfArgs(1).argName("url").desc("Server URL").build());
		options.addOption(Option.builder().longOpt("verbose").desc("Verbose output").build());
		options.addOption(Option.builder().longOpt("only-verified").desc("Only download entries from verified uploaders").build());
		options.addOption(Option.builder().longOpt("non-recursive").desc("Do not scan direcories recursively").build());
		options.addOption(Option.builder().longOpt("init-keys").desc("Ensure keys exist and exit").build());
		return options;
	}

}
