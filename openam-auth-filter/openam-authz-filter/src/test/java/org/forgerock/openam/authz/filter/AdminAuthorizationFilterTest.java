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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.authz.filter;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.auth.shared.AuthUtilsWrapper;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.forgerock.openam.utils.Config;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

public class AdminAuthorizationFilterTest {

    private AdminAuthorizationModule module;

    private AuthnRequestUtils requestUtils;
    private SessionService sessionService;
    private SSOTokenFactory ssoTokenFactory;
    private AuthUtilsWrapper authUtilsWrapper;

    @BeforeMethod
    public void setup() throws ExecutionException, InterruptedException {
        requestUtils = mock(AuthnRequestUtils.class);
        sessionService = mock(SessionService.class);
        ssoTokenFactory = mock(SSOTokenFactory.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);

        Config<SessionService> sessionServiceConfig = mock(Config.class);
        given(sessionServiceConfig.isReady()).willReturn(true);
        given(sessionServiceConfig.get()).willReturn(sessionService);

        module = new AdminAuthorizationModule(ssoTokenFactory, requestUtils, sessionServiceConfig, authUtilsWrapper);

        module.initialise(null);
    }

    @Test
    public void shouldReturnFalseWhenTokenIdNotInRequest() {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(requestUtils.getTokenId(request)).willReturn(null);

        // When
        boolean authorized = module.authorize(request);

        // Then
        assertFalse(authorized);
    }

    @Test
    public void shouldReturnFalseWhenTokenIdCannotBeResolved() {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(requestUtils.getTokenId(request)).willReturn("TOKEN_ID");
        given(ssoTokenFactory.getTokenFromId("TOKEN_ID")).willReturn(null);

        // When
        boolean authorized = module.authorize(request);

        // Then
        assertFalse(authorized);
    }

    @SuppressWarnings("unchecked")
    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldThrowSSOExceptionWhenUserIdNotFound() throws SSOException {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken token = mock(SSOToken.class);

        given(requestUtils.getTokenId(request)).willReturn("TOKEN_ID");
        given(ssoTokenFactory.getTokenFromId("TOKEN_ID")).willReturn(token);
        given(token.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willThrow(SSOException.class);

        // When
        module.authorize(request);

        // Then
        fail();
    }

    @Test
    public void shouldDelegateSuperUserCheckToSessionService() throws SSOException {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken token = mock(SSOToken.class);

        given(requestUtils.getTokenId(request)).willReturn("TOKEN_ID");
        given(ssoTokenFactory.getTokenFromId("TOKEN_ID")).willReturn(token);
        given(token.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("USER_ID");

        // When
        module.authorize(request);

        // Then
        verify(sessionService).isSuperUser("USER_ID");
    }
}
