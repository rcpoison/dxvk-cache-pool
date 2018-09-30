/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.ignorelist.kassandra.dxvk.cache.pool.client.rest.DxvkCachePoolRestClient;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.FsScanner;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
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
public class DxvkCachePoolClient {

	private final Configuration configuration;

	private DxvkCachePoolClient(Configuration c) {
		this.configuration=c;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		Options options=buildOptions();
		CommandLineParser parser=new DefaultParser();
		CommandLine commandLine;
		Configuration c=new Configuration();
		try {
			commandLine=parser.parse(options, args);
			if (commandLine.hasOption('h')) {
				printHelp(options);
				System.exit(0);
			}

			final Path envDxvkCachePath=getEnvDxvkCachePath();

			if (commandLine.hasOption("t")) {
				final Path targetPath=Paths.get(commandLine.getOptionValue("t"));
				if (!Files.isDirectory(targetPath)) {
					System.err.println("target path does not exist");
				}
				c.setCacheTargetPath(targetPath);
			} else if (null!=envDxvkCachePath) {
				c.setCacheTargetPath(envDxvkCachePath);
			} else {
				System.err.println("target path is required");
				System.err.println();
				printHelp(options);
				System.exit(1);
			}
			if (commandLine.hasOption("host")) {
				c.setHost(commandLine.getOptionValue("host"));
			}
			if (commandLine.hasOption("verbose")) {
				c.setVerbose(true);
			}

			ImmutableSet<Path> paths=commandLine.getArgList().stream()
					.map(Paths::get)
					.filter(p -> {
						if (Files.isDirectory(p)) {
							return true;
						}
						System.err.println("directory does not exist: "+p);
						return false;
					})
					.collect(ImmutableSet.toImmutableSet());
			c.setGamePaths(paths);
		} catch (ParseException pe) {
			System.err.println(pe.getMessage());
			System.err.println();
			printHelp(options);
			System.exit(1);
		}
		DxvkCachePoolClient client=new DxvkCachePoolClient(c);
		client.merge();
	}

	private static Path getEnvDxvkCachePath() throws IOException {
		String envDxvkCache=System.getenv("DXVK_STATE_CACHE_PATH");
		if (!Strings.isNullOrEmpty(envDxvkCache)) {
			String envDxvkCacheResolved=envDxvkCache.replaceFirst("^~/", System.getProperty("user.home")+"/");
			try {
				return Paths.get(envDxvkCacheResolved);
			} catch (Exception e) {
				System.err.println("failed to resolve DXVK_STATE_CACHE_PATH '"+envDxvkCache+"':"+e.getMessage());
			}
		}
		System.err.println("warning: DXVK_STATE_CACHE_PATH is not set or could not be resolved. You should set it as a global environment variable for the sync to have any effect.");
		return null;
	}

	private FsScanner scan() {
		System.err.println("scanning directories");
		FsScanner fs=FsScanner.scan(configuration.getCacheTargetPath(), configuration.getGamePaths());
		System.err.println("scanned "+fs.getVisitedFiles()+" files");
		return fs;
	}

	private ImmutableMap<String, DxvkStateCacheInfo> fetchCacheDescriptors(Set<String> baseNames) throws IOException {
		try (DxvkCachePoolRestClient restClient=new DxvkCachePoolRestClient(configuration.getHost())) {
			System.err.println("looking up remove caches for "+baseNames.size()+" possible games");
			Set<DxvkStateCacheInfo> cacheDescriptors=restClient.getCacheDescriptors(StateCacheHeaderInfo.getLatestVersion(), baseNames);
			ImmutableMap<String, DxvkStateCacheInfo> cacheDescriptorsByBaseName=Maps.uniqueIndex(cacheDescriptors, DxvkStateCacheInfo::getBaseName);
			return cacheDescriptorsByBaseName;
		}
	}

