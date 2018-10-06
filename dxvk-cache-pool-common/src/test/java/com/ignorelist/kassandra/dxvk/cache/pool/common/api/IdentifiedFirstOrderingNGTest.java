/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.api;

import com.beust.jcommander.internal.Lists;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.CryptoUtil;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.Identity;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKey;
import com.ignorelist.kassandra.dxvk.cache.pool.common.crypto.PublicKeyInfo;
import java.security.KeyPair;
import java.util.List;
import java.util.Objects;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
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
public class IdentifiedFirstOrderingNGTest {

	private static KeyPair keyPair0;
	private static KeyPair keyPair1;
	private static PublicKey publicKey0;
	private static PublicKey publicKey1;

	public IdentifiedFirstOrderingNGTest() throws Exception {
		keyPair0=CryptoUtil.generate();
		keyPair1=CryptoUtil.generate();
		publicKey0=new PublicKey(keyPair0.getPublic());
		publicKey1=new PublicKey(keyPair1.getPublic());
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
	 * Test of compare method, of class IdentifiedFirstOrdering.
	 */
	@Test
	public void testCompare() {
		PublicKeyInfo publicKeyInfo0=new PublicKeyInfo(publicKey0);

		PublicKeyInfo publicKeyInfo1=new PublicKeyInfo(publicKey1);

		IdentityStorage identityStorageMock=Mockito.mock(IdentityStorage.class);
		Mockito.when(identityStorageMock.getIdentity(Mockito.any(PublicKeyInfo.class)))
				.thenAnswer((InvocationOnMock iom) -> {
					PublicKeyInfo argument=iom.<PublicKeyInfo>getArgument(0);
					if (Objects.equals(argument, publicKeyInfo1)) {
						Identity i=new Identity();
						i.setPublicKeyInfo(publicKeyInfo1);
						i.setEmail("a");
						i.setName("b");
						return i;
					}
					return null;
				});

		IdentifiedFirstOrdering ordering=new IdentifiedFirstOrdering(identityStorageMock);

		Assert.assertEquals(ordering.compare(publicKeyInfo0, publicKeyInfo0), 0);
		Assert.assertEquals(ordering.compare(publicKeyInfo1, publicKeyInfo1), 0);

		final int compare01=ordering.compare(publicKeyInfo0, publicKeyInfo1);
		Assert.assertTrue(compare01>0, Integer.toString(compare01));

		final int compare10=ordering.compare(publicKeyInfo1, publicKeyInfo0);
		Assert.assertTrue(compare10<0, Integer.toString(compare10));

		List<PublicKeyInfo> publicKeyInfosList=Lists.newArrayList(publicKeyInfo0, publicKeyInfo1);
		publicKeyInfosList.sort(ordering);
		Assert.assertEquals(publicKeyInfosList.get(0), publicKeyInfo1);
	}

}
