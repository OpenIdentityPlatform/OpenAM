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
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.config.wizard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Coverage for the migrated wizard shell page, matching the old Click {@code ProtectedPage}/
 * {@code Wizard} behavior this was ported from.
 *
 * <p>{@code onInit()} unconditionally recomputes {@code hostName}/{@code defaultPort}/{@code
 * defaultAdminPort}/{@code defaultJmxPort} on every request via {@code
 * AMSetupUtils.getFirstUnusedPort}, binding real local sockets - same as the old Click page,
 * which did this in eager field initializers at page-construction time (see the implementation
 * notes for why that moved to {@code onInit()} here). {@code request.getServerName()} is stubbed
 * to {@code "localhost"} below purely to make that side effect deterministic; the assertions below
 * don't pin exact port numbers since the actual free port depends on the test host's environment.
 *
 * <p>{@code createConfig()} - the wizard's "execute" operation - is not unit-tested: its only
 * substantive logic is aggregating session state into request parameters ahead of a direct call to
 * the static {@code AMSetupServlet.processRequest(...)}, which performs real configuration writes
 * (LDAP, on-disk config) that are out of scope for an {@code openam-core} unit test - same
 * disproportionate-effort call already made for {@code AMSetupServlet.isConfigured()} in
 * increment 1. The migration plan's own end-to-end verification step (walk the wizard through a
 * browser and click "Create Configuration") is the coverage for this method; the existing Selenium
 * IT test ({@code IT_SetupWithOpenDJ}) already drives this exact flow. See the implementation
 * notes.
 *
 * <p>The already-configured branch of {@code onSecurityCheck} isn't covered here either, same
 * disproportionate-effort call as every other step since increment 1's Step1Test.
 */
public class WizardTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Map<String, Object> sessionAttributes;
    private Wizard wizard;

    @BeforeMethod
    public void setup() throws Exception {
        sessionAttributes = new HashMap<>();

        HttpSession session = mock(HttpSession.class);
        doAnswer(inv -> sessionAttributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(session).setAttribute(anyString(), any());
        when(session.getAttribute(anyString())).thenAnswer(inv -> sessionAttributes.get(inv.getArgument(0)));

        request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter("locale")).thenReturn("en");
        when(request.getServerName()).thenReturn("localhost");

        StringWriter responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        wizard = new Wizard();
        wizard.setContext(new ConfiguratorContext(request, response));
    }

    @Test
    public void onInitComputesHostNameAndDefaultPorts() {
        wizard.onInit();

        assertThat(wizard.startingTab).isEqualTo(1);
        assertThat(wizard.defaultPort).isNotNull();
        assertThat(Integer.parseInt(wizard.defaultPort)).isGreaterThan(0);
        assertThat(Integer.parseInt(wizard.defaultAdminPort)).isGreaterThan(0);
        assertThat(Integer.parseInt(wizard.defaultJmxPort)).isGreaterThan(0);
    }

    @Test
    public void onInitRecomputesDefaultPortsOnEveryCall() {
        // Matches the old Click page's per-request eager-field-init behavior (see the class
        // javadoc): every onInit() call binds fresh sockets, it never reuses a cached value.
        wizard.onInit();
        String firstPort = wizard.defaultPort;

        wizard.defaultPort = null;
        wizard.onInit();

        assertThat(wizard.defaultPort).isNotNull();
        assertThat(wizard.defaultPort).isEqualTo(firstPort);
    }
}
