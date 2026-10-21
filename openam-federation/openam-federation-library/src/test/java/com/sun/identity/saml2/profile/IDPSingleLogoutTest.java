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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.saml2.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the RelayState handed to the multi-federation-protocol logout.
 *
 * <p>{@link com.sun.identity.multiprotocol.SingleLogoutManager} redirects the browser to that value with no check
 * of its own, and it can not be loaded in this module's test JVM - its static initialiser reaches for the
 * configuration service - so the guard sits here, where it can be exercised.
 */
public class IDPSingleLogoutTest {

    private HttpServletRequest request;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
    }

    @DataProvider(name = "relayStatesLeavingThisServer")
    public Object[][] relayStatesLeavingThisServer() {
        return new Object[][] {
            { "https://attacker.example.com/" },
            { "//attacker.example.com/" },
            { "https://openam.example.com@attacker.example.com/" },
            { "https://openam.example.com.attacker.example.com/" },
            { "javascript:alert(1)" },
        };
    }

    @Test(dataProvider = "relayStatesLeavingThisServer")
    public void shouldDropARelayStateLeavingThisServer(String relayState) {
        // SingleLogoutManager.sendLogoutResponse appends the logout status to this value and hands it straight to
        // sendRedirect. validateRelayStateURL in front of it accepts everything while the hosted entity has no
        // relayStateUrlList, which is the default, so what does not point back at this server is dropped here.
        assertThat(IDPSingleLogout.relayStateForMultiProtocolLogout(request, "/", "https://idp.example.com",
                relayState)).isNull();
    }

    @DataProvider(name = "relayStatesPointingBackAtThisServer")
    public Object[][] relayStatesPointingBackAtThisServer() {
        return new Object[][] {
            { "/openam/saml2/jsp/default.jsp" },
            { "/openam/XUI/#login" },
            { "https://openam.example.com/openam/" },
        };
    }

    @Test(dataProvider = "relayStatesPointingBackAtThisServer")
    public void shouldKeepARelayStatePointingBackAtThisServer(String relayState) {
        assertThat(IDPSingleLogout.relayStateForMultiProtocolLogout(request, "/", "https://idp.example.com",
                relayState)).isEqualTo(relayState);
    }

    @Test
    public void shouldPassOnTheAbsenceOfARelayState() {
        // Nothing to redirect to is not a rejection: SingleLogoutManager prints the logout status instead, and
        // turning the empty value into null must not change which of those two branches it takes.
        assertThat(IDPSingleLogout.relayStateForMultiProtocolLogout(request, "/", "https://idp.example.com", null))
                .isNull();
        assertThat(IDPSingleLogout.relayStateForMultiProtocolLogout(request, "/", "https://idp.example.com", ""))
                .isEmpty();
    }
}
