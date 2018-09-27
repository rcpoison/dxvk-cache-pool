/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 *
 * @author poison
 */
public class FsScanner {

	private static final class FileExtPredicate implements Predicate<Path> {

		private final String ext;

		public FileExtPredicate(String ext) {
			this.ext=ext;
		}

		@Override
		public boolean apply(Path input) {
			return input.getFileName().toString().endsWith(ext);
		}

	}

	private static final Predicate<Path> PREDICATE_EXE=Predicates.or(new FileExtPredicate(".exe"), new FileExtPredicate(".EXE"));
	private static final String DXVK_CACHE_EXT=".dxvk-cache";
	private static final Predicate<Path> PREDICATE_CACHE=new FileExtPredicate(DXVK_CACHE_EXT);

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
		String cacheFileName=Util.removeFileExtension(fileNameString)+DXVK_CACHE_EXT;
		return executableInfo.getPath().resolveSibling(cacheFileName);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		FsScanner fsScanner=scan(ImmutableSet.of(Paths.get("/usr/local/games/SteamLibrary/Steam/")));

		fsScanner.getCachePaths().forEach(System.err::println);

	}

	public static FsScanner scan(ImmutableSet<Path> baseDirectories) {
		// walking the FS is slow, only do it once.
		final ImmutableSet<Path> paths=baseDirectories.parallelStream()
				.map(FsScanner::scan)
				.flatMap(Collection::stream)
				.collect(ImmutableSet.toImmutableSet());
		ImmutableSet<Path> cachePaths=paths.stream()
				.filter(PREDICATE_CACHE)
				.collect(ImmutableSet.toImmutableSet());
		ImmutableSet<ExecutableInfo> exec=paths.stream()
				.filter(PREDICATE_EXE)
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
					.filter(Predicates.or(PREDICATE_EXE, PREDICATE_CACHE))
					.filter(Files::isRegularFile)
					.collect(ImmutableSet.toImmutableSet());
			return paths;
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
