package org.forgerock.oauth2.core;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.testng.annotations.Test;

public class RedirectUriValidatorTest {
	private ClientRegistration clientRegistration;
	
	@Test
	public void test() throws InvalidRequestException, RedirectUriMismatchException {
		clientRegistration = mock(ClientRegistration.class);
		given(clientRegistration.getRedirectUris()).willReturn( 
				 new HashSet<>(Arrays.asList(new URI[]{
						 URI.create("http://one"),
						 URI.create("http://two"), 
						 URI.create("http://three"),
						 URI.create("https://192.168.0.1/login")
				}))
		);
		new RedirectUriValidator().validate(clientRegistration, "https://192.168.0.1/login?idp_id=1");
	}
}
