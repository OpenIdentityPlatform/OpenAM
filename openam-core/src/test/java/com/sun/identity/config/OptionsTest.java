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
 * Coverage for the migrated {@code Options} page, the first migrated page that has no
 * {@code onSecurityCheck()} override at all - see the comment on the class: the old {@code
 * Options} extended {@code TemplatedPage} (not {@code ProtectedPage}), so it must stay reachable
 * even once OpenAM is configured (that's how a completed install reaches the upgrade-options
 * branch), and {@code SetupPage}'s default {@code onSecurityCheck()} (always {@code true})
 * already matches that without an override.
 *
 * <p>{@code onInit()} itself is not exercised here at all, unlike every other migrated page's
 * {@code onInit()}: its very first computed value, {@code EmbeddedOpenDS.isOpenDSVer1Installed()}
 * -&gt; {@code getOpenDSVersion()} -&gt; {@code AMSetupServlet.getBaseDir()}, unconditionally
 * throws {@code ConfiguratorException("Servlet Context is null")} unless {@code AMSetupServlet}'s
 * private static {@code servletCtx} has already been set - which only happens via that servlet's
 * real container-driven {@code init(ServletConfig)} lifecycle, never present in a bare {@code
 * openam-core} test JVM. This is pre-existing behavior copied 1:1 from the old Click {@code
 * Options.doInit()} (which called the same method just as unconditionally), not something this
 * port introduced, and in the real deployed webapp {@code AMSetupServlet} is already initialized
 * long before {@code /config/options.htm} can be requested. Faking it here would mean either
 * reflectively poking {@code AMSetupServlet}'s private static field with a mock {@code
 * ServletContext} deep enough to satisfy several chained lookups ({@code getAppResource},
 * {@code getRealPath}, ...), or calling the real (heavyweight, side-effectful) {@code init(...)} -
 * both disproportionate for this page, same category of call as every other
 * environment-coupled gap already documented in the implementation notes. Only the one method
 * that doesn't touch it - {@code isNewInstall()} - is covered below.
 *
 * <p>The already-configured/upgrade-in-progress branches of {@code isNewInstall()} aren't covered
 * either, same disproportionate-effort call already made for every other step's {@code
 * onSecurityCheck} since increment 1.
 */
public class OptionsTest {

    private Options options;

    @BeforeMethod
    public void setup() throws Exception {
        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        HttpServletResponse response = mock(HttpServletResponse.class);

        options = new Options();
        options.setContext(new ConfiguratorContext(request, response));
    }

    @Test
    public void isNewInstallIsTrueOnAFreshUnconfiguredInstance() {
        assertThat(AMSetupServlet.isConfigured()).isFalse();

        assertThat(options.isNewInstall()).isTrue();
    }
}
