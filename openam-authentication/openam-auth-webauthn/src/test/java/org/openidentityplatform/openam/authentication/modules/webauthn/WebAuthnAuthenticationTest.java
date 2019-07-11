package org.openidentityplatform.openam.authentication.modules.webauthn;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.servlet.http.HttpServletRequest;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.webauthn4j.authenticator.Authenticator;

public class WebAuthnAuthenticationTest {

	WebAuthnAuthentication webAuthnAuthentication;
	
	@BeforeMethod
	public void initMocks() throws AuthLoginException {
		
		HttpServletRequest httpServletRequest =  mock(HttpServletRequest.class);
		when(httpServletRequest.getScheme()).thenReturn("http");
		when(httpServletRequest.getServerName()).thenReturn("localhost");
		when(httpServletRequest.getServerPort()).thenReturn(8080);
		
		webAuthnAuthentication = mock(WebAuthnAuthentication.class, Mockito.CALLS_REAL_METHODS);
		when(webAuthnAuthentication.getHttpServletRequest()).thenReturn(httpServletRequest);
		when(webAuthnAuthentication.getSessionId()).thenReturn("87DCE7CF5F9DB00AC98367CA8640884F");
		doNothing().when(webAuthnAuthentication).replaceCallback(anyInt(), anyInt(), any(Callback.class));
		doReturn(Collections.<Authenticator>emptySet()).when(webAuthnAuthentication).loadAuthenticators();
	}
	
	@Test
	public void testProcessRequestUsername() throws Exception {
		webAuthnAuthentication.init(null, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		assertEquals(1, webAuthnAuthentication.process(null, 1));
	}
	
	@Test
	public void testProcessRequestCredentialsUserNameCallback() throws Exception {
		webAuthnAuthentication.init(null, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		NameCallback userNameCallback = new NameCallback("Username", "user1");
		userNameCallback.setName("user1");
		Callback[] callbacks = new Callback[] {userNameCallback};
		assertEquals(webAuthnAuthentication.process(callbacks, 1), 2);
	}
	
	@Test
	public void testProcessRequestCredentials() throws Exception {
		webAuthnAuthentication.init(null, Collections.singletonMap(webAuthnAuthentication.getUserKey(), "user1"), Collections.EMPTY_MAP);
		assertEquals(webAuthnAuthentication.process(null, 1), 2);
	}
	
}
