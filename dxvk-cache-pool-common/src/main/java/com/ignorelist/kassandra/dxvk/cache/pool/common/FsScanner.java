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
import com.google.common.collect.Sets;
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
	private final ImmutableSet<Path> wineRoots;
	private final int visitedFiles;
	private ImmutableMap<String, Path> baseNameToCacheTarget;

	private FsScanner(Path targetPath, ImmutableSet<Path> executables, ImmutableSet<Path> caches, ImmutableSet<Path> wineRoots, int visitedFiles) {
		this.targetPath=targetPath;
		this.executables=executables;
		this.cachePaths=caches;
		this.wineRoots=wineRoots;
		this.visitedFiles=visitedFiles;
	}

	public ImmutableSet<Path> getExecutables() {
		return executables;
	}

	public ImmutableSet<Path> getStateCaches() {
		return cachePaths;
	}

	public ImmutableSet<Path> getWineRoots() {
		return wineRoots;
	}

	public ImmutableSet<Path> getStateCachesInTarget() {
		return cachePaths.stream()
				.filter(p -> p.getParent().equals(targetPath))
				.collect(ImmutableSet.toImmutableSet());
	}

	public Set<Path> getStateCachesNotInTarget() {
		return Sets.difference(cachePaths, getStateCachesInTarget());
	}

	/**
	 * get .dxvk-cache files residing in the target directory, indexed by baseNam
	 *
	 * @return .dxvk-cache files residing in the target directory, indexed by baseNam
	 */
	public synchronized ImmutableMap<String, Path> getBaseNameToCacheTarget() {
		if (null==baseNameToCacheTarget) {
			baseNameToCacheTarget=Maps.uniqueIndex(getStateCachesInTarget(), Util::baseName);
		}
		return baseNameToCacheTarget;
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
		ImmutableSet<Path> wineRoots=paths.stream()
				.filter(Util.PREDICATE_DRIVEC_WINDOWS)
				.map(Path::getParent)
				.collect(ImmutableSet.toImmutableSet());
		return new FsScanner(targetPath, exec, cachePaths, wineRoots, visited.get());
	}

	private static ImmutableSet<Path> scan(final Path baseDirectory, final AtomicInteger visited) {
		try {
			// doesn't properly handle symlinks, need to refactor.
			final ImmutableSet<Path> paths=Files.walk(baseDirectory)
					.map(Path::toAbsolutePath)
					.peek(p -> visited.incrementAndGet())
					.filter(p -> null!=p.getParent())
					.filter(p -> !p.getParent().endsWith("system32"))
					.filter(p -> !p.getParent().endsWith("syswow64"))
					.filter(p -> !p.getParent().endsWith("fakedlls"))
					.filter(p -> !p.getParent().endsWith("windows"))
					.filter(Predicates.or(Util.PREDICATE_EXE, Util.PREDICATE_CACHE, Util.PREDICATE_DRIVEC_WINDOWS))
					.filter(Predicates.or(Util.PREDICATE_DRIVEC_WINDOWS, Files::isRegularFile))
					.collect(ImmutableSet.toImmutableSet());
			return paths;
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
