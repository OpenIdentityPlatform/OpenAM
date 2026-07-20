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
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.jaxrpc;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import com.iplanet.am.util.SystemProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the notification-registration authorization guard used to prevent unauthenticated
 * stored SSRF via the JAXRPC {@code registerNotificationURL} endpoints (GHSA-w858-46wv-v45w).
 */
public class JAXRPCRequestFilterTest {

    @BeforeMethod
    @AfterMethod
    public void resetSkipProperty() {
        SystemProperties.initializeProperties(JAXRPCRequestFilter.SKIP_AUTH_CHECK, "false");
    }

    @Test
    public void anonymousCallerIsRejectedWhenNoRequestBound() {
        // No /jaxrpc request is bound to this thread and the skip-auth toggle is off,
        // so an unauthenticated caller must not be allowed to register a notification URL.
        assertNull(JAXRPCRequestFilter.getCurrentRequest());
        assertFalse(JAXRPCRequestFilter.isServerOrAgentAuthorized(),
                "unauthenticated caller must be rejected");
    }

    @Test
    public void skipAuthPropertyBypassesCheck() {
        SystemProperties.initializeProperties(JAXRPCRequestFilter.SKIP_AUTH_CHECK, "true");
        assertTrue(JAXRPCRequestFilter.isServerOrAgentAuthorized(),
                "skip-auth-check=true must bypass the check (runtime escape hatch)");
    }

    @Test
    public void filterBindsRequestForTheChainAndClearsItAfterwards() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest[] seenDuringChain = new HttpServletRequest[1];
        FilterChain chain = (req, res) -> seenDuringChain[0] = JAXRPCRequestFilter.getCurrentRequest();

        new JAXRPCRequestFilter().doFilter(request, response, chain);

        assertSame(seenDuringChain[0], request, "request must be bound during the filter chain");
        assertNull(JAXRPCRequestFilter.getCurrentRequest(),
                "request must be cleared once the filter chain completes");
    }
}
