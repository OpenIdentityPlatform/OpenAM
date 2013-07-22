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
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertTrue;

/**
 * @author robert.wapshott@forgerock.com
 */
public class AdminAuthZFilterTest {

    private AdminAuthorizationFilter filter;
    private AuthnRequestUtils mockUtils;
    private SessionService mockService;
    private SSOTokenFactory mockFactory;

    /**
     * Setup the filter before each test as all tests will use these values.
     */
    @BeforeMethod
    public void setup() {
        mockUtils = mock(AuthnRequestUtils.class);
        mockService = mock(SessionService.class);
        mockFactory = mock(SSOTokenFactory.class);
        filter = new AdminAuthorizationFilter(mockFactory, mockUtils, mockService);
    }

    @Test
    public void shouldUseRequestUtilsForTokenId() throws IOException, ServletException {
        // Given
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockUtils.getTokenId(any(HttpServletRequest.class))).willReturn(null);

        // When
        filter.authorize(null, mockResponse);

        // Then
        verify(mockUtils).getTokenId(any(HttpServletRequest.class));
    }

    @Test
    public void shouldFailIfTokenIdIsMissing() throws IOException, ServletException {
        // Given
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockUtils.getTokenId(any(HttpServletRequest.class))).willReturn(null);

        // When
        boolean result = filter.authorize(null, mockResponse);

        // Then
        assertTrue(!result);
    }

    @Test
    public void shouldUseSSOTokenFactoryToGenerateToken() throws IOException, ServletException {
        // Given
        String key = "badger";
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockUtils.getTokenId(any(HttpServletRequest.class))).willReturn(key);

        // When
        filter.authorize(null, mockResponse);

        // Then
        verify(mockFactory).getTokenFromId(eq(key));
    }

    @Test
    public void shouldFailIfNoTokenCreated() throws IOException, ServletException {
        // Given
        String key = "badger";
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockUtils.getTokenId(any(HttpServletRequest.class))).willReturn(key);

        // When
        boolean result = filter.authorize(null, mockResponse);

        // Then
        assertTrue(!result);
    }

    @Test
    public void shouldUseSessionServiceToDetermineSuperUser() throws IOException, ServletException, SSOException {
        // Given
        String key = "badger";

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockUtils.getTokenId(any(HttpServletRequest.class))).willReturn(key);

        SSOToken mockToken = mock(SSOToken.class);
        given(mockFactory.getTokenFromId(anyString())).willReturn(mockToken);

        given(mockToken.getProperty(anyString())).willReturn(key);

        // When
        filter.authorize(null, mockResponse);

        // Then
        verify(mockService).isSuperUser(eq(key));
    }

    @Test
    public void shouldVerifyUserIsAnAdminUser() throws SSOException, IOException, ServletException {
        // Given
        String key = "badger";

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockUtils.getTokenId(any(HttpServletRequest.class))).willReturn(key);

        SSOToken mockToken = mock(SSOToken.class);
        given(mockToken.getProperty(anyString())).willReturn(key);
        given(mockFactory.getTokenFromId(anyString())).willReturn(mockToken);

        given(mockService.isSuperUser(anyString())).willReturn(Boolean.TRUE);

        // When
        boolean result = filter.authorize(null, mockResponse);

        // Then
        assertTrue(result);
    }

    @Test
    public void shouldSet403ForNonAdminUser() throws SSOException, IOException, ServletException {
        // Given
        String key = "badger";

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockUtils.getTokenId(any(HttpServletRequest.class))).willReturn(key);

        SSOToken mockToken = mock(SSOToken.class);
        given(mockToken.getProperty(anyString())).willReturn(key);
        given(mockFactory.getTokenFromId(anyString())).willReturn(mockToken);

        given(mockService.isSuperUser(anyString())).willReturn(Boolean.FALSE);

        // When
        boolean result = filter.authorize(null, mockResponse);

        // Then
        assertTrue(!result);
    }
}
