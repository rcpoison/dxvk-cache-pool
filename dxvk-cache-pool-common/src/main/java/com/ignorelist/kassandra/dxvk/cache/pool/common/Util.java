/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 *
 * @author poison
 */
public final class Util {

	public static final class FileExtPredicate implements Predicate<Path> {

		private final String ext;

		public FileExtPredicate(String ext) {
			this.ext=ext;
		}

		@Override
		public boolean apply(Path input) {
			return input.getFileName().toString().endsWith(ext);
		}

	}

	public static final String DXVK_CACHE_EXT=".dxvk-cache";
	public static final Pattern SHA_256_HEX_PATTERN=Pattern.compile("[0-9A-F]{64}", Pattern.CASE_INSENSITIVE);
	private static final Pattern SAFE_BASE_NAME=Pattern.compile("^[\\w. -]+$", Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CHARACTER_CLASS);

	private Util() {
	}

	public static HashCode hash(HashFunction hashFunction, Path path) throws IOException {
		try (InputStream fileStream=Files.newInputStream(path)) {
			return hash(hashFunction, fileStream);
		}
	}

	public static HashCode hash(HashFunction hashFunction, InputStream inputStream) throws IOException {
		HashingInputStream hashingInputStream=new HashingInputStream(hashFunction, inputStream);
		ByteStreams.exhaust(hashingInputStream);
		return hashingInputStream.hash();
	}

	public static Path removeFileSuffix(Path path) {
		String fileName=removeFileSuffix(path.getFileName().toString());
		return path.resolveSibling(fileName);
	}

	public static String removeFileSuffix(String fileName) {
		final int lastIndexOf=fileName.lastIndexOf('.');
		if (-1==lastIndexOf) {
			return fileName;
		}
		return fileName.substring(0, lastIndexOf);
	}

	/**
	 * remove directory and suffix
	 *
	 * @param path
	 * @return name without directory and suffix
	 */
	public static String baseName(Path path) {
		return removeFileSuffix(path.getFileName().toString());
	}

	public static boolean isSafeBaseName(String baseName) {
		return SAFE_BASE_NAME.matcher(baseName).matches();
	}

	public static String cacheFileNameForBaseName(String baseName) {
		return baseName+DXVK_CACHE_EXT;
	}

	public static Path cacheFileForBaseName(Path targetPath, String baseName) {
		return targetPath.resolve(cacheFileNameForBaseName(baseName));
	}

	public static Path parseUnixPath(String path) {
		final String parsedPath=path.replaceFirst("^~/", System.getProperty("user.home")+"/");
		final Path resolvedPath=Paths.get(parsedPath);
		return resolvedPath;
	}

	public static Path getEnvPath(final String envVar) {
		final String envPath=System.getenv(envVar);
		if (!Strings.isNullOrEmpty(envPath)) {
			try {
				return Util.parseUnixPath(envPath);
			} catch (Exception e) {
				System.err.println("failed to resolve '"+envVar+"' '"+envPath+"': "+e.getMessage());
			}
		}
		return null;
	}

	/**
	 * extract subpath that ends with the specified parent
	 *
	 * @param path compelte path
	 * @param parentEnding path fragment with which the path is supposed to end
	 * @return subpath that ends with the specified parent, null if parentEnding is not contained
	 */
	public static Path extractParentPath(final Path path, final Path parentEnding) {
		Path p=path;
		while (null!=p) {
			if (p.endsWith(parentEnding)) {
				return p;
			}
			p=p.getParent();
		}
		return null;
	}

	public static int compare(final byte[] a, final byte[] b) {
		if (a.length!=b.length) {
			throw new IllegalArgumentException("arrays must be of the same length");
		}
		for (int i=0; i<a.length; ++i) {
			final int result=Byte.compare(a[i], b[i]);
			if (0!=result) {
				return result;
			}
		}
		return 0;
	}

}
