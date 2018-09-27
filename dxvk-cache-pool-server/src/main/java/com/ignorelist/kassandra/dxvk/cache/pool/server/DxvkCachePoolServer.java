/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.CacheStorageFS;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

/**
 *
 * @author poison
 */
public class DxvkCachePoolServer implements Closeable {

	private static final Logger LOG=Logger.getLogger(DxvkCachePoolServer.class.getName());

	private final Configuration configuration;
	private final CacheStorage cacheStorage;
	private Server server;

	public DxvkCachePoolServer(final Configuration configuration, final CacheStorage cacheStorage) {
		this.configuration=configuration;
		this.cacheStorage=cacheStorage;
	}

	public synchronized void start() throws Exception {
		if (null!=server) {
			throw new IllegalStateException("server already started");
		}
		URI baseUri=UriBuilder.fromUri("http://localhost/").port(configuration.getPort()).build();
		ResourceConfig resourceConfig=buildResourceConfig();

		server=JettyHttpContainerFactory.createServer(baseUri, resourceConfig);
		server.setRequestLog(new RequestLog() {
			@Override
			public void log(Request request, Response response) {
				LOG.info(request.toString());
			}
		});
		server.start();
	}

	private ResourceConfig buildResourceConfig() {
		ResourceConfig resourceConfig=new ResourceConfig();
		resourceConfig.register(DxvkCachePoolREST.class);
		resourceConfig.register(new ServerBinder(configuration, cacheStorage));
		EncodingFilter.enableFor(resourceConfig, GZipEncoder.class);
		return resourceConfig;
	}

	public void join() throws InterruptedException {
		if (null==server) {
			throw new IllegalStateException("server not started");
		}
		server.join();
	}

	@Override
	public synchronized void close() throws IOException {
		if (null==server) {
			LOG.warning("no server");
			return;
		}
		try {
			server.stop();
			server.destroy();
			server=null;
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, null, ex);
			throw new IOException(ex);
		}
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws Exception {
		Configuration configuration=null;
		try {
			configuration=parseConfiguration(args);
		} catch (ParseException|EOFException e) {
			System.exit(1);
		}
		final Path storagePath=Paths.get(System.getProperty("user.home"), ".local", "share", "dxvk-cache-pool-server", "storage");

		try (CacheStorageFS storage=new CacheStorageFS(storagePath); DxvkCachePoolServer js=new DxvkCachePoolServer(configuration, storage)) {
			storage.init();
			js.start();
			js.join();
		}
	}

	public static Configuration parseConfiguration(String[] args) throws IOException, ParseException {
		Options options=buildOptions();
		CommandLineParser parser=new DefaultParser();
		CommandLine commandLine;
		try {
			commandLine=parser.parse(options, args);
		} catch (ParseException pe) {
			System.err.println(pe.getMessage());
			System.err.println();
			printHelp(options);
			throw pe;
		}
		if (commandLine.hasOption('h')) {
			printHelp(options);
			throw new EOFException();
		}

		return new Configuration();
	}

	private static void printHelp(Options options) throws IOException {
		HelpFormatter formatter=new HelpFormatter();
		formatter.printHelp("dvxk-cache-pool-server", options, true);
	}

	private static Options buildOptions() {
		Options options=new Options();
		options.addOption("h", "help", false, "show this help");
		options.addOption(Option.builder().longOpt("port").numberOfArgs(1).argName("port").desc("Server port").build());
		options.addOption(Option.builder().longOpt("storage").numberOfArgs(1).argName("path").desc("Storage path").build());
		return options;
	}

}
