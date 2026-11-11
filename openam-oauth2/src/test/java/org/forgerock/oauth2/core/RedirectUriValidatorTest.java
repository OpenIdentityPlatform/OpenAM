package org.forgerock.oauth2.core;

import static org.mockito.BDDMockito.given;
import static org.testng.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.testng.annotations.DataProvider;
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

	private static void validate(String registered, String requested)
			throws InvalidRequestException, RedirectUriMismatchException {
		ClientRegistration registration = mock(ClientRegistration.class);
		given(registration.getRedirectUris()).willReturn(new HashSet<>(Arrays.asList(
				URI.create("https://app.example.com/callback"), URI.create(registered))));
		new RedirectUriValidator().validate(registration, requested);
	}

	@DataProvider
	public Object[][] allowedLoopbackRedirects() {
		return new Object[][] {
				// RFC 8252 7.3: registered without a port, any port is allowed
				{"http://127.0.0.1/callback", "http://127.0.0.1:51004/callback"},
				{"http://127.0.0.1/callback", "http://127.0.0.1/callback"},
				{"http://[::1]/callback", "http://[::1]:61023/callback"},
				{"http://localhost/callback", "http://localhost:8765/callback"},
				{"http://LOCALHOST/callback", "http://localhost:8765/callback"},
				{"http://127.0.0.1", "http://127.0.0.1:51004/"},
				// wildcard port
				{"http://localhost:*/callback", "http://localhost:43123/callback"},
				{"http://127.0.0.1:*/callback", "http://127.0.0.1:1/callback"},
				{"http://localhost:*/callback", "http://localhost:43123/callback?state=abc"},
				// port range, inclusive
				{"http://localhost:49152-65535/callback", "http://localhost:49152/callback"},
				{"http://localhost:49152-65535/callback", "http://localhost:65535/callback"},
				{"http://127.0.0.1:8000-9000/cb", "http://127.0.0.1:8765/cb"},
				{"http://localhost:1-100/callback", "http://localhost/callback"},
				// an explicit port still matches exactly
				{"http://localhost:8765/callback", "http://localhost:8765/callback"},
		};
	}

	@Test(dataProvider = "allowedLoopbackRedirects")
	public void shouldAllowLoopbackRedirectOnAnyMatchingPort(String registered, String requested)
			throws InvalidRequestException, RedirectUriMismatchException {
		validate(registered, requested);
	}

	@DataProvider
	public Object[][] rejectedRedirects() {
		return new Object[][] {
				// an explicit loopback port is not widened
				{"http://localhost:8765/callback", "http://localhost:8766/callback"},
				// outside the range
				{"http://localhost:49152-65535/callback", "http://localhost:49151/callback"},
				{"http://127.0.0.1:8000-9000/cb", "http://127.0.0.1:9001/cb"},
				// path, host and scheme must match
				{"http://localhost:*/callback", "http://localhost:43123/other"},
				{"http://127.0.0.1/callback", "http://127.0.0.1:51004/callback/extra"},
				{"http://localhost:*/callback", "http://127.0.0.1:43123/callback"},
				{"http://127.0.0.1/callback", "http://localhost:51004/callback"},
				{"http://127.0.0.1/callback", "https://127.0.0.1:51004/callback"},
				{"http://127.0.0.1/callback", "http://127.0.0.2:51004/callback"},
				{"http://127.0.0.1/callback", "http://user@127.0.0.1:51004/callback"},
				{"https://127.0.0.1/callback", "http://127.0.0.1:5000/callback"},
				{"http://user@localhost/callback", "http://localhost:5000/callback"},
				// never for non-loopback hosts
				{"http://app.example.com/callback", "http://app.example.com:8443/callback"},
				{"http://app.example.com:*/callback", "http://app.example.com:8443/callback"},
				{"https://localhost/callback", "https://localhost:8443/callback"},
				{"http://localhost.example.com/callback", "http://localhost.example.com:8443/callback"},
				// malformed patterns never match
				{"http://localhost:80*/callback", "http://localhost:8080/callback"},
				{"http://localhost:*-9000/callback", "http://localhost:8080/callback"},
		};
	}

	@Test(dataProvider = "rejectedRedirects", expectedExceptions = RedirectUriMismatchException.class)
	public void shouldRejectNonMatchingRedirect(String registered, String requested)
			throws InvalidRequestException, RedirectUriMismatchException {
		validate(registered, requested);
	}

	@DataProvider
	public Object[][] loopbackPatternHosts() {
		return new Object[][] {
				{"http://localhost:*/callback", "localhost"},
				{"http://127.0.0.1:8000-9000/cb", "127.0.0.1"},
				{"http://app_1.example.com/callback", null},
				{"http://user@localhost:*/callback", null},
		};
	}

	@Test(dataProvider = "loopbackPatternHosts")
	public void shouldResolveLoopbackPatternHost(String registered, String host) {
		assertEquals(RedirectUriValidator.loopbackPatternHost(URI.create(registered)), host);
	}
}