	private void merge() throws IOException {
		final FsScanner fs=scan();
		final ImmutableSet<String> baseNames=ImmutableList.of(fs.getExecutables(), fs.getStateCaches())
				.stream()
				.flatMap(Collection::stream)
				.map(Util::baseName)
				.collect(ImmutableSet.toImmutableSet());

		ImmutableMap<String, DxvkStateCacheInfo> cacheDescriptorsByBaseName=fetchCacheDescriptors(baseNames);
		System.err.println("found "+cacheDescriptorsByBaseName.size()+" matching caches");
		if (configuration.isVerbose()) {
			cacheDescriptorsByBaseName.values().forEach(d -> {
				System.err.println(" -> "+d.getBaseName()+" ("+d.getEntries().size()+" entries)");
			});
		}

		if (!cacheDescriptorsByBaseName.isEmpty()) {
			// create new caches
			final ImmutableMap<String, Path> baseNameToCacheTarget=fs.getBaseNameToCacheTarget();
			Map<String, DxvkStateCacheInfo> entriesWithoutLocalCache=Maps.filterKeys(cacheDescriptorsByBaseName, Predicates.not(baseNameToCacheTarget::containsKey));
			System.err.println("writing "+entriesWithoutLocalCache.size()+" new caches");
			if (!entriesWithoutLocalCache.isEmpty()) {
				try (DxvkCachePoolRestClient restClient=new DxvkCachePoolRestClient(configuration.getHost())) {
					for (DxvkStateCacheInfo cacheInfo : entriesWithoutLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path targetPath=Util.cacheFileForBaseName(configuration.getCacheTargetPath(), baseName);
						System.err.println(" -> "+baseName+": writing to "+targetPath);
						final DxvkStateCache cache=restClient.getCache(StateCacheHeaderInfo.getLatestVersion(), baseName);
						StateCacheIO.write(targetPath, cache);
					}
				}
			}

			// merge existing caches
			Map<String, DxvkStateCacheInfo> entriesLocalCache=Maps.filterKeys(cacheDescriptorsByBaseName, baseNameToCacheTarget::containsKey);
			System.err.println("updating "+entriesLocalCache.size()+" caches");
			if (!entriesLocalCache.isEmpty()) {
				try (DxvkCachePoolRestClient restClient=new DxvkCachePoolRestClient(configuration.getHost())) {
					for (DxvkStateCacheInfo cacheInfo : entriesLocalCache.values()) {
						final String baseName=cacheInfo.getBaseName();
						final Path cacheFile=baseNameToCacheTarget.get(baseName);
						final DxvkStateCache localCache=StateCacheIO.parse(cacheFile);

						final int localCacheEntriesSize=localCache.getEntries().size();
						final DxvkStateCacheInfo localCacheInfo=localCache.toInfo();
						final Set<DxvkStateCacheEntry> missingEntries=restClient.getMissingEntries(localCacheInfo);
						if (missingEntries.isEmpty()) {
							System.err.println(" -> "+baseName+": is to date ("+localCacheEntriesSize+" entries)");
						} else {
							System.err.println(" -> "+baseName+": patching ("+localCacheEntriesSize+" existing entries, adding "+missingEntries.size()+" entries)");
							localCache.patch(missingEntries);
							final Path tmpFile=cacheFile.resolveSibling(baseName+".tmp");
							StateCacheIO.write(tmpFile, localCache);
							Files.move(tmpFile, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
						}

						final DxvkStateCache missingOnServer=localCache.diff(cacheInfo);
						if (!missingOnServer.getEntries().isEmpty()) {
							System.err.println(" -> "+baseName+": sending "+missingOnServer.getEntries().size()+" missing entries to remote");
							restClient.store(missingOnServer);
						}
					}
				}
			}

		}

		uploadUnknown(fs, cacheDescriptorsByBaseName);
	}

	private void uploadUnknown(final FsScanner fs, final ImmutableMap<String, DxvkStateCacheInfo> cacheDescriptorsByBaseName) throws IOException {
		// upload unkown caches
		ImmutableListMultimap<String, Path> cachePathsByBaseName=Multimaps.index(fs.getStateCaches(), Util::baseName);
		ListMultimap<String, Path> pathsToUpload=Multimaps.filterKeys(cachePathsByBaseName, Predicates.not(cacheDescriptorsByBaseName::containsKey));
		System.err.println("found "+pathsToUpload.keySet().size()+" candidates for upload");
		try (DxvkCachePoolRestClient restClient=new DxvkCachePoolRestClient(configuration.getHost())) {
			for (Map.Entry<String, Collection<Path>> entry : pathsToUpload.asMap().entrySet()) {
				System.err.println(" -> uploading "+entry.getKey());
				DxvkStateCache cache=readMerged(ImmutableSet.copyOf(entry.getValue()));
				restClient.store(cache);
			}
		}
	}

	private static DxvkStateCache readMerged(Set<Path> paths) throws IOException {
		DxvkStateCache cache=null;
		for (Path path : paths) {
			DxvkStateCache parsed=StateCacheIO.parse(path);
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
		options.addOption(Option.builder("t").longOpt("target").numberOfArgs(1).argName("path").desc("Target path to store caches").build());
		options.addOption(Option.builder().longOpt("host").numberOfArgs(1).argName("url").desc("Server URL").build());
		options.addOption(Option.builder().longOpt("verbose").desc("Verbose output").build());
		return options;
	}

}
