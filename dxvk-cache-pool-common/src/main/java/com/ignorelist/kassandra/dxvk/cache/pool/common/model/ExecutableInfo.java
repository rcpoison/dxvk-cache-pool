/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.MoreObjects;
import com.google.common.io.BaseEncoding;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author poison
 */
@XmlRootElement
public class ExecutableInfo implements Serializable {

	@NotNull
	private Path path;
	private byte[] hash;

	public ExecutableInfo() {
	}

	public ExecutableInfo(Path path) {
		this.path=path;
	}

	public ExecutableInfo(Path path, byte[] hash) {
		this.path=path;
		this.hash=hash;
	}

	@XmlElement(required=true)
	@XmlJavaTypeAdapter(PathAdapter.class)
	public Path getPath() {
		return path;
	}

	@XmlTransient
	public Path getFileName() {
		if (null==path) {
			return null;
		}
		return path.getFileName();
	}

	@XmlTransient
	public Path getParentFileName() {
		if (null==path) {
			return null;
		}
		return path.getParent().getFileName();
	}

	@XmlTransient
	public Path getRelativePath() {
		if (null==path) {
			return null;
		}
		final Path parentFileName=getParentFileName();
		if (null==parentFileName) {
			return getFileName();
		}
		return parentFileName.resolve(getFileName());
	}

	public void setPath(Path path) {
		this.path=path;
	}

	@XmlTransient
	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash=hash;
	}

	@Override
	public int hashCode() {
		int hash=3;
		hash=67*hash+Objects.hashCode(this.path);
		hash=67*hash+Arrays.hashCode(this.hash);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this==obj) {
			return true;
		}
		if (obj==null) {
			return false;
		}
		if (getClass()!=obj.getClass()) {
			return false;
		}
		final ExecutableInfo other=(ExecutableInfo) obj;
		if (!Objects.equals(this.path, other.path)) {
			return false;
		}
		if (!Arrays.equals(this.hash, other.hash)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		MoreObjects.ToStringHelper tSH=MoreObjects.toStringHelper(this)
				.add("path", path);
		if (null==hash) {
			return tSH.toString();
		}
		return tSH
				.add("hash", BaseEncoding.base16().encode(hash))
				.toString();
	}

	public static ExecutableInfo build(Path path) {
		try {
			ExecutableInfo executableInfo=new ExecutableInfo(path);
			return executableInfo;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static ExecutableInfo copyWithRelativePath(ExecutableInfo executableInfo) {
		return new ExecutableInfo(executableInfo.getRelativePath(), executableInfo.getHash());

	}

}
