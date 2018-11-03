/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.ignorelist.kassandra.dxvk.cache.pool.server.rest.CachePoolREST;
import com.ignorelist.kassandra.dxvk.cache.pool.server.rest.CachePoolHome;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.ignorelist.kassandra.dxvk.cache.pool.server.rest.IllegalArgumentExceptionMapper;
import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.CacheStorageFS;
import com.ignorelist.kassandra.dxvk.cache.pool.server.storage.SignatureStorageFS;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
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
public class CachePoolServer implements Closeable {

	public static class DelayedResetLogManager extends LogManager {

		private static DelayedResetLogManager instance;

		public DelayedResetLogManager() {
			instance=this;
		}

		@Override
		public void reset() {
		}

		private void actuallyReset() {
			super.reset();
		}

		public static void resetStatic() {
			instance.actuallyReset();
		}
	}

	private static final Logger LOG;

	static {
		System.setProperty("java.util.logging.manager", DelayedResetLogManager.class.getName());
		//System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %2$s %5$s%6$s%n");
		LOG=Logger.getLogger(CachePoolServer.class.getName());
	}

	private final Configuration configuration;
	private CacheStorageFS cacheStorage;
	private SignatureStorageFS signatureStorage;
	private ForkJoinPool forkJoinPool;
	private ScheduledExecutorService scheduledExecutorService;
	private Server server;

	public CachePoolServer(final Configuration configuration) {
		this.configuration=configuration;
	}

	public synchronized void start() throws Exception {
		if (null!=server) {
			throw new IllegalStateException("server already started");
		}
		final int threads=Math.max(4, Runtime.getRuntime().availableProcessors());
		forkJoinPool=new ForkJoinPool(threads);
		scheduledExecutorService=Executors.newScheduledThreadPool(threads);

		cacheStorage=new CacheStorageFS(configuration.getStorage().resolve("cache"), forkJoinPool);
		scheduledExecutorService.submit(() -> {
			try {
				cacheStorage.init();
			} catch (IOException ex) {
				Logger.getLogger(CachePoolServer.class.getName()).log(Level.SEVERE, "failed to init cache storage", ex);
				throw new IllegalStateException(ex);
			}
		});

		signatureStorage=new SignatureStorageFS(configuration.getStorage().resolve("signatures"), forkJoinPool);
		scheduledExecutorService.submit(() -> {
			try {
				signatureStorage.init();
			} catch (IOException ex) {
				Logger.getLogger(CachePoolServer.class.getName()).log(Level.SEVERE, "failed to init signature storage", ex);
				throw new IllegalStateException(ex);
			}
		});

		ResourceConfig resourceConfig=buildResourceConfig();

		URI baseUri=UriBuilder.fromUri("http://localhost/").port(configuration.getPort()).build();
		server=JettyHttpContainerFactory.createServer(baseUri, resourceConfig);
		server.setRequestLog((Request request, Response response) -> {
			LOG.info(() -> request.getRemoteHost()+" "+request.getMethod()+" "+request.getContentLength()+" "+request.getPathInfo()+" "+response.getStatus());
		});
		server.start();
	}

	private ResourceConfig buildResourceConfig() {
		ResourceConfig resourceConfig=new ResourceConfig();
		resourceConfig.register(CachePoolREST.class);
		resourceConfig.register(CachePoolHome.class);
		resourceConfig.register(new ServerBinder(configuration, cacheStorage, signatureStorage, forkJoinPool, scheduledExecutorService));
		resourceConfig.register(IllegalArgumentExceptionMapper.class);
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
			LOG.info("stopping server");
			server.stop();
			server.destroy();
			server=null;
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, null, ex);
			throw new IOException(ex);
		}
		LOG.info("closing storage");
		cacheStorage.close();
		signatureStorage.close();
		LOG.info("shutting down executors");
		MoreExecutors.shutdownAndAwaitTermination(forkJoinPool, 2, TimeUnit.MINUTES);
		MoreExecutors.shutdownAndAwaitTermination(scheduledExecutorService, 2, TimeUnit.MINUTES);
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

		try (final CachePoolServer js=new CachePoolServer(configuration)) {
			js.start();

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					LOG.info("attempting graceful shutdown");
					try {
						js.close();
					} catch (Exception ex) {
						LOG.log(Level.SEVERE, "failed to shutdown gracefully", ex);
					} finally {
						LOG.info("finished graceful shutdown");
						DelayedResetLogManager.resetStatic();
					}
				}

			});

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

		Configuration cfg=new Configuration();
		if (commandLine.hasOption("port")) {
			cfg.setPort(Integer.parseInt(commandLine.getOptionValue("port")));
		}
		if (commandLine.hasOption("storage")) {
			cfg.setStorage(Paths.get(commandLine.getOptionValue("storage")));
		}
		if (commandLine.hasOption("versions")) {
			ImmutableSet<Integer> acceptsVersions=ImmutableSet.copyOf(commandLine.getOptionValues("versions")).stream()
					.map(Integer::parseInt)
					.collect(ImmutableSet.toImmutableSet());
			cfg.setVersions(acceptsVersions);
		}
		return cfg;
	}

	private static void printHelp(Options options) throws IOException {
		HelpFormatter formatter=new HelpFormatter();
		formatter.printHelp("dvxk-cache-server", options, true);
	}

	private static Options buildOptions() {
		Options options=new Options();
		options.addOption("h", "help", false, "show this help");
		options.addOption(Option.builder().longOpt("storage").numberOfArgs(1).argName("path").desc("Storage path").build());
		options.addOption(Option.builder().longOpt("port").numberOfArgs(1).argName("port").desc("Server port").build());
		options.addOption(Option.builder().longOpt("versions").hasArgs().argName("version").desc("DXVK state cache versions to accept").build());
		return options;
	}

}
