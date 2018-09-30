/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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

	private static final class WineRootPredicate implements Predicate<Path> {

		private static final Path DRIVEC_WINDOWS=Paths.get("drive_c", "windows");

		@Override
		public boolean apply(Path input) {
			return input.endsWith(DRIVEC_WINDOWS);
		}

	}

	public static final String DXVK_CACHE_EXT=".dxvk-cache";
	public static final Predicate<Path> PREDICATE_CACHE=new FileExtPredicate(DXVK_CACHE_EXT);
	public static final Predicate<Path> PREDICATE_EXE=Predicates.or(new FileExtPredicate(".exe"), new FileExtPredicate(".EXE"));
	public static final Predicate<Path> PREDICATE_DRIVEC_WINDOWS=new WineRootPredicate();
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

	public static Path removeFileExtension(Path path) {
		String fileName=removeFileExtension(path.getFileName().toString());
		return path.resolveSibling(fileName);
	}

	public static String removeFileExtension(String fileName) {
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
		return removeFileExtension(path.getFileName().toString());
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

}
