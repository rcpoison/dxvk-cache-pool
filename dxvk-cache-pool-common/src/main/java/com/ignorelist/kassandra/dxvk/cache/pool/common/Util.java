/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author poison
 */
public final class Util {

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
}
