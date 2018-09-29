/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import java.time.Instant;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author poison
 */
public class InstantAdapter extends XmlAdapter<Instant, String> {

	@Override
	public String unmarshal(Instant v) throws Exception {
		return v.toString();
	}

	@Override
	public Instant marshal(String v) throws Exception {
		return Instant.parse(v);
	}

}
