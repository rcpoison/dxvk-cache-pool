/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.base.Predicates;
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
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.KeyStore;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfoSignees;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheMeta;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

	private FsScanner scanResult;
	private ImmutableSet<String> availableBaseNames;
	private ImmutableMap<String, StateCacheInfoSignees> cacheDescriptorsByBaseName;
	//private ImmutableMap<String, StateCacheInfo> cacheDescriptorsByBaseNameUnsigned;
	private KeyStore keyStore;

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
			if (commandLine.hasOption("verbose")) {
				c.setVerbose(true);
			}

			ImmutableSet<Path> paths=commandLine.getArgList().stream()
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

	public synchronized FsScanner getScanResult() throws IOException {
		if (null==scanResult) {
			System.err.println("scanning directories");
			scanResult=FsScanner.scan(configuration.getCacheTargetPath(), configuration.getGamePaths());
			System.err.println("scanned "+scanResult.getVisitedFiles()+" files");
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
		if (null==cacheDescriptorsByBaseName) {
			try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
				System.err.println("looking up remote caches for "+getAvailableBaseNames().size()+" possible games");
				Set<StateCacheInfoSignees> cacheDescriptors=restClient.getCacheDescriptorsSignees(StateCacheHeaderInfo.getLatestVersion(), getAvailableBaseNames());
				cacheDescriptorsByBaseName=Maps.uniqueIndex(cacheDescriptors, StateCacheMeta::getBaseName);
				//cacheDescriptorsByBaseNameUnsigned=ImmutableMap.copyOf(Maps.transformValues(cacheDescriptorsByBaseName, StateCacheInfoSignees::toUnsigned));
				System.err.println("found "+cacheDescriptorsByBaseName.size()+" matching caches");
				if (configuration.isVerbose()) {
					cacheDescriptorsByBaseName.values().forEach(d -> {
						System.err.println(" -> "+d.getBaseName()+" ("+d.getEntries().size()+" entries)");
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
		System.err.println("preparing wine prefixes");
		for (Path wineDriveC : getScanResult().getWineRoots()) {
			final Path symLink=wineDriveC.resolve(Configuration.WINE_PREFIX_SYMLINK);
			if (!Files.isSymbolicLink(symLink)) {
				if (Files.isDirectory(symLink, LinkOption.NOFOLLOW_LINKS)||Files.isRegularFile(symLink, LinkOption.NOFOLLOW_LINKS)) {
					System.err.println(" -> warning: "+symLink+" exists and is a directory/file instead of a symlink. dxvk-cache-pool will not work for this wine prefix.");
				} else {
					System.err.println("-> creating symlink from "+symLink+" to "+configuration.getCacheTargetPath());
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
			System.err.println("writing "+entriesWithoutLocalCache.size()+" new caches");
			if (!entriesWithoutLocalCache.isEmpty()) {
				try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
					for (StateCacheInfoSignees cacheInfo : entriesWithoutLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path targetPath=Util.cacheFileForBaseName(configuration.getCacheTargetPath(), baseName);
						System.err.println(" -> "+baseName+": writing to "+targetPath);
						final StateCache cache=restClient.getCache(StateCacheHeaderInfo.getLatestVersion(), baseName);
						StateCacheIO.write(targetPath, cache);
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
			System.err.println("updating "+entriesLocalCache.size()+" caches");
			if (!entriesLocalCache.isEmpty()) {
				try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
					for (StateCacheInfoSignees cacheInfo : entriesLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path cacheFile=baseNameToCacheTarget.get(baseName);
						final StateCache localCache=StateCacheIO.parse(cacheFile);

						final int localCacheEntriesSize=localCache.getEntries().size();
						final StateCacheInfo localCacheInfo=localCache.toInfo();
						final Set<StateCacheEntry> missingEntries=restClient.getMissingEntries(localCacheInfo);
						if (missingEntries.isEmpty()) {
							System.err.println(" -> "+baseName+": is to date ("+localCacheEntriesSize+" entries)");
						} else {
							System.err.println(" -> "+baseName+": patching ("+localCacheEntriesSize+" existing entries, adding "+missingEntries.size()+" entries)");
							localCache.patch(missingEntries);
							StateCacheIO.writeAtomic(cacheFile, localCache);
						}
						//final StateCacheInfo cacheInfoUnsigned=cacheDescriptorsByBaseNameUnsigned.get(baseName);
						final StateCacheInfo cacheInfoUnsigned=cacheInfo.toUnsigned();
						final StateCache missingOnServer=localCache.diff(cacheInfoUnsigned);
						if (!missingOnServer.getEntries().isEmpty()) {
							System.err.println(" -> "+baseName+": sending "+missingOnServer.getEntries().size()+" missing entries to remote");
							restClient.store(missingOnServer);
						}
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
		System.err.println("found "+pathsToUpload.keySet().size()+" candidates for upload");
		try (CachePoolRestClient restClient=new CachePoolRestClient(configuration.getHost())) {
			for (Map.Entry<String, Collection<Path>> entry : pathsToUpload.asMap().entrySet()) {
				final String baseName=entry.getKey();
				System.err.println(" -> uploading "+baseName);
				final StateCache cache=readMerged(ImmutableSet.copyOf(entry.getValue()));
				restClient.store(cache);

				final Path targetPath=Util.cacheFileForBaseName(configuration.getCacheTargetPath(), baseName);
				if (!Files.exists(targetPath)) {
					System.err.println(" -> "+baseName+" does not yet exist in target directory, copying to "+targetPath);
					StateCacheIO.writeAtomic(targetPath, cache);
				}
			}
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
		options.addOption(Option.builder().longOpt("init-keys").desc("Ensure keys exist and exit").build());
		return options;
	}

}
