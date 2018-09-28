/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.client.rest.DxvkCachePoolRestClient;
import com.ignorelist.kassandra.dxvk.cache.pool.common.FsScanner;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfoEquivalenceRelativePath;
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
			c.setPaths(paths);
		} catch (ParseException pe) {
			System.err.println(pe.getMessage());
			System.err.println();
			printHelp(options);
			System.exit(1);
		}

		merge(c);
	}

	private static void merge(Configuration c) throws IOException {
		FsScanner fs=FsScanner.scan(c.getPaths());
		try (DxvkCachePoolRestClient restClient=new DxvkCachePoolRestClient(c.getHost())) {
			final ImmutableSet<ExecutableInfo> executables=fs.getExecutables();
			System.err.println("looking up state caches for "+executables.size()+" executables");
			Set<DxvkStateCacheInfo> cacheDescriptors=restClient.getCacheDescriptors(2, executables);
			System.err.println("found "+cacheDescriptors.size()+" matching state caches:");
			if (c.isVerbose()) {
				cacheDescriptors.forEach(d -> {
					System.err.println(" > "+d.getExecutableInfo().getRelativePath()+", "+d.getEntries().size()+" entries");
				});
			}
			if (cacheDescriptors.isEmpty()) {
				return;
			}
			final ExecutableInfoEquivalenceRelativePath equivalenceRelativePath=new ExecutableInfoEquivalenceRelativePath();
			//Multimaps.index(executables, ExecutableInfoEquivalenceRelativePath::wrap);

		}
	}

	private static void printHelp(Options options) throws IOException {
		HelpFormatter formatter=new HelpFormatter();
		formatter.printHelp("dvxk-cache-client  directory...", options, true);
	}

	private static Options buildOptions() {
		Options options=new Options();
		options.addOption("h", "help", false, "show this help");
		options.addOption(Option.builder().longOpt("verbose").desc("verbose output").build());
		return options;
	}

}
