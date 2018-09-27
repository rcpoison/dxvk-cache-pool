/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import java.io.Serializable;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author poison
 */
@XmlRootElement
public class DxvkStateCacheDescriptor implements DxvkStateCacheMeta, Serializable {

    @NotNull
    private ExecutableInfo executableInfo;
    private int version;
    @NotNull
    private int entrySize;
    private Set<DxvkStateCacheEntryInfo> entries;

    public DxvkStateCacheDescriptor() {
    }

    @Override
    public ExecutableInfo getExecutableInfo() {
        return executableInfo;
    }

    public void setExecutableInfo(ExecutableInfo executableInfo) {
        this.executableInfo = executableInfo;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public int getEntrySize() {
        return entrySize;
    }

    public void setEntrySize(int entrySize) {
        this.entrySize = entrySize;
    }

    public Set<DxvkStateCacheEntryInfo> getEntries() {
        return entries;
    }

    public void setEntries(Set<DxvkStateCacheEntryInfo> entries) {
        this.entries = entries;
    }

}
