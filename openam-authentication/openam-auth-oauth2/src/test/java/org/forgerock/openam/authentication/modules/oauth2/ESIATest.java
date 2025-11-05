/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018-2025 3A Systems LLC.
 */

package org.forgerock.openam.authentication.modules.oauth2;

import org.openidentityplatform.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.forgerock.openam.authentication.modules.oauth2.service.ESIAServiceUrlProvider;
import org.forgerock.openam.authentication.modules.oauth2.service.esia.Signer;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@PrepareForTest(HttpRequestContent.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class ESIATest extends PowerMockTestCase {

	@Test
	public void syncOffsetTest() throws Exception {
		HttpRequestContent httpRequestContent =  mock(HttpRequestContent.class);
		Map<String, List<String>> headers = Collections.singletonMap("Date", Collections.singletonList("Mon, 11 Mar 2019 07:07:25 GMT"));
		when(httpRequestContent.getHeadersUsingHEAD(Matchers.anyString())).thenReturn(headers);
		PowerMockito.mockStatic(HttpRequestContent.class);
		PowerMockito.when(HttpRequestContent.getInstance()).thenReturn(httpRequestContent);
		ESIAServiceUrlProvider.getSyncOffset();
	}

	final static String algorithm = "ECGOST3410-2012";
	final static String paramsSpec = "Tc26-Gost-3410-12-256-paramSetA";
	final static String signatureAlgorithm = "GOST3411WITHGOST3410-2012-256";
	final static String alias = "openam-test";
	
	@Test
	public void testSigner() throws Exception {
		Security.addProvider(new org.openidentityplatform.bouncycastle.jce.provider.BouncyCastleProvider());

		KeyPairGenerator keygen = KeyPairGenerator.getInstance(algorithm, "BC");
		keygen.initialize(new ECGenParameterSpec(paramsSpec));

		KeyPair keyPair = keygen.generateKeyPair();

		final String keyPath = generateTempKeyFile(keyPair);
		final String certPath = generateTempCertificateFile(keyPair);

		String signed = new Signer(keyPath, certPath).signString("test");
		Assert.assertNotNull(signed);
		System.out.println(signed);
	}

	private String generateTempCertificateFile(KeyPair keyPair)throws Exception {
		org.openidentityplatform.bouncycastle.asn1.x500.X500Name subject = new org.openidentityplatform.bouncycastle.asn1.x500.X500Name("CN=" + alias);
		BigInteger serial = BigInteger.ONE;
		Date notBefore = new Date();
		Date notAfter = new Date(notBefore.getTime() + TimeUnit.DAYS.toMillis(365 * 10));

		org.openidentityplatform.bouncycastle.cert.X509v3CertificateBuilder certificateBuilder = new org.openidentityplatform.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
				subject, serial,
				notBefore, notAfter,
				subject, keyPair.getPublic()
		);
		org.openidentityplatform.bouncycastle.cert.X509CertificateHolder certificateHolder = certificateBuilder.build(
				new org.openidentityplatform.bouncycastle.operator.jcajce.JcaContentSignerBuilder(signatureAlgorithm)
						.build(keyPair.getPrivate())
		);
		org.openidentityplatform.bouncycastle.cert.jcajce.JcaX509CertificateConverter certificateConverter
				= new org.openidentityplatform.bouncycastle.cert.jcajce.JcaX509CertificateConverter();

		X509Certificate certificate = certificateConverter.getCertificate(certificateHolder);

		File file = File.createTempFile("cert-", ".pem");

		try(FileWriter sw = new FileWriter(file); JcaPEMWriter writer = new JcaPEMWriter(sw)) {
			writer.writeObject(certificate);
			writer.flush();
		}
		return file.getAbsolutePath();
	}

	private String generateTempKeyFile(KeyPair keyPair) throws Exception {
		PrivateKey privateKey = keyPair.getPrivate();
		File file = File.createTempFile("key-", ".pem");
		try(FileWriter sw = new FileWriter(file); JcaPEMWriter writer = new JcaPEMWriter(sw)) {
			writer.writeObject(privateKey);
			writer.flush();
		}
		return file.getAbsolutePath();
	}

}
