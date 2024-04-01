package org.forgerock.openam.authentication.modules.oauth2;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.forgerock.openam.authentication.modules.oauth2.service.ESIAServiceUrlProvider;
import org.forgerock.openam.authentication.modules.oauth2.service.esia.Signer;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
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
	
	@Test
	public void testSigner() {
		URL certUrl = ESIATest.class.getClassLoader().getResource("test.crt");
		URL keyUrl = ESIATest.class.getClassLoader().getResource("test.key");
		System.setProperty(Signer.class.getName().concat(".certPath"), certUrl.getPath());
		System.setProperty(Signer.class.getName().concat(".keyPath"), keyUrl.getPath());
		
		String signed = new Signer().signString("test");
		System.out.println(signed);
	}

	public void generateCertificate() throws Exception {

		final String algorithm = "ECGOST3410-2012";
		final String paramsSpec = "Tc26-Gost-3410-12-256-paramSetA";
		final String signatureAlgorithm = "GOST3411WITHGOST3410-2012-256";

		final String alias = "openam-test";
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		KeyPairGenerator keygen = KeyPairGenerator.getInstance(algorithm, "BC");
		keygen.initialize(new ECGenParameterSpec(paramsSpec));

		KeyPair keyPair = keygen.generateKeyPair();

		org.bouncycastle.asn1.x500.X500Name subject = new org.bouncycastle.asn1.x500.X500Name("CN=" + alias);
		BigInteger serial = BigInteger.ONE;
		Date notBefore = new Date();
		Date notAfter = new Date(notBefore.getTime() + TimeUnit.DAYS.toMillis(365 * 10));

		org.bouncycastle.cert.X509v3CertificateBuilder certificateBuilder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
				subject, serial,
				notBefore, notAfter,
				subject, keyPair.getPublic()
		);
		org.bouncycastle.cert.X509CertificateHolder certificateHolder = certificateBuilder.build(
				new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(signatureAlgorithm)
						.build(keyPair.getPrivate())
		);
		org.bouncycastle.cert.jcajce.JcaX509CertificateConverter certificateConverter
				= new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter();

		X509Certificate certificate = certificateConverter.getCertificate(certificateHolder);
		PrivateKey privateKey = keyPair.getPrivate();

		try(StringWriter sw = new StringWriter(); JcaPEMWriter writer = new JcaPEMWriter(sw)) {
			writer.writeObject(privateKey);
			writer.flush();
			System.out.println(sw.getBuffer().toString());
		}

		try(StringWriter sw = new StringWriter(); JcaPEMWriter writer = new JcaPEMWriter(sw)) {
			writer.writeObject(certificate);
			writer.flush();
			System.out.println(sw.getBuffer().toString());
		}
	}
}
