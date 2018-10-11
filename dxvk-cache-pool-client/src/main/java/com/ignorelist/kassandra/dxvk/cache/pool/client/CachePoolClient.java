/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
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
			if (commandLine.hasOption("min-signatures")) {
				c.setMinimumSignatures(Integer.parseInt(commandLine.getOptionValue("min-signatures")));
			}
			if (commandLine.hasOption("cache-target-dir")) {
				c.setCacheTargetPath(Paths.get(commandLine.getOptionValue("cache-target-dir")));
			}
			final List<String> argList=commandLine.getArgList();
			if (1==argList.size()&&Files.isRegularFile(Paths.get(argList.get(0)))) {
				c.setGamePaths(ImmutableSet.of(Paths.get(argList.get(0)).getParent()));
				c.setScanRecursive(false);
			} else {
				final ImmutableSet<Path> paths=argList.stream()
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
			}
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
		CachePoolMerger merger=new CachePoolMerger(c);
		if (commandLine.hasOption("init-keys")) {
			merger.getKeyStore();
			System.exit(0);
		}
		try {
			merger.verifyProtocolVersion();
		} catch (UnsupportedOperationException e) {
			System.err.println("Version mismatch: "+e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		if (commandLine.hasOption("download-verified")) {
			merger.downloadVerifiedKeyData();
		}
		merger.merge();
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
		options.addOption(Option.builder().longOpt("download-verified").desc("Download verified public keys and associated verification data").build());
		options.addOption(Option.builder().longOpt("non-recursive").desc("Do not scan direcories recursively").build());
		options.addOption(Option.builder().longOpt("init-keys").desc("Ensure keys exist and exit").build());
		options.addOption(Option.builder().longOpt("min-signatures").numberOfArgs(1).argName("count").desc("Minimum required signatures to download a cache entry").build());
		options.addOption(Option.builder().longOpt("cache-target-dir").numberOfArgs(1).argName("dir").desc("Override default cache target directory").build());
		return options;
	}

}
