/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.rest;

import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.SignatureCount;
import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.runtime.OutputStreamOutput;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.ByteStreams;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheIO;
import com.ignorelist.kassandra.dxvk.cache.pool.common.StateCacheHeaderInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.Util;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorageSigned;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.server.rest.views.Index;
import com.ignorelist.kassandra.dxvk.cache.pool.server.rest.views.Stats;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 *
 * @author poison
 */
@Singleton
@Path("/")
public class CachePoolHome {

	private static final Logger LOG=Logger.getLogger(CachePoolHome.class.getName());

	private static final int PAGE_SIZE=64;
	private static final int VERSION=StateCacheHeaderInfo.getLatestVersion();
	private static final Date LAST_MODIFIED=new Date();
	private static final String MIME_CSS="text/css";
	private static final String MIME_JAVASCRIPT="application/javascript";

	private final Supplier<Set<SignatureCount>> totalSignatureCount;
	private final LoadingCache<String, Set<SignatureCount>> signatureCountCache;

	@Inject
	private CacheStorage cacheStorage;
	@Inject
	private CacheStorageSigned cacheStorageSigned;

	public CachePoolHome() {
		totalSignatureCount=Suppliers.memoizeWithExpiration(() -> cacheStorageSigned.getTotalSignatureCounts(VERSION), 2, TimeUnit.MINUTES);
		this.signatureCountCache=CacheBuilder.newBuilder()
				.expireAfterWrite(1, TimeUnit.MINUTES)
				.build(new CacheLoader<String, Set<SignatureCount>>() {
					@Override
					public Set<SignatureCount> load(String baseName) throws Exception {
						return cacheStorageSigned.getSignatureCounts(VERSION, baseName);
					}
				});
	}

	private Response buildResponse(final RockerModel rockerModel) {
		StreamingOutput output=(OutputStream out) -> {
			rockerModel.render((contentType, charsetName) -> new OutputStreamOutput(contentType, out, charsetName));
		};
		return Response
				.ok(output)
				.build();
	}

	private Response buildResponseForStatic(Request request, final String name, final String mediaType) {
		final EntityTag etag=new EntityTag(Long.toString(LAST_MODIFIED.getTime()));
		CacheControl cacheControl=new CacheControl();
		cacheControl.setMustRevalidate(false);
		cacheControl.setNoCache(false);
		cacheControl.setNoTransform(true);
		cacheControl.setMaxAge(-1);
		Response.ResponseBuilder responseBuilder=null;

		responseBuilder=request.evaluatePreconditions(LAST_MODIFIED, etag);

		if (null!=responseBuilder) {
			return responseBuilder
					.cacheControl(cacheControl)
					.build();
		}
		try (final InputStream res=CachePoolHome.class.getResourceAsStream(name)) {
			if (null==res) {
				throw new IllegalArgumentException("file not found: "+name);
			}
			final byte[] data=ByteStreams.toByteArray(res);
			final StreamingOutput output=(OutputStream out) -> {
				out.write(data);
			};
			return Response
					.ok(output)
					.cacheControl(cacheControl)
					.lastModified(LAST_MODIFIED)
					.type(mediaType)
					.tag(etag)
					.build();
		} catch (Exception e) {
			LOG.warning(e.getMessage());
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}

	}

	@GET
	@Path("{a:(|index.html)}")
	@Produces(MediaType.TEXT_HTML)
	public Response list(@QueryParam("page") int page, @QueryParam("search") String search) {
		final Set<String> cacheInfos=cacheStorage.findBaseNames(VERSION, search);
		final int lastPage=cacheInfos.size()/PAGE_SIZE;
		final int offset=PAGE_SIZE*Math.min(Math.max(page, 0), lastPage);
		ImmutableSet<StateCacheInfo> cacheInfosForPage=cacheInfos.stream()
				.sorted()
				.skip(offset)
				.limit(PAGE_SIZE)
				.map(e -> cacheStorage.getCacheDescriptor(VERSION, e))
				.collect(ImmutableSet.toImmutableSet());
		Index template=Index.template(cacheInfosForPage, lastPage, page, search);
		return buildResponse(template);
	}

	private SetMultimap<String, SignatureCount> buildSignatureCounts(final Set<String> baseNames) {
		ImmutableSetMultimap.Builder<String, SignatureCount> builder=ImmutableSetMultimap.<String, SignatureCount>builder();
		for (String baseName : baseNames) {
			builder.putAll(baseName, signatureCountCache.getUnchecked(baseName));
		}
		return builder.build();
	}

	@GET
	@Path("stats.html")
	@Produces(MediaType.TEXT_HTML)
	public Response stats(@QueryParam("page") int page, @QueryParam("search") String search) {
		final Set<SignatureCount> totalSignatureCounts=totalSignatureCount.get();

		final Set<String> cacheInfos=cacheStorage.findBaseNames(VERSION, search);
		final int lastPage=cacheInfos.size()/PAGE_SIZE;
		final int offset=PAGE_SIZE*Math.min(Math.max(page, 0), lastPage);
		ImmutableSet<String> baseNames=cacheInfos.stream()
				.sorted()
				.skip(offset)
				.limit(PAGE_SIZE)
				.collect(ImmutableSet.toImmutableSet());
		SetMultimap<String, SignatureCount> signatureCounts=buildSignatureCounts(baseNames);
		//signatureCounts.
		Stats template=Stats.template(totalSignatureCounts, signatureCounts, lastPage, page, search);
		return buildResponse(template);
	}

	@GET
	@Path("d/{fileName:(.*\\.dxvk-cache)}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response download(@PathParam("fileName") String fileName) {
		if (Strings.isNullOrEmpty(fileName)) {
			System.err.println("");
			throw new IllegalArgumentException("filename may not be empty");
		}
		final String baseName=Util.removeFileSuffix(fileName);
		final StateCache cache=cacheStorage.getCache(VERSION, baseName);
		if (null==cache) {
			throw new IllegalStateException("cache not found for: "+baseName);
		}
		final StreamingOutput streamingOutput=(OutputStream output) -> {
			StateCacheIO.write(output, cache);
		};
		return Response
				.ok(streamingOutput)
				.build();
	}

	@GET
	@Path("s/{css:([a-z]+\\.css)}")
	@Produces(MIME_CSS)
	public Response getCss(@Context Request request, @PathParam("css") String css) {
		return buildResponseForStatic(request, "css/"+css, MIME_CSS);
	}

	@GET
	@Path("s/{js:([a-z]+\\.js)}")
	@Produces(MIME_JAVASCRIPT)
	public Response getJs(@Context Request request, @PathParam("js") String js) {
		return buildResponseForStatic(request, "js/"+js, MIME_JAVASCRIPT);
	}

	@GET
	@Path("signatureStats/{baseName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<SignatureCount> signatureStats(@PathParam("baseName") String baseName) {
		if (Strings.isNullOrEmpty(baseName)) {
			throw new IllegalArgumentException("baseName may not be empty");
		}
		return signatureCountCache.getUnchecked(baseName);
	}
}
