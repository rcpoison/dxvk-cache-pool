/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author poison
 */
public class UtilNGTest {

	public UtilNGTest() {
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

	@DataProvider
	public Object[][] baseNames() {
		return new Object[][]{
			{"Hello cruel Word", true},
			{"test.path", true},
			{"test-path", true},
			{"ägain", true},
			{"test,path", false},
			{"test/path", false},
			{"test\\path", false},
			{"test?path", false},
			{"test%path", false}
		};
	}

	
	@Test(dataProvider="baseNames")
	public void testIsSafeBaseName(String baseName, boolean expected) {
		boolean result=Util.isSafeBaseName(baseName);
		Assert.assertEquals(result, expected, baseName);
	}

}
