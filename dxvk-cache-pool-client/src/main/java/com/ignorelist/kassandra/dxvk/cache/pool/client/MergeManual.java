/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client;

import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author poison
 */
public class MergeManual {

	public static void main(String[] args) throws IOException, ParseException {
		Options options=new Options();
		options.addOption(Option.builder("i").hasArgs().required().argName("input").desc("Input files").build());
		options.addOption(Option.builder("o").numberOfArgs(1).required().argName("output").desc("Output file").build());
		CommandLineParser parser=new DefaultParser();
		CommandLine commandLine=parser.parse(options, args);

		StateCache stateCache=null;
		for (String inputFileName : commandLine.getOptionValues("i")) {
			StateCache part=StateCacheIO.parse(Paths.get(inputFileName));
			if (null==stateCache) {
				stateCache=part;
			} else {
				stateCache.patch(part);
			}
		}

		StateCacheIO.write(Paths.get(commandLine.getOptionValue("o")), stateCache);
	}

}
