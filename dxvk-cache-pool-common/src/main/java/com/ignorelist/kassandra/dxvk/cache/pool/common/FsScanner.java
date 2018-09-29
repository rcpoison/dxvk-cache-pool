/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
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

	private final ImmutableSet<Path> executables;
	private final ImmutableSet<Path> cachePaths;
	private final int visitedFiles;

	private FsScanner(ImmutableSet<Path> executables, ImmutableSet<Path> caches, int visitedFiles) {
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

	public int getVisitedFiles() {
		return visitedFiles;
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
		ImmutableSet<Path> exec=paths.stream()
				.filter(Util.PREDICATE_EXE)
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
