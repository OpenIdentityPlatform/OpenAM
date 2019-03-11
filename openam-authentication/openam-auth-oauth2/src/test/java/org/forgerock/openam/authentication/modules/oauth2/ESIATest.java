package org.forgerock.openam.authentication.modules.oauth2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.forgerock.openam.authentication.modules.oauth2.service.ESIAServiceUrlProvider;
import org.forgerock.openam.authentication.modules.oauth2.service.esia.Signer;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

@PrepareForTest(HttpRequestContent.class)
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
