/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.client.rest.DxvkCachePoolRestClient;
import com.ignorelist.kassandra.dxvk.cache.pool.common.FsScanner;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
			if (commandLine.hasOption("t")) {
				c.setCacheTargetPath(Paths.get(commandLine.getOptionValue("t")));
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
			if (paths.isEmpty()) {
				System.err.println("no valid directories passed");
				System.exit(1);
			}
			c.setGamePaths(paths);
		} catch (ParseException pe) {
			System.err.println(pe.getMessage());
			System.err.println();
			printHelp(options);
			System.exit(1);
		}

		merge(c);
	}

	private static void merge(Configuration c) throws IOException {
		System.err.println("scanning directories");
		final ImmutableSet<Path> pathsToScan=ImmutableSet.<Path>builder()
				.addAll(c.getGamePaths())
				.add(c.getCacheTargetPath())
				.build();
		FsScanner fs=FsScanner.scan(pathsToScan);
		System.err.println("scanned "+fs.getVisitedFiles()+" files");
		try (DxvkCachePoolRestClient restClient=new DxvkCachePoolRestClient(c.getHost())) {
			final ImmutableSet<Path> executables=fs.getExecutables();
			final ImmutableSet<String> baseNames=executables.stream()
					.map(Util::baseName)
					.collect(ImmutableSet.toImmutableSet());
			System.err.println("looking up state caches for "+baseNames.size()+" baseNames");
			Set<DxvkStateCacheInfo> cacheDescriptors=restClient.getCacheDescriptors(StateCacheHeaderInfo.getLatestVersion(), baseNames);
			System.err.println("found "+cacheDescriptors.size()+" matching state caches");
			if (c.isVerbose()) {
				cacheDescriptors.forEach(d -> {
					System.err.println(" -> "+d.getBaseName()+", "+d.getEntries().size()+" entries");
				});
			}
			if (cacheDescriptors.isEmpty()) {
				return;
			}

		}
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
		options.addOption(Option.builder().longOpt("verbose").desc("verbose output").build());
		return options;
	}

}
