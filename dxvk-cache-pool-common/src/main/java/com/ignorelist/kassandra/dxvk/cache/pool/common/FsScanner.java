/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 *
 * @author poison
 */
public class FsScanner {

	private final ImmutableSet<ExecutableInfo> executables;
	private final ImmutableSet<Path> cachePaths;

	private FsScanner(ImmutableSet<ExecutableInfo> executables, ImmutableSet<Path> caches) {
		this.executables=executables;
		this.cachePaths=caches;
	}

	public ImmutableSet<ExecutableInfo> getExecutables() {
		return executables;
	}

	public ImmutableSet<Path> getCachePaths() {
		return cachePaths;
	}

	public static Path buildCachePath(ExecutableInfo executableInfo) {
		final String fileNameString=executableInfo.getPath().getFileName().toString();
		String cacheFileName=Util.removeFileExtension(fileNameString)+Util.DXVK_CACHE_EXT;
		return executableInfo.getPath().resolveSibling(cacheFileName);
	}

	public static FsScanner scan(Set<Path> baseDirectories) {
		// walking the FS is slow, only do it once.
		final ImmutableSet<Path> paths=baseDirectories.parallelStream()
				.map(FsScanner::scan)
				.flatMap(Collection::stream)
				.collect(ImmutableSet.toImmutableSet());
		ImmutableSet<Path> cachePaths=paths.stream()
				.filter(Util.PREDICATE_CACHE)
				.collect(ImmutableSet.toImmutableSet());
		ImmutableSet<ExecutableInfo> exec=paths.stream()
				.filter(Util.PREDICATE_EXE)
				.map(ExecutableInfo::build)
				.collect(ImmutableSet.toImmutableSet());
		return new FsScanner(exec, cachePaths);
	}

	private static ImmutableSet<Path> scan(Path baseDirectory) {
		try {
			final ImmutableSet<Path> paths=Files.walk(baseDirectory)
					.map(Path::toAbsolutePath)
					.filter(p -> null!=p.getParent())
					.filter(p -> !p.getParent().endsWith("system32"))
					.filter(p -> !p.getParent().endsWith("syswow64"))
					.filter(p -> !p.getParent().endsWith("fakedlls"))
					.filter(p -> !p.getParent().endsWith("windows"))
					.filter(Predicates.or(Util.PREDICATE_EXE, Util.PREDICATE_CACHE))
					.filter(Files::isRegularFile)
					.collect(ImmutableSet.toImmutableSet());
			return paths;
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
