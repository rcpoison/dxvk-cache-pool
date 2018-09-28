/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author poison
 */
public class DxvkStateCacheIO {

	private static final ImmutableMap<Integer, Integer> STATE_HEADER_VERSION_SIZE=ImmutableMap.<Integer, Integer>builder()
			.put(2, 1824)
			.build();

	public static Integer getEntrySize(int version) {
		return Optional.ofNullable(STATE_HEADER_VERSION_SIZE.get(version)).orElseThrow(() -> new IllegalArgumentException("unknown version: "+version));
	}

	public static DxvkStateCache parse(final Path path) throws IOException {
		try (BufferedInputStream is=new BufferedInputStream(Files.newInputStream(path))) {
			final DxvkStateCache cache=parse(is);
			cache.setExecutableInfo(new ExecutableInfo(path));
			return cache;
		}
	}

	public static DxvkStateCache parse(final InputStream inputStream) throws UnsupportedOperationException, IOException {
		/*
		struct DxvkStateCacheHeader {
		  char     magic[4]   = { 'D', 'X', 'V', 'K' };
		  uint32_t version    = 1;
		  uint32_t entrySize  = sizeof(DxvkStateCacheEntry);
		};

		struct DxvkStateCacheEntry {
		  DxvkStateCacheKey             shaders;
		  DxvkGraphicsPipelineStateInfo state;
		  DxvkRenderPassFormat          format;
		  Sha1Hash                      hash;
		};
		 */
		byte[] magicBytes=new byte[4];
		inputStream.read(magicBytes);
		final String magicString=new String(magicBytes, Charsets.US_ASCII);
		if (!"DXVK".equals(magicString)) {
			throw new UnsupportedOperationException("wrong header: "+magicString);
		}
		byte[] versionBytes=new byte[4];
		inputStream.read(versionBytes);
		int version=parseUnsignedInt(versionBytes);
		System.err.println(version);

		byte[] entrySizeBytes=new byte[4];
		inputStream.read(entrySizeBytes);
		final int entrySize=parseUnsignedInt(entrySizeBytes);
		System.err.println(entrySize);
		DxvkStateCache dxvkStateCache=new DxvkStateCache();
		dxvkStateCache.setVersion(version);
		dxvkStateCache.setEntrySize(entrySize);

		Set<DxvkStateCacheEntry> cacheEntries=new LinkedHashSet<>();

		while (true) {
			byte[] entry=new byte[entrySize];
			final int bytesRead=inputStream.read(entry);
			if (-1==bytesRead) {
				break;
			}
			if (bytesRead!=entrySize) {
				throw new IllegalStateException("wrong entry size, parser broken or file currupt. entrySize:"+entrySize+", bytesRead: "+bytesRead);
			}
			DxvkStateCacheEntry cacheEntry=new DxvkStateCacheEntry(entry);
			cacheEntries.add(cacheEntry);
		}
		dxvkStateCache.setEntries(cacheEntries);
		return dxvkStateCache;
	}

	public static void write(final Path path, DxvkStateCache cache) throws IOException {
		try (OutputStream os=Files.newOutputStream(path)) {
			write(os, cache);
		}
	}

	public static void write(final OutputStream out, DxvkStateCache cache) throws IOException {
		out.write("DXVK".getBytes(Charsets.US_ASCII));
		out.write(toUnsignedIntBytes(cache.getVersion()));
		out.write(toUnsignedIntBytes(cache.getEntrySize()));
		cache.getEntries().stream()
				.map(DxvkStateCacheEntry::getEntry)
				.forEachOrdered(e -> {
					try {
						out.write(e);
					} catch (IOException ex) {
						throw new IllegalStateException("failed to write cache entry");
					}
				});

	}

	private static int parseUnsignedInt(byte[] bytes) {
		// ugh, lets just assume there won't be a version or entrySize past Integer.MAX_VALUE
		return UnsignedInteger.fromIntBits(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt()).intValue();
	}

	private static byte[] toUnsignedIntBytes(int i) {
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(UnsignedInteger.valueOf(i).intValue()).array();
	}

}
