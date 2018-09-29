/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.test;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author poison
 */
public class TestUtil {

	public static byte[] readStateCacheData() throws IOException {
		try (InputStream stateCacheStream=new GZIPInputStream(TestUtil.class.getResourceAsStream("/BeatSaber.dxvk-cache.gz"))) {
			return ByteStreams.toByteArray(stateCacheStream);
		}
	}

}
