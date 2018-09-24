/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author poison
 */
public class PathAdapter extends XmlAdapter<Path, String> {

	@Override
	public String unmarshal(Path v) throws Exception {
		return v.toString();
	}

	@Override
	public Path marshal(String v) throws Exception {
		return Paths.get(v);
	}

}
