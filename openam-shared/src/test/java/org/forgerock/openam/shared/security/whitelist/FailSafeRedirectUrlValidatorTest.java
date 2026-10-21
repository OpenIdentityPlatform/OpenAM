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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link FailSafeRedirectUrlValidator}.
 */
public class FailSafeRedirectUrlValidatorTest {

    /**
     * Hands back whatever patterns the test configured for a given configuration name.
     */
    private static final class StubExtractor implements ValidDomainExtractor<String> {

        private final Map<String, Collection<String>> patterns = new HashMap<String, Collection<String>>();

        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            return patterns.get(configInfo);
        }
    }

    /**
     * A validator that believes this server is deployed under the origin the test names.
     */
    private static final class ConfiguredOriginValidator extends FailSafeRedirectUrlValidator<String> {

        private final Map<String, String> properties = new HashMap<String, String>();

        private ConfiguredOriginValidator(ValidDomainExtractor<String> domainExtractor) {
            super(domainExtractor);
        }

        @Override
        protected String getServerProperty(String key) {
            return properties.get(key);
        }
    }

    private HttpServletRequest request;
    private StubExtractor extractor;
    private FailSafeRedirectUrlValidator<String> validator;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/openam");
        extractor = new StubExtractor();
        validator = new FailSafeRedirectUrlValidator<String>(extractor);
    }

    @DataProvider(name = "urlsLeavingThisServer")
    public Object[][] urlsLeavingThisServer() {
        // Inside this deployment, so that nothing but the length limit refuses it.
        StringBuilder tooLong = new StringBuilder("https://openam.example.com/openam/");
        while (tooLong.length() <= 2000) {
            tooLong.append('a');
        }
        return new Object[][] {
            { null },
            { "" },
            { "https://attacker.example.com/" },
            { "http://attacker.example.com/" },
            { "//attacker.example.com/" },
            { "https://openam.example.com@attacker.example.com/" },
            { "https://openam.example.com.attacker.example.com/" },
            { "https://openam.example.com:8443/openam/" },
            { "http://openam.example.com/openam/" },
            { "javascript:alert(1)" },
            { "data:text/html,<script>alert(1)</script>" },
            { "ftp://openam.example.com/" },
            // The origin of this server, but not this server: another application sharing the host is only
            // reachable through a whitelist, or the same-origin fallback would widen every configured pattern.
            { "https://openam.example.com/otherapp/x" },
            { "https://openam.example.com/" },
            { "https://openam.example.com" },
            { "https://openam.example.com/openam-console/x" },
            // Userinfo is not part of this server's own URLs, whether or not the host behind it is.
            { "https://evil.tld@openam.example.com/openam/" },
            { "/\\attacker.example.com" },
            { "https:/\\attacker.example.com" },
            { " https://attacker.example.com/" },
            // Climbing out of the deployment leaves it, however the path is written. URI.normalize() removes
            // only the plain form: it runs before the percent decoding, and it does not know that a container
            // drops the path parameters of a segment.
            { "https://openam.example.com/openam/../otherapp/x" },
            { "https://openam.example.com/openam/%2e%2e/otherapp/x" },
            { "https://openam.example.com/openam/..;/otherapp/x" },
            { "https://openam.example.com/openam%2f..%2fotherapp/x" },
            { "https://openam.example.com/openam/./../otherapp/x" },
            { tooLong.toString() },
        };
    }

    @Test(dataProvider = "urlsLeavingThisServer")
    public void shouldRejectUrlsLeavingThisServerWhenNoWhitelistIsConfigured(String url) {
        assertThat(validator.isRedirectUrlValid(request, url, "realm")).isFalse();
    }

    @DataProvider(name = "urlsPointingBackAtThisServer")
    public Object[][] urlsPointingBackAtThisServer() {
        return new Object[][] {
            { "/openam/XUI/#logout/" },
            { "openam/UI/Login" },
            // A relative URL is accepted whatever its path, as in RedirectUrlValidator: the container resolves it
            // against the request, so it can not leave this server either way.
            { "/otherapp/x" },
            { "https://openam.example.com/openam/" },
            { "https://openam.example.com/openam" },
            { "https://openam.example.com:443/openam/" },
            { "https://OPENAM.EXAMPLE.COM/openam/" },
            { "//openam.example.com/openam/x" },
            // A path parameter that is not a dot segment stays inside the deployment.
            { "https://openam.example.com/openam/XUI/;jsessionid=abc" },
        };
    }

    @Test(dataProvider = "urlsPointingBackAtThisServer")
    public void shouldAcceptUrlsPointingBackAtThisServerWhenNoWhitelistIsConfigured(String url) {
        assertThat(validator.isRedirectUrlValid(request, url, "realm")).isTrue();
    }

    @Test
    public void shouldAcceptWhitelistedUrlOnAnotherServer() {
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        assertThat(validator.isRedirectUrlValid(request, "https://partner.example.com/slo", "realm")).isTrue();
    }

    @Test
    public void shouldRefuseANonHttpSchemeEvenWhenAWildcardWhitelistMatchesIt() {
        // URLPatternMatcher matches an ftp: URL naming a port against "*", so only the scheme check refuses it.
        extractor.patterns.put("realm", Collections.singleton("*"));
        assertThat(validator.isRedirectUrlValid(request, "ftp://partner.example.com:21/x", "realm")).isFalse();
    }

    @Test
    public void shouldRejectUrlMissingFromTheWhitelist() {
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        assertThat(validator.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
    }

    @Test
    public void shouldStillAcceptSameOriginUrlWhenWhitelistIsConfigured() {
        // A whitelist naming only the federated partners must not lock this server out of its own endpoints:
        // WS-Federation home realm discovery sends a wreply of this server's own WSFederationServlet URL.
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        assertThat(validator.isRedirectUrlValid(request, "https://openam.example.com/openam/", "realm")).isTrue();
    }

    @Test
    public void shouldLeaveAWhitelistNarrowedByPathNarrowedOnThisHost() {
        // The fallback covers this deployment, not the whole host: an administrator who narrows the list by path
        // keeps that narrowing for every other application sharing the host.
        extractor.patterns.put("realm", Collections.singletonList("https://openam.example.com/console/*"));
        assertThat(validator.isRedirectUrlValid(request, "https://openam.example.com/console/x", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://openam.example.com/otherapp/x", "realm")).isFalse();
        // Its own deployment stays reachable, which is what the fallback is for.
        assertThat(validator.isRedirectUrlValid(request, "https://openam.example.com/openam/XUI/", "realm")).isTrue();
    }

    @Test
    public void shouldAcceptEveryPathOfTheOriginWhenDeployedAsTheRootContext() {
        when(request.getContextPath()).thenReturn("");

        ConfiguredOriginValidator configured = new ConfiguredOriginValidator(extractor);
        assertThat(configured.isRedirectUrlValid(request, "https://openam.example.com/anything", "realm")).isTrue();
        assertThat(configured.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
    }

    @Test
    public void shouldNarrowToTheConfiguredDeploymentUriWhenThereIsNoRequest() {
        // The deprecated API of SAML2Utils and WSFederationUtils validates without a request.
        ConfiguredOriginValidator configured = new ConfiguredOriginValidator(extractor);
        configured.properties.put(Constants.AM_SERVER_PROTOCOL, "https");
        configured.properties.put(Constants.AM_SERVER_HOST, "openam.example.com");
        configured.properties.put(Constants.AM_SERVER_PORT, "443");
        configured.properties.put(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, "/openam");

        assertThat(configured.isRedirectUrlValid(null, "https://openam.example.com/openam/XUI/", "realm")).isTrue();
        assertThat(configured.isRedirectUrlValid(null, "https://openam.example.com/otherapp/x", "realm")).isFalse();
        assertThat(configured.isRedirectUrlValid(null, "/openam/XUI/", "realm")).isTrue();
    }

    @Test
    public void shouldNotTakeTheWholeHostForThisServerWhenTheDeploymentIsUnknown() {
        // An unset deployment descriptor is not the root deployment: with no request to ask either, nothing on
        // this host is known to belong to this server, so only the whitelist can accept an absolute URL.
        ConfiguredOriginValidator configured = new ConfiguredOriginValidator(extractor);
        configured.properties.put(Constants.AM_SERVER_PROTOCOL, "https");
        configured.properties.put(Constants.AM_SERVER_HOST, "openam.example.com");
        configured.properties.put(Constants.AM_SERVER_PORT, "443");

        assertThat(configured.isRedirectUrlValid(null, "https://openam.example.com/openam/XUI/", "realm")).isFalse();
        assertThat(configured.isRedirectUrlValid(null, "/openam/XUI/", "realm")).isTrue();

        extractor.patterns.put("realm", Collections.singletonList("https://openam.example.com/openam/*"));
        assertThat(configured.isRedirectUrlValid(null, "https://openam.example.com/openam/XUI/", "realm")).isTrue();
    }

    @Test
    public void shouldStillAcceptRelativeUrlWhenWhitelistIsConfigured() {
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        assertThat(validator.isRedirectUrlValid(request, "/openam/XUI/", "realm")).isTrue();
    }

    @Test
    public void shouldHonourTheWhitelistOfEveryConfiguration() {
        // An endpoint reachable for more than one role has to accept what either of them lists, not just the
        // first one that happens to carry a whitelist.
        extractor.patterns.put("idp", Collections.singletonList("https://idp-partner.example.com/*"));
        extractor.patterns.put("sp", Collections.singletonList("https://sp-partner.example.com/*"));
        List<String> configInfos = Arrays.asList("idp", "sp");
        assertThat(validator.isRedirectUrlValid(request, "https://idp-partner.example.com/slo", configInfos)).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://sp-partner.example.com/slo", configInfos)).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://attacker.example.com/", configInfos)).isFalse();
    }

    @Test
    public void shouldHonourTheWhitelistOfAConfigurationThatIsNotTheFirstOne() {
        extractor.patterns.put("sp", Collections.singletonList("https://partner.example.com/*"));
        List<String> configInfos = Arrays.asList("idp", "sp");
        assertThat(validator.isRedirectUrlValid(request, "https://partner.example.com/slo", configInfos)).isTrue();
    }

    @Test
    public void shouldFallBackToOriginWhenNoConfigurationHoldsAWhitelist() {
        List<String> configInfos = Arrays.asList("idp", "sp");
        assertThat(validator.isRedirectUrlValid(request, "https://partner.example.com/slo", configInfos)).isFalse();
        assertThat(validator.isRedirectUrlValid(request, "https://openam.example.com/openam/", configInfos)).isTrue();
    }

    @Test
    public void shouldFallBackToOriginWhenConfigurationIsNull() {
        assertThat(validator.isRedirectUrlValid(request, "https://attacker.example.com/", (String) null)).isFalse();
        assertThat(validator.isRedirectUrlValid(request, "/openam/XUI/", (String) null)).isTrue();
    }

    @Test
    public void shouldAcceptTheConfiguredServerOriginWhenTheContainerSeesAnotherOne() {
        // A TLS terminating proxy leaves the container with the internal scheme and port.
        when(request.getScheme()).thenReturn("http");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getServerName()).thenReturn("internal.example.com");

        ConfiguredOriginValidator configured = new ConfiguredOriginValidator(extractor);
        configured.properties.put(Constants.AM_SERVER_PROTOCOL, "https");
        configured.properties.put(Constants.AM_SERVER_HOST, "openam.example.com");
        configured.properties.put(Constants.AM_SERVER_PORT, "443");

        assertThat(configured.isRedirectUrlValid(request, "https://openam.example.com/openam/", "realm")).isTrue();
        assertThat(configured.isRedirectUrlValid(request, "http://internal.example.com:8080/openam/", "realm"))
                .isTrue();
        assertThat(configured.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
        assertThat(configured.isRedirectUrlValid(request, "https://openam.example.com:8443/openam/", "realm"))
                .isFalse();
    }

    @Test
    public void shouldIgnoreAnIncompleteConfiguredOrigin() {
        // The request has to point elsewhere, or the request-origin branch would answer before the configured
        // origin is ever consulted and this would pass with no port parsing at all.
        when(request.getScheme()).thenReturn("http");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getServerName()).thenReturn("internal.example.com");

        ConfiguredOriginValidator configured = new ConfiguredOriginValidator(extractor);
        configured.properties.put(Constants.AM_SERVER_PROTOCOL, "https");
        configured.properties.put(Constants.AM_SERVER_HOST, "openam.example.com");
        configured.properties.put(Constants.AM_SERVER_PORT, "not-a-number");

        assertThat(configured.isRedirectUrlValid(request, "https://openam.example.com/openam/", "realm")).isFalse();
        assertThat(configured.isRedirectUrlValid(request, "http://internal.example.com:8080/openam/", "realm"))
                .isTrue();
    }

    @Test
    public void shouldRecogniseThisServerBehindAHostnameUriCanNotParse() {
        // java.net.URI leaves getHost() null for a hostname holding an underscore; getAuthority() still has it.
        when(request.getServerName()).thenReturn("openam_prod.example.com");

        assertThat(validator.isRedirectUrlValid(request, "https://openam_prod.example.com/openam/", "realm"))
                .isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://openam_prod.example.com:443/openam/", "realm"))
                .isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://openam_prod.example.com:8443/openam/", "realm"))
                .isFalse();
        assertThat(validator.isRedirectUrlValid(request, "https://attacker_evil.example.com/", "realm")).isFalse();
        // Refused at the userinfo check: java.net.URI parses this one as server based.
        assertThat(validator.isRedirectUrlValid(request, "https://openam_prod.example.com@attacker.example.com/",
                "realm")).isFalse();
        // Reaches the authority fallback, and the userinfo must not be mistaken for the host.
        assertThat(validator.isRedirectUrlValid(request, "https://attacker@openam_prod.example.com/", "realm"))
                .isFalse();
    }

    @Test
    public void shouldNotTakeAHostSpeltWithTheDotlessIForThisServer() {
        // Decoded, the authority spells "victim.example.com" with the dotless i, U+0131, which equalsIgnoreCase
        // folds onto "i" while a browser resolves it to another, registrable, domain.
        when(request.getServerName()).thenReturn("victim.example.com");

        assertThat(validator.isRedirectUrlValid(request, "https://victim.example.com/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://v%C4%B1ct%C4%B1m.example.com/openam/", "realm"))
                .isFalse();
        assertThat(validator.isRedirectUrlValid(request, "https://v\u0131ct\u0131m.example.com/openam/", "realm"))
                .isFalse();
    }

    @Test
    public void shouldNotTakeAHostSpeltWithTheDottedCapitalIForThisServer() {
        // U+0130 lower cases onto "i" under equalsIgnoreCase, and IDNA maps it to "i" plus a combining dot.
        when(request.getServerName()).thenReturn("info.example.com");

        assertThat(validator.isRedirectUrlValid(request, "https://%C4%B0nfo.example.com/openam/", "realm"))
                .isFalse();
    }
}
