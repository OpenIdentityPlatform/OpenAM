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
* Copyright 2013 ForgeRock AS.
*/

package org.forgerock.openam.authz.filter;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

/**
 * @author robert.wapshott@forgerock.com
 * @author Phill Cunnington
 */
public class AdminAuthorizationFilterTest {

    private AdminAuthorizationFilter filter;

    private AuthnRequestUtils requestUtils;
    private SessionService sessionService;
    private SSOTokenFactory ssoTokenFactory;

    private DebugLogger debugLogger;

    @BeforeMethod
    public void setup() {
        requestUtils = mock(AuthnRequestUtils.class);
        sessionService = mock(SessionService.class);
        ssoTokenFactory = mock(SSOTokenFactory.class);
        filter = new AdminAuthorizationFilter(ssoTokenFactory, requestUtils, sessionService);

        debugLogger = mock(DebugLogger.class);
        filter.initialise(null, null, debugLogger);
    }

    @Test
    public void shouldReturnFalseWhenTokenIdNotInRequest() {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(requestUtils.getTokenId(request)).willReturn(null);

        // When
        boolean authorized = filter.authorize(request, null);

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
        boolean authorized = filter.authorize(request, null);

        // Then
        assertFalse(authorized);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldThrowSSOExceptionWhenUserIdNotFound() throws SSOException {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken token = mock(SSOToken.class);

        given(requestUtils.getTokenId(request)).willReturn("TOKEN_ID");
        given(ssoTokenFactory.getTokenFromId("TOKEN_ID")).willReturn(token);
        given(token.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willThrow(SSOException.class);

        // When
        filter.authorize(request, null);

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
        filter.authorize(request, null);

        // Then
        verify(sessionService).isSuperUser("USER_ID");
    }
}
