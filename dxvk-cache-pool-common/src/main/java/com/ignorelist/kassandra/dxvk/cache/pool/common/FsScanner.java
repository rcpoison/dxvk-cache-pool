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
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author poison
 */
public class FsScanner {

	private final ImmutableSet<ExecutableInfo> executables;
	private final ImmutableSet<Path> cachePaths;
	private final int visitedFiles;

	private FsScanner(ImmutableSet<ExecutableInfo> executables, ImmutableSet<Path> caches, int visitedFiles) {
		this.executables=executables;
		this.cachePaths=caches;
		this.visitedFiles=visitedFiles;
	}

	public ImmutableSet<ExecutableInfo> getExecutables() {
		return executables;
	}

	public ImmutableSet<Path> getCachePaths() {
		return cachePaths;
	}

	public int getVisitedFiles() {
		return visitedFiles;
	}

	public static Path buildCachePath(ExecutableInfo executableInfo) {
		final String fileNameString=executableInfo.getPath().getFileName().toString();
		String cacheFileName=Util.removeFileExtension(fileNameString)+Util.DXVK_CACHE_EXT;
		return executableInfo.getPath().resolveSibling(cacheFileName);
	}

	public static FsScanner scan(Set<Path> baseDirectories) {
		// walking the FS is slow, only do it once.
		final AtomicInteger visited=new AtomicInteger();
		final ImmutableSet<Path> paths=baseDirectories.parallelStream()
				.map(s -> FsScanner.scan(s, visited))
				.flatMap(Collection::stream)
				.collect(ImmutableSet.toImmutableSet());

		ImmutableSet<Path> cachePaths=paths.stream()
				.filter(Util.PREDICATE_CACHE)
				.collect(ImmutableSet.toImmutableSet());
		ImmutableSet<ExecutableInfo> exec=paths.stream()
				.filter(Util.PREDICATE_EXE)
				.map(ExecutableInfo::build)
				.collect(ImmutableSet.toImmutableSet());
		return new FsScanner(exec, cachePaths, visited.get());
	}

	private static ImmutableSet<Path> scan(final Path baseDirectory, final AtomicInteger visited) {
		try {
			final ImmutableSet<Path> paths=Files.walk(baseDirectory)
					.map(Path::toAbsolutePath)
					.peek(p -> visited.incrementAndGet())
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
