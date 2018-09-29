/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import com.google.common.io.ByteStreams;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author poison
 */
public class DxvkStateCacheIONGTest {

	public DxvkStateCacheIONGTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@BeforeMethod
	public void setUpMethod() throws Exception {
	}

	@AfterMethod
	public void tearDownMethod() throws Exception {
	}

	/**
	 * Test of parse method, of class DxvkStateCacheIO.
	 */
	@Test
	public void testParse_Path() throws Exception {
		// TODO
	}

	/**
	 * Test of read and write method, of class DxvkStateCacheIO.
	 */
	@Test
	public void testReadWriteRoundtrip() throws Exception {
		try (InputStream stateCacheStream=new GZIPInputStream(DxvkStateCacheIONGTest.class.getResourceAsStream("/BeatSaber.dxvk-cache.gz"))) {
			final byte[] stateCacheData=ByteStreams.toByteArray(stateCacheStream);
			Assert.assertEquals(stateCacheData.length, 1008684);

			DxvkStateCache stateCache=DxvkStateCacheIO.parse(new ByteArrayInputStream(stateCacheData));
			Assert.assertEquals(stateCache.getVersion(), 2);
			Assert.assertEquals(stateCache.getEntrySize(), StateCacheHeaderInfo.getEntrySize(2).intValue());
			Assert.assertEquals(stateCache.getEntries().size(), 553);

			final ByteArrayOutputStream baos=new ByteArrayOutputStream();
			DxvkStateCacheIO.write(baos, stateCache);

			Assert.assertEquals(baos.toByteArray(), stateCacheData);
		}
	}

}
