/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
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

	private final Path targetPath;
	private final ImmutableSet<Path> executables;
	private final ImmutableSet<Path> cachePaths;
	private final int visitedFiles;

	private FsScanner(Path targetPath, ImmutableSet<Path> executables, ImmutableSet<Path> caches, int visitedFiles) {
		this.targetPath=targetPath;
		this.executables=executables;
		this.cachePaths=caches;
		this.visitedFiles=visitedFiles;
	}

	public ImmutableSet<Path> getExecutables() {
		return executables;
	}

	public ImmutableSet<Path> getStateCaches() {
		return cachePaths;
	}

	public ImmutableSet<Path> getStateCachesInTarget() {
		return cachePaths.stream()
				.filter(p -> p.getParent().equals(targetPath))
				.collect(ImmutableSet.toImmutableSet());
	}

	public ImmutableMap<String, Path> getBaseNameToCacheTarget() {
		return Maps.uniqueIndex(getStateCachesInTarget(), Util::baseName);
	}

	public int getVisitedFiles() {
		return visitedFiles;
	}

	public static FsScanner scan(Path targetPath, Set<Path> baseDirectories) {
		// walking the FS is slow, only do it once.
		final AtomicInteger visited=new AtomicInteger();
		final ImmutableSet<Path> pathsToScan=ImmutableSet.<Path>builder()
				.addAll(baseDirectories)
				.add(targetPath)
				.build();
		final ImmutableSet<Path> paths=pathsToScan.parallelStream()
				.map(s -> FsScanner.scan(s, visited))
				.flatMap(Collection::stream)
				.collect(ImmutableSet.toImmutableSet());

		ImmutableSet<Path> cachePaths=paths.stream()
				.filter(Util.PREDICATE_CACHE)
				.collect(ImmutableSet.toImmutableSet());
		ImmutableSet<Path> exec=paths.stream()
				.filter(Util.PREDICATE_EXE)
				.collect(ImmutableSet.toImmutableSet());
		return new FsScanner(targetPath, exec, cachePaths, visited.get());
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
