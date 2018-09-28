/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.runtime.OutputStreamOutput;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.DxvkStateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import views.index;

/**
 *
 * @author poison
 */
@Path("/")
public class DxvkCachePoolHome {

	private static final int PAGE_SIZE=1024;
	private static final int VERSION=2;

	@Inject
	private CacheStorage cacheStorage;

	private Response buildResponse(final RockerModel rockerModel) {
		StreamingOutput output=(OutputStream out) -> {
			rockerModel.render((contentType, charsetName) -> new OutputStreamOutput(contentType, out, charsetName));
		};
		return Response
				.ok(output)
				.build();
	}

	@GET
	@Path("{a:(|index.html)}")
	@Produces(MediaType.TEXT_HTML)
	public Response list(@QueryParam("page") int page, @QueryParam("search") String search) {
		final Set<ExecutableInfo> executables=cacheStorage.findExecutables(VERSION, search);
		final int lastPage=executables.size()/PAGE_SIZE;
		final int offset=PAGE_SIZE*Math.min(Math.max(page, 0), lastPage);
		ImmutableSet<DxvkStateCacheInfo> executablesForPage=executables.stream()
				.sorted()
				.skip(offset)
				.limit(PAGE_SIZE)
				.map(e -> cacheStorage.getCacheDescriptor(VERSION, e))
				.collect(ImmutableSet.toImmutableSet());
		index template=index.template(executablesForPage, lastPage, page, search);
		return buildResponse(template);
	}

	@GET
	@Path("d/{parent}/{filename:(.*\\.dxvk-cache)}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response download(@PathParam("parent") String parent, @PathParam("filename") String filename) {
		if (Strings.isNullOrEmpty(parent)) {
			throw new IllegalArgumentException("parent may not be empty");
		}
		if (Strings.isNullOrEmpty(filename)) {
			throw new IllegalArgumentException("filename may not be empty");
		}
		final Optional<ExecutableInfo> executableInfo=cacheStorage.findExecutables(VERSION, parent+"/"+Util.removeFileExtension(filename)).stream()
				.findFirst();
		if (!executableInfo.isPresent()) {
			throw new IllegalArgumentException("no executable entry found");
		}
		final DxvkStateCache cache=cacheStorage.getCache(2, executableInfo.get());
		if (null==cache) {
			throw new IllegalStateException("cache not found for: "+executableInfo);
		}
		final StreamingOutput streamingOutput=(OutputStream output) -> {
			DxvkStateCacheIO.write(output, cache);
		};
		return Response
				.ok(streamingOutput)
				.build();
	}
}
