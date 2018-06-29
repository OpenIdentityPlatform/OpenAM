package org.forgerock.openam.authentication.modules.oauth2;

import java.net.URL;

import org.forgerock.openam.authentication.modules.oauth2.service.ESIAServiceUrlProvider;
import org.forgerock.openam.authentication.modules.oauth2.service.esia.Signer;
import org.testng.annotations.Test;

public class ESIATest {

	@Test
	public void syncOffsetTest() throws Exception {
		ESIAServiceUrlProvider.getSyncOffset();
	}
	
	//@Test
	public void testSigner() {
		URL certUrl = ESIATest.class.getClassLoader().getResource("test.crt");
		URL keyUrl = ESIATest.class.getClassLoader().getResource("test.key");
		System.setProperty(Signer.class.getName().concat(".certPath"), certUrl.getPath());
		System.setProperty(Signer.class.getName().concat(".keyPath"), keyUrl.getPath());
		
		String signed = Signer.signString("test");
		System.err.println(signed);
	}
}
