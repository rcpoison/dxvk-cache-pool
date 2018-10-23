/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.util.Iterator;
import javax.xml.bind.annotation.XmlRootElement;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.meanbean.lang.Factory;
import org.meanbean.test.BeanTester;
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
public class ModelsNGTest {

	public ModelsNGTest() {
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

	@DataProvider(parallel=true)
	private Iterator<Object[]> models() throws IOException {
		final ClassLoader classLoader=Thread.currentThread().getContextClassLoader();
		final ClassPath classPath=ClassPath.from(classLoader);
		final ImmutableSet<ClassPath.ClassInfo> allClasses=classPath.getTopLevelClassesRecursive("com.ignorelist.kassandra.dxvk.cache.pool.common");
		final ImmutableSet<Class<?>> modelClasses=allClasses.stream()
				.map(ClassPath.ClassInfo::load)
				.filter(c -> c.isAnnotationPresent(XmlRootElement.class))
				.collect(ImmutableSet.toImmutableSet());
		return Iterables.transform(modelClasses, c -> new Object[]{c}).iterator();
	}

	@Test(dataProvider="models")
	public void testEquals(Class<?> clazz) {
		EqualsVerifier.forClass(clazz)
				.suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
				.verify();
	}

	@Test(dataProvider="models")
	public void testBean(Class<?> clazz) {
		BeanTester beanTester=new BeanTester();
		beanTester.getFactoryCollection()
				.addFactory(byte[].class, new Factory<byte[]>() {
					@Override
					public byte[] create() {
						return new byte[]{6, 6, 6};
					}
				});
		beanTester.testBean(clazz);
		System.err.println(clazz.getName());
	}

}
