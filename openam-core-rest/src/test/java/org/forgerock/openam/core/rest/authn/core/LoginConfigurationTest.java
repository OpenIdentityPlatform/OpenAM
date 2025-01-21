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
 * Copyright 2013-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.core;

import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LoginConfigurationTest {

    @Test
    public void shouldSetHttpRequest() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        loginConfiguration.httpRequest(request);

        //Then
        assertEquals(loginConfiguration.getHttpRequest(), request);
    }

    @Test
    public void shouldSetIndexType() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        loginConfiguration.indexType(AuthIndexType.MODULE);

        //Then
        assertEquals(loginConfiguration.getIndexType(), AuthIndexType.MODULE);
    }

    @Test
    public void shouldSetIndexValue() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration().indexType(AuthIndexType.MODULE);

        //When
        loginConfiguration.indexValue("INDEX_VALUE");

        //Then
        assertEquals(loginConfiguration.getIndexValue(), "INDEX_VALUE");
    }

    @Test
    public void shouldSetIndexValueWithCompositeIndexType() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        LoginConfiguration loginConfiguration = new LoginConfiguration()
                .httpRequest(request)
                .indexType(AuthIndexType.COMPOSITE);

        //When
        loginConfiguration.indexValue("INDEX_VALUE");

        //Then
        assertEquals(loginConfiguration.getIndexValue(), "INDEX_VALUE");
        assertNotEquals(loginConfiguration.getHttpRequest(), request);
        assertEquals(loginConfiguration.getHttpRequest().getParameterMap().size(), 1);
    }

    @Test
    public void shouldGetSessionIdWhenNotSet() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        String sessionId = loginConfiguration.getSessionId();

        //Then
        assertEquals(sessionId, "");
    }

    @Test
    public void shouldSetSessionId() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        loginConfiguration.sessionId("SESSIONID");

        //Then
        String sessionId = loginConfiguration.getSessionId();
        assertEquals(sessionId, "SESSIONID");
    }

    @Test
    public void shouldNotSetSessionIdIfNull() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        loginConfiguration.sessionId(null);

        //Then
        String sessionId = loginConfiguration.getSessionId();
        assertEquals(sessionId, "");
    }

    @Test
    public void shouldGetSessionUpgradeWhenNotSet() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        String ssoTokenId = loginConfiguration.getSSOTokenId();

        //Then
        assertEquals(ssoTokenId, "");
    }

    @Test
    public void shouldSetSessionUpgrade() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        loginConfiguration.sessionUpgrade("SSOTOKENID");

        //Then
        String ssoTokenId = loginConfiguration.getSSOTokenId();
        assertEquals(ssoTokenId, "SSOTOKENID");
    }

    @Test
    public void shouldNotSetSessionUpgradeIfNull() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        loginConfiguration.sessionUpgrade(null);

        //Then
        String ssoTokenId = loginConfiguration.getSSOTokenId();
        assertEquals(ssoTokenId, "");
    }

    @Test
    public void shouldCheckIsSessionUpgradeRequestWhenSessionUpgradeNotSet() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();

        //When
        boolean isSessionUpgradeRequest = loginConfiguration.isSessionUpgradeRequest();

        //Then
        assertFalse(isSessionUpgradeRequest);
    }

    @Test
    public void shouldCheckIsSessionUpgradeRequestWhenSessionUpgradeSet() {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        loginConfiguration.sessionUpgrade("SSOTOKENID");

        //When
        boolean isSessionUpgradeRequest = loginConfiguration.isSessionUpgradeRequest();

        //Then
        assertTrue(isSessionUpgradeRequest);
    }
}
