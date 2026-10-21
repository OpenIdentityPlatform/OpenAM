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
package org.forgerock.openam.shared.security.whitelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests that {@link FailSafeRedirectUrlValidator} reads the real <code>com.iplanet.am.server.*</code> properties.
 *
 * <p>{@link FailSafeRedirectUrlValidatorTest} drives the origin matrix through a subclass that overrides the three
 * getters, which leaves the reads themselves uncovered. They are exercised here, on the properties provider this
 * module carries for its tests, and in a class of their own because the properties belong to the whole JVM.
 */
public class FailSafeRedirectUrlValidatorServerPropertiesTest {

    /**
     * Reports no whitelist, so that every URL falls through to the origin of this server.
     */
    private static final class NoWhitelist implements ValidDomainExtractor<String> {

        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            return null;
        }
    }

    private final FailSafeRedirectUrlValidator<String> validator =
            new FailSafeRedirectUrlValidator<String>(new NoWhitelist());

    /**
     * The properties belong to the whole JVM and the provider this module carries for its tests offers no way of
     * removing one, so they are blanked on both sides of every method: the class neither inherits what ran before
     * it nor leaves anything behind. Blank reads back the same way as unset everywhere they are consulted -
     * <code>StringUtils.isEmpty</code> for the scheme and the host, <code>getAsInt</code> for the port.
     */
    @BeforeMethod
    public void setUp() {
        configureServer("", "", "");
    }

    @AfterMethod
    public void tearDown() {
        configureServer("", "", "");
    }

    /**
     * Puts this server on the given origin. A non blank origin is deployed under <code>/openam</code>: without a
     * request the deployment descriptor is what bounds the path an absolute URL of this server may take.
     */
    private static void configureServer(final String protocol, final String host, final String port) {
        SystemPropertiesManager.initializeProperties(Constants.AM_SERVER_PROTOCOL, protocol);
        SystemPropertiesManager.initializeProperties(Constants.AM_SERVER_HOST, host);
        SystemPropertiesManager.initializeProperties(Constants.AM_SERVER_PORT, port);
        SystemPropertiesManager.initializeProperties(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR,
                host.isEmpty() ? "" : "/openam");
    }

    @Test
    public void shouldReadTheConfiguredOriginFromTheServerProperties() {
        configureServer("https", "sso.example.com", "443");
        assertThat(validator.getConfiguredScheme()).isEqualTo("https");
        assertThat(validator.getConfiguredHost()).isEqualTo("sso.example.com");
        assertThat(validator.getConfiguredPort()).isEqualTo(443);
    }

    @Test
    public void shouldAcceptTheConfiguredOriginWithNoRequestToFallBackOn() {
        // With no request there is no container origin to widen the answer, so this can only pass by reading the
        // properties.
        configureServer("https", "sso.example.com", "443");
        assertThat(validator.isRedirectUrlValid(null, "https://sso.example.com/openam/console", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(null, "https://attacker.example.com/", "realm")).isFalse();
    }

    @Test
    public void shouldAcceptTheConfiguredOriginBehindATlsTerminatingProxy() {
        configureServer("https", "sso.example.com", "443");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("10.0.0.5");
        when(request.getServerPort()).thenReturn(8080);
        assertThat(validator.isRedirectUrlValid(request, "https://sso.example.com/openam/", "realm")).isTrue();
    }

    @Test
    public void shouldTreatAnUnparseablePortAsUnconfigured() {
        configureServer("https", "sso.example.com", "not a port");
        assertThat(validator.getConfiguredPort()).isEqualTo(-1);
        assertThat(validator.isRedirectUrlValid(null, "https://sso.example.com/openam/", "realm")).isFalse();
    }

    @Test
    public void shouldRejectEveryAbsoluteUrlWhileThePropertiesAreUnset() {
        configureServer("", "", "");
        assertThat(validator.isRedirectUrlValid(null, "https://sso.example.com/openam/", "realm")).isFalse();
        assertThat(validator.isRedirectUrlValid(null, "/openam/XUI/", "realm")).isTrue();
    }
}
