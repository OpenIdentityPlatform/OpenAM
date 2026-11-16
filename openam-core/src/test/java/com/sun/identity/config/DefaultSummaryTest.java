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
package com.sun.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.sun.identity.setup.AMSetupServlet;
import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Coverage for the migrated {@code DefaultSummary} (one-click default config) page, matching the
 * old Click {@code ProtectedPage}/{@code DefaultSummary} behavior this was ported from.
 *
 * <p>{@code createDefaultConfig()} - the page's only handler - is not unit-tested here: its only
 * substantive logic is aggregating request/session state into request parameters ahead of a
 * direct call to the static {@code AMSetupServlet.processRequest(...)}, which performs real
 * configuration writes (embedded OpenDJ, on-disk config) - the same disproportionate-effort call
 * already made for {@code Wizard.createConfig()} in increment 5. Unlike that method, there is no
 * dedicated Cargo/Selenium IT for it, but {@code openam-server}'s
 * {@code IT_Setup} already drives this exact path through the real UI (clicks
 * {@code DemoConfiguration}, then {@code createDefaultConfig}), so it isn't left wholly
 * unverified.
 *
 * <p>The already-configured branch of {@code onSecurityCheck} isn't covered here either, same
 * disproportionate-effort call as every other step since increment 1's {@code Step1Test}.
 */
public class DefaultSummaryTest {

    private DefaultSummary defaultSummary;

    @BeforeMethod
    public void setup() throws Exception {
        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        HttpServletResponse response = mock(HttpServletResponse.class);

        defaultSummary = new DefaultSummary();
        defaultSummary.setContext(new ConfiguratorContext(request, response));
    }

    @Test
    public void onSecurityCheckAllowsAccessOnAFreshUnconfiguredInstance() {
        assertThat(AMSetupServlet.isConfigured()).isFalse();

        assertThat(defaultSummary.onSecurityCheck()).isTrue();
        assertThat(defaultSummary.isSkipRender()).isFalse();
    }
}
