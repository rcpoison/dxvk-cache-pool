/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedInteger;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.StateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author poison
 */
public class StateCacheIO {

	private static final Logger LOG=Logger.getLogger(StateCacheIO.class.getName());

	/**
	 * Parse StateCache and its entries.
	 *
	 * Will set basename base on the passed Path
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static StateCache parse(final Path path) throws IOException {
		try (BufferedInputStream is=new BufferedInputStream(Files.newInputStream(path))) {
			final StateCache cache=parse(is);
			cache.setBaseName(Util.baseName(path));
			return cache;
		}
	}

	/**
	 * Parse StateCache and its entries. Will not set basename of course.
	 *
	 * @param inputStream
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws IOException
	 */
	public static StateCache parse(final InputStream inputStream) throws UnsupportedOperationException, IOException {
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
		if (!StateCacheHeaderInfo.getKnownVersions().contains(version)) {
			LOG.log(Level.WARNING, "unknon version encountered: {0}", version);
		}

		byte[] entrySizeBytes=new byte[4];
		inputStream.read(entrySizeBytes);
		final int entrySize=parseUnsignedInt(entrySizeBytes);
		if (0==entrySize||entrySize>StateCacheHeaderInfo.ENTRY_SIZE_MAX||(StateCacheHeaderInfo.getKnownVersions().contains(version)&&StateCacheHeaderInfo.getEntrySize(version)!=entrySize)) {
			throw new IllegalStateException("header corrupt? entry size: "+entrySize);
		}

		StateCache dxvkStateCache=new StateCache();
		dxvkStateCache.setVersion(version);
		dxvkStateCache.setEntrySize(entrySize);

		List<byte[]> bareEntries=new ArrayList<>();
		while (true) {
			byte[] entry=new byte[entrySize];
			final int bytesRead=inputStream.read(entry);
			if (-1==bytesRead) {
				break;
			}
			if (bytesRead!=entrySize) {
				throw new IllegalStateException("wrong entry size, parser broken or file currupt. entrySize:"+entrySize+", bytesRead: "+bytesRead);
			}
			bareEntries.add(entry);
		}
		ImmutableSet<DxvkStateCacheEntry> cacheEntries=bareEntries.parallelStream()
				.map(DxvkStateCacheEntry::new)
				.collect(ImmutableSet.toImmutableSet());
		dxvkStateCache.setEntries(cacheEntries);
		return dxvkStateCache;
	}

	public static void write(final Path path, StateCache cache) throws IOException {
		try (OutputStream os=Files.newOutputStream(path)) {
			write(os, cache);
		}
	}

	public static void write(final OutputStream out, StateCache cache) throws IOException {
		final int version=cache.getVersion();
		final int entrySize=cache.getEntrySize();
		if (StateCacheHeaderInfo.getKnownVersions().contains(version)&&StateCacheHeaderInfo.getEntrySize(version)!=entrySize) {
			throw new IllegalStateException("wrong entry size "+entrySize+" for version "+version);
		}
		out.write("DXVK".getBytes(Charsets.US_ASCII));
		out.write(toUnsignedIntBytes(version));
		out.write(toUnsignedIntBytes(entrySize));
		cache.getEntries().stream()
				.map(DxvkStateCacheEntry::getEntry)
				.forEachOrdered(e -> {
					try {
						if (entrySize!=e.length) {
							throw new IllegalStateException("wrong entry size: "+e.length);
						}
						out.write(e);
					} catch (IOException ex) {
						throw new IllegalStateException("failed to write cache entry", ex);
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
