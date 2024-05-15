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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2024 3A-Systems LLC. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.webauthn4j.authenticator.Authenticator;

public class WebAuthnRegistrationTest {
	
	WebAuthnRegistration webAuthnRegistration;
	
	@BeforeMethod
	public void initMocks() throws AuthLoginException {
		
		HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
		when(httpServletRequest.getScheme()).thenReturn("http");
		when(httpServletRequest.getServerName()).thenReturn("localhost");
		when(httpServletRequest.getServerPort()).thenReturn(8080);
		when(httpServletRequest.getHeader("Origin")).thenReturn("http://localhost:8080");
		
		webAuthnRegistration = mock(WebAuthnRegistration.class, Mockito.CALLS_REAL_METHODS);
		when(webAuthnRegistration.getHttpServletRequest()).thenReturn(httpServletRequest);
		when(webAuthnRegistration.getSessionId()).thenReturn("87DCE7CF5F9DB00AC98367CA8640884F");
		doNothing().when(webAuthnRegistration).replaceCallback(anyInt(), anyInt(), any(Callback.class));
		doNothing().when(webAuthnRegistration).save(any(Authenticator.class));
		doNothing().when(webAuthnRegistration).initUserId();
		webAuthnRegistration.userId = "test";
		webAuthnRegistration.init(null, Collections.emptyMap(), Collections.emptyMap());
	}
	
	@Test
	public void testProcessRequestUsername() throws Exception {
		webAuthnRegistration.init(null, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		assertEquals(webAuthnRegistration.process(null, 1), 2);
	}
	
	@Test
	public void testProcessRequestCredentialsUserNameCallback() throws Exception {
		webAuthnRegistration.init(null, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		NameCallback userNameCallback = new NameCallback("Username", "user1");
		userNameCallback.setName("user1");
		Callback[] callbacks = new Callback[] {userNameCallback};
		assertEquals(2, webAuthnRegistration.process(callbacks, 1));
	}
	
	@Test
	public void testProcessRequestCredentials() throws Exception {
		webAuthnRegistration.init(null, Collections.singletonMap(webAuthnRegistration.getUserKey(), "user1"), Collections.EMPTY_MAP);
		assertEquals(2, webAuthnRegistration.process(null, 1));
	}
	
	@Test
	public void testProcessCredentials() throws Exception {

		PasswordCallback idCallback = new PasswordCallback("data", false);
		final String credentialId = "Dt46gcUIV08YHRo4tXmt85Ie8Ihiw2MDr5ARgPhKG2ByDhmH0jzQbivWALXGM0RKM0LWO9mI7rtX1KNnzhhyLVCE_3F1V-ePT2M-HNu-91bcBeZ_CHzrAVksE-wP2NCpJSp_dMlxV1-rPUampHGoMRMyU_7Xi9Rw8Scl_9jqRitCRD3XbZWOCOY8B7T0j-EIAGFrIei30YwTMxQBndBDu8WcYUA60yM0BwwR482seQSCHBfh8maYy1GEyiP8bpeYjZHuDouel5EKIvc2pa5esGVuVY1cBQaMfZh4DVMCgnJlb8PvoOfmKJhUXTStVdDXrtplK9Id-AWh2UdoMK-T";
		final String attestationObject = "o2NmbXRoZmlkby11MmZnYXR0U3RtdKJjc2lnWEcwRQIgbdJQ2Q0udhpZxTSwCM00TvxDTPhQ2lxRafOkQgvd8IkCIQDnjiJiGygHNbsCm-yEznz8RkdR94YDCqp5Fpcp-g7wRWN4NWOBWQF1MIIBcTCCARagAwIBAgIJAIqK93XCOr_GMAoGCCqGSM49BAMCMBMxETAPBgNVBAMMCFNvZnQgVTJGMB4XDTE3MTAyMDIxNTEzM1oXDTI3MTAyMDIxNTEzM1owEzERMA8GA1UEAwwIU29mdCBVMkYwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQ5gjPmSDFBJap5rwDMdyqO4lCcWqQXxXtHBN-S-zt6ytC3amquoctXGuOOKZikTkT_gX8LFXVqmMZIcvC4EziGo1MwUTAdBgNVHQ4EFgQU8bJw2i1BjqI2uvqQWqNempxTxD4wHwYDVR0jBBgwFoAU8bJw2i1BjqI2uvqQWqNempxTxD4wDwYDVR0TAQH_BAUwAwEB_zAKBggqhkjOPQQDAgNJADBGAiEApFdcnvfziaAunldkAvHDwNViRH461fZv_6tFlbYPGEwCIQCS1PM8fMOKTgdr3hpqeQq_ysQK8NJZtPbFADEk8effHWhhdXRoRGF0YVkBg0mWDeWIDoxodDQXD2R2YFuP5K65ooYyx5lc87qDHZdjQQAAAAAAAAAAAAAAAAAAAAAAAAAAAP8O3jqBxQhXTxgdGji1ea3zkh7wiGLDYwOvkBGA-EobYHIOGYfSPNBuK9YAtcYzREozQtY72Yjuu1fUo2fOGHItUIT_cXVX549PYz4c2773VtwF5n8IfOsBWSwT7A_Y0KklKn90yXFXX6s9RqakcagxEzJT_teL1HDxJyX_2OpGK0JEPddtlY4I5jwHtPSP4QgAYWsh6LfRjBMzFAGd0EO7xZxhQDrTIzQHDBHjzax5BIIcF-HyZpjLUYTKI_xul5iNke4Oi56XkQoi9zalrl6wZW5VjVwFBox9mHgNUwKCcmVvw--g5-YomFRdNK1V0Neu2mUr0h34BaHZR2gwr5OlAQIDJiABIVggFIRwFmWDe2G6Vap-47mKkZJy0fjxw7vaWjy3nUlKxjUiWCCXaby_67nTqm3pDPHI8mI2dkZzZjS9beegzhdkL6Hddg";
		final String clientDataJSON = "eyJjaGFsbGVuZ2UiOiJPRGRFUTBVM1EwWTFSamxFUWpBd1FVTTVPRE0yTjBOQk9EWTBNRGc0TkVZIiwib3JpZ2luIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwidHlwZSI6IndlYmF1dGhuLmNyZWF0ZSJ9";
		final String credentials = String.format("{\"credentialId\": \"%s\", " +
				"\"attestationObject\": \"%s\", " +
				"\"clientDataJSON\": \"%s\" }", credentialId, attestationObject, clientDataJSON);
		idCallback.setPassword(credentials.toCharArray());
		

		TextOutputCallback credentialsCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "credentials");
		
		Callback[] callbacks = new Callback[] {idCallback, credentialsCallback};
		
		assertEquals(ISAuthConstants.LOGIN_SUCCEED, webAuthnRegistration.process(callbacks, 2));
	}

}
