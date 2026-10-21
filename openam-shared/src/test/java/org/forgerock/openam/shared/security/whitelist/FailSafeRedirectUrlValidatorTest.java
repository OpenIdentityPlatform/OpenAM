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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link FailSafeRedirectUrlValidator}.
 */
public class FailSafeRedirectUrlValidatorTest {

    /** The configuration of an entity that was read and holds no patterns. */
    private static final String EMPTY = "empty";

    /** The configuration of an entity whose patterns could not be read at all. */
    private static final String UNREADABLE = "unreadable";

    /**
     * Hands back whatever patterns the test configured, keyed by the configuration name, and tells the two answers
     * an extractor can give for "no patterns here" apart: an empty collection for a configuration that was read
     * and holds none, <code>null</code> for one that could not be read.
     */
    private static final class StubExtractor implements ValidDomainExtractor<String> {

        private final Map<String, Collection<String>> patterns = new HashMap<String, Collection<String>>();
        private int reads;

        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            reads++;
            if (EMPTY.equals(configInfo)) {
                return Collections.emptyList();
            }
            if (UNREADABLE.equals(configInfo)) {
                return null;
            }
            final Collection<String> configured = patterns.get(configInfo);
            return configured == null ? Collections.<String>emptyList() : configured;
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

    /**
     * Stands in for the <code>com.iplanet.am.server.*</code> properties, so that a test can put this server on one
     * origin and the container on another without moving properties that are shared by the whole JVM.
     *
     * <p>That the validator reads the real properties is covered by
     * {@link FailSafeRedirectUrlValidatorServerPropertiesTest}, which sets them for real.
     */
    private static final class TestValidator extends FailSafeRedirectUrlValidator<String> {

        private String scheme;
        private String host;
        private int port = -1;
        private String deploymentUri;
        private int maxUrlLength = RedirectUrlValidator.getMaxUrlLength();

        TestValidator(final ValidDomainExtractor<String> domainExtractor) {
            super(domainExtractor);
        }

        /**
         * Puts this server on the given origin, deployed under <code>/openam</code>: without a request the
         * deployment descriptor is what bounds the path an absolute URL of this server may take.
         */
        void configureServer(final String scheme, final String host, final int port) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.deploymentUri = "/openam";
        }

        @Override
        protected String getServerProperty(final String key) {
            return Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR.equals(key) ? deploymentUri : null;
        }

        @Override
        String getConfiguredScheme() {
            return scheme;
        }

        @Override
        String getConfiguredHost() {
            return host;
        }

        @Override
        int getConfiguredPort() {
            return port;
        }

        @Override
        int getMaxUrlLength() {
            return maxUrlLength;
        }
    }

    /**
     * The origin fallback counts the URL this server was installed under as one of its own, so these are pinned
     * rather than left to whatever else ran in this JVM first - an ambient value would silently add an accepted
     * origin and make the assertions below depend on test ordering.
     */
    private static final String[] INSTALLATION = {
        Constants.AM_SERVER_PROTOCOL,
        Constants.AM_SERVER_HOST,
        Constants.AM_SERVER_PORT,
    };

    private final Map<String, String> previousValues = new HashMap<String, String>();

    private HttpServletRequest request;
    private StubExtractor extractor;
    private TestValidator validator;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/openam");
        extractor = new StubExtractor();
        validator = new TestValidator(extractor);

        previousValues.clear();
        for (String property : INSTALLATION) {
            previousValues.put(property, SystemPropertiesManager.get(property));
        }
        SystemPropertiesManager.initializeProperties(Constants.AM_SERVER_PROTOCOL, "https");
        SystemPropertiesManager.initializeProperties(Constants.AM_SERVER_HOST, "installed.example.com");
        SystemPropertiesManager.initializeProperties(Constants.AM_SERVER_PORT, "443");
    }

    @AfterMethod
    public void tearDown() {
        for (String property : INSTALLATION) {
            String previous = previousValues.get(property);
            // Restores what get() returned before. The test SystemProperties stub has no remove, so a property that
            // was absent comes back as "" and stays present: getOrDefault then answers "" rather than its default.
            // Harmless for the installation properties, which are read through StringUtils.isEmpty.
            SystemPropertiesManager.initializeProperties(property, previous == null ? "" : previous);
        }
    }

    @Test
    public void shouldAcceptTheUrlThisServerWasInstalledUnder() {
        // The container reports openam.example.com for this request while the installation names another host;
        // both are origins of this server, which is what keeps a proxied deployment working. A plain
        // validator rather than the test one, so that the real properties are what is read.
        FailSafeRedirectUrlValidator<String> validator = new FailSafeRedirectUrlValidator<String>(extractor);
        assertThat(validator.isRedirectUrlValid(request, "https://installed.example.com/openam/", EMPTY)).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://installed.example.com:8443/openam/", EMPTY))
                .isFalse();
    }

    /**
     * Puts the container behind a TLS terminating proxy: it sees the internal connection, while every URL a working
     * deployment is configured with names the address the browser reaches this server at.
     */
    private void putBehindTlsTerminatingProxy() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("10.0.0.5");
        when(request.getServerPort()).thenReturn(8080);
        validator.configureServer("https", "sso.example.com", 443);
    }

    @DataProvider(name = "urlsLeavingThisServer")
    public Object[][] urlsLeavingThisServer() {
        // Inside this deployment, so that nothing but the length limit refuses it.
        StringBuilder tooLong = new StringBuilder("https://openam.example.com/openam/");
        while (tooLong.length() <= RedirectUrlValidator.getMaxUrlLength()) {
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
            // The parent domain of the host the container reports. The equality is bounded in both directions:
            // the row above widens the origin, this one is contained in it, and reversing the comparison to
            // originHost.endsWith(host) would hand this server's own origin to whoever registers example.com.
            // No origin is configured in this fixture, so the container is the only one that can vouch here.
            { "https://example.com/" },
            // A subdomain of that host, which the cookie domain widening host.endsWith("." + originHost) would
            // accept: no row above is a subdomain of it, so none of them bounds that widening.
            { "https://x.openam.example.com/" },
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
    public void shouldAcceptUrlOfTheLengthLimitAndRejectTheNextCharacter() {
        // A limit of its own rather than the default, so that a hard coded 2000 would fail this.
        validator.maxUrlLength = 64;
        // Inside this deployment, so that nothing but the length limit decides.
        final StringBuilder url = new StringBuilder("https://openam.example.com/openam/");
        while (url.length() < 64) {
            url.append('a');
        }
        assertThat(validator.isRedirectUrlValid(request, url.toString(), "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(request, url.append('a').toString(), "realm")).isFalse();
    }

    @DataProvider(name = "urlsOfTheConfiguredOrigin")
    public Object[][] urlsOfTheConfiguredOrigin() {
        return new Object[][] {
            { "https://sso.example.com/openam/console" },
            { "https://sso.example.com:443/openam/console" },
            { "https://SSO.EXAMPLE.COM/openam/" },
            { "//sso.example.com/openam/" },
        };
    }

    @Test(dataProvider = "urlsOfTheConfiguredOrigin")
    public void shouldAcceptTheConfiguredOriginBehindATlsTerminatingProxy(String url) {
        putBehindTlsTerminatingProxy();
        assertThat(validator.isRedirectUrlValid(request, url, "realm")).isTrue();
    }

    @Test
    public void shouldAcceptTheContainerOriginAlongsideTheConfiguredOne() {
        // On a site installation the server properties name this instance while browsers reach the site through
        // the load balancer, and the URLs OpenAM hands out - RPSigninRequest builds its wreply from
        // getRequestURL() - carry the container's view. Neither origin is right on its own, so both are accepted.
        putBehindTlsTerminatingProxy();
        assertThat(validator.isRedirectUrlValid(request, "http://10.0.0.5:8080/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://sso.example.com/openam/", "realm")).isTrue();
    }

    @DataProvider(name = "urlsOfNeitherOrigin")
    public Object[][] urlsOfNeitherOrigin() {
        return new Object[][] {
            { "https://attacker.example.com/" },
            { "https://sso.example.com.attacker.example.com/" },
            // The host is compared for equality, not as a suffix, and neither direction of a suffix match is
            // allowed. Widening it to host.endsWith(originHost) would let each of these three through - the
            // first two against the configured origin, the third against the one the container reports - while
            // the cases above still passed. The third carries the scheme and port of the container on purpose,
            // or it would die on the scheme before the host was ever reached, and it is an address rather than
            // x10.0.0.5, whose host URI.getHost() cannot read at all.
            { "https://evilsso.example.com/" },
            { "https://xsso.example.com/openam/" },
            { "http://110.0.0.5:8080/openam/" },
            // And the parent domain of the configured host, which the opposite widening -
            // originHost.endsWith(host) - would accept. The container reports an address here, so this row
            // bounds the configured origin; the parent domain row in urlsLeavingThisServer bounds the other.
            { "https://example.com/" },
            // And a subdomain of the configured host, which host.endsWith("." + originHost) would accept; the row
            // of the same shape in urlsLeavingThisServer bounds the container's origin.
            { "https://x.sso.example.com/" },
            { "http://sso.example.com/openam/" },
            { "https://sso.example.com:8443/openam/" },
            { "https://10.0.0.5:8080/openam/" },
            { "http://10.0.0.5/openam/" },
        };
    }

    @Test(dataProvider = "urlsOfNeitherOrigin")
    public void shouldRejectUrlsMatchingNeitherOriginBehindATlsTerminatingProxy(String url) {
        putBehindTlsTerminatingProxy();
        assertThat(validator.isRedirectUrlValid(request, url, "realm")).isFalse();
    }

    @Test
    public void shouldAcceptTheConfiguredOriginNamedWithATrailingRootLabel() {
        // sso.example.com. is the same host as sso.example.com. Turning it away denied a legitimate redirect
        // back to this server on every deployment that has no whitelist configured.
        putBehindTlsTerminatingProxy();
        assertThat(validator.isRedirectUrlValid(request, "https://sso.example.com./openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://attacker.example.com./", "realm")).isFalse();
    }

    @Test
    public void shouldNotLetAUnicodeOriginVouchForTheNameIdna2003WouldFoldItTo() {
        // java.net.IDN is IDNA2003, which maps characters rather than preserving them: it turns
        // gro<sharp s>.example.com into gross.example.com, a separately registrable third party domain.
        // Canonicalising the origin with it handed that domain this server's own origin.
        validator.configureServer("https", "gro\u00df.example.com", 443);
        assertThat(validator.isRedirectUrlValid(null, "https://gross.example.com/openam/", "realm")).isFalse();
        // And it would not have served the name it was applied for either: a browser reaching that host sends
        // the UTS46 form, xn--gro-7ka.example.com, which IDNA2003 never produces.
        assertThat(validator.isRedirectUrlValid(null, "https://xn--gro-7ka.example.com/openam/", "realm"))
                .isFalse();
    }

    @Test
    public void shouldNotFoldTheHostTheContainerReports() {
        // getServerName() reaches the comparison with no java.net.URI in front of it to reject a non ASCII
        // name, so the container side has to be left unfolded as well: IDNA2003 maps a soft hyphen away, which
        // turned this Host header into sso.example.com.
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("sso.exam\u00adple.com");
        when(request.getServerPort()).thenReturn(443);
        assertThat(validator.isRedirectUrlValid(request, "https://sso.example.com/openam/", "realm")).isFalse();
    }

    @Test
    public void shouldAcceptAnInternationalisedOriginNamedInItsAsciiForm() {
        // A host is compared as it stands, so a deployment with an internationalised name sets
        // com.iplanet.am.server.host to the ASCII compatible form of it - what the whitelist path expects too.
        validator.configureServer("https", "xn--gro-7ka.example.com", 443);
        assertThat(validator.isRedirectUrlValid(null, "https://xn--gro-7ka.example.com/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(null, "https://xn--gro-7ka.example.com.evil/", "realm")).isFalse();
        assertThat(validator.isRedirectUrlValid(null, "https://gross.example.com/openam/", "realm")).isFalse();
    }

    @Test
    public void shouldRejectAUrlWhoseHostCannotBeParsed() {
        // https://good%2ecom/ leaves URI.getHost() null. There is an authority to fall back on, but it carries
        // the userinfo and the port as well - https://sso.example.com@attacker.example.com/ has an authority
        // naming this server - so a host that cannot be read is answered the fail safe way instead.
        putBehindTlsTerminatingProxy();
        assertThat(validator.isRedirectUrlValid(request, "https://sso.example%2ecom/openam/", "realm")).isFalse();
    }

    @Test
    public void shouldNotLetTheContainerOriginVouchForAThirdHost() {
        // Accepting the origin the container reports is bounded: it admits URLs pointing at that origin, not
        // URLs of some other host that merely arrived on a request carrying an unrelated Host header. Without
        // this the accept could be widened to "there is a request, so allow it" and the suite would not notice.
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("lb.example.com");
        when(request.getServerPort()).thenReturn(443);
        validator.configureServer("https", "sso.example.com", 443);

        assertThat(validator.isRedirectUrlValid(request, "https://other.example.com/openam/", "realm")).isFalse();
        // Bounded by an equality on the host as well, not by a suffix of the name the container reports.
        assertThat(validator.isRedirectUrlValid(request, "https://evillb.example.com/openam/", "realm")).isFalse();
        assertThat(validator.isRedirectUrlValid(request, "https://lb.example.com/openam/", "realm")).isTrue();
    }

    @Test
    public void shouldFallBackToTheContainerOriginWhileTheServerPropertiesAreUnset() {
        // An installation that never set com.iplanet.am.server.* still has to work.
        assertThat(validator.isRedirectUrlValid(request, "https://openam.example.com/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
    }

    @Test
    public void shouldFallBackToTheConfiguredOriginWithoutARequest() {
        // WSFederationUtils.isWReplyURLValid(metaAlias, wreply, role) has no request to offer.
        validator.configureServer("https", "sso.example.com", 443);
        assertThat(validator.isRedirectUrlValid(null, "https://sso.example.com/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(null, "https://attacker.example.com/", "realm")).isFalse();
        assertThat(validator.isRedirectUrlValid(null, "/openam/XUI/", "realm")).isTrue();
    }

    @Test
    public void shouldRejectEveryAbsoluteUrlWhenNeitherOriginIsKnown() {
        assertThat(validator.isRedirectUrlValid(null, "https://openam.example.com/openam/", "realm")).isFalse();
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
    public void shouldReadTheWhitelistOnlyOnce() {
        // Reading it a second time would take a configuration error, which the extractors report as null, for
        // "no whitelist configured" - and that allows every URL.
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        assertThat(validator.isRedirectUrlValid(request, "https://partner.example.com/slo", "realm")).isTrue();
        assertThat(extractor.reads).isEqualTo(1);
    }

    @Test
    public void shouldReadEachConfigurationOnlyOnce() {
        // Taking the whitelists together costs one read per configuration named, and no more.
        extractor.patterns.put("idp", Collections.singletonList("https://partner.example.com/*"));
        extractor.patterns.put("sp", Collections.singletonList("https://partner.example.com/*"));
        List<String> configInfos = Arrays.asList("idp", "sp");
        assertThat(validator.isRedirectUrlValid(request, "https://partner.example.com/slo", configInfos)).isTrue();
        assertThat(extractor.reads).isEqualTo(configInfos.size());
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
    public void shouldTakeTheWhitelistsOfAllConfigurationsTogether() {
        // A dual role entity must not have the list of one role shadow the other's: whichever configuration the
        // URL is on, it is accepted.
        ValidDomainExtractor<String> byConfigInfo = new ValidDomainExtractor<String>() {

            @Override
            public Collection<String> extractValidDomains(String configInfo) {
                return Collections.singletonList("https://" + configInfo + ".example.com/*");
            }
        };
        TestValidator unioning = new TestValidator(byConfigInfo);
        List<String> configInfos = Arrays.asList("idp", "sp");
        assertThat(unioning.isRedirectUrlValid(request, "https://sp.example.com/slo", configInfos)).isTrue();
        assertThat(unioning.isRedirectUrlValid(request, "https://idp.example.com/slo", configInfos)).isTrue();
        assertThat(unioning.isRedirectUrlValid(request, "https://attacker.example.com/", configInfos)).isFalse();
    }

    @Test
    public void shouldKeepDecidingWhenOneConfigurationCannotBeRead() {
        // A whitelist that throws contributes nothing; the one that can be read still decides, and a URL on
        // neither is refused rather than let through.
        ValidDomainExtractor<String> throwing = new ValidDomainExtractor<String>() {

            @Override
            public Collection<String> extractValidDomains(String configInfo) {
                if ("broken".equals(configInfo)) {
                    throw new IllegalStateException("the configuration cannot be read");
                }
                return Collections.singletonList("https://partner.example.com/*");
            }
        };
        TestValidator resilient = new TestValidator(throwing);
        List<String> configInfos = Arrays.asList("broken", "realm");
        assertThat(resilient.isRedirectUrlValid(request, "https://partner.example.com/slo", configInfos)).isTrue();
        assertThat(resilient.isRedirectUrlValid(request, "https://attacker.example.com/", configInfos)).isFalse();
    }

    @Test
    public void shouldFallBackToOriginWhenEveryConfigurationThrows() {
        ValidDomainExtractor<String> throwing = new ValidDomainExtractor<String>() {

            @Override
            public Collection<String> extractValidDomains(String configInfo) {
                throw new IllegalStateException("the configuration cannot be read");
            }
        };
        TestValidator resilient = new TestValidator(throwing);
        assertThat(resilient.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
        assertThat(resilient.isRedirectUrlValid(request, "https://openam.example.com/openam/", "realm")).isTrue();
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

    @Test
    public void shouldAcceptAnAdditionalOriginThisServerAnswersOn() {
        // Behind a TLS terminating reverse proxy the container reports the internal origin for a request that
        // reached OpenAM on the published one, so the published one has to count as this server too.
        FailSafeRedirectUrlValidator<String> published = new PublishedAtSsoExampleCom(extractor);

        assertThat(published.isRedirectUrlValid(request, "https://sso.example.com/openam/console", "realm")).isTrue();
        // Still an origin comparison, not a prefix one.
        assertThat(published.isRedirectUrlValid(request, "https://sso.example.com:8443/openam/", "realm")).isFalse();
        assertThat(published.isRedirectUrlValid(request, "https://sso.example.com.attacker.example.com/", "realm"))
                .isFalse();
        assertThat(published.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
        // The origin of the request itself keeps working alongside it.
        assertThat(published.isRedirectUrlValid(request, "https://openam.example.com/openam/", "realm")).isTrue();
    }

    @Test
    public void shouldAcceptAnAdditionalOriginOfThisDeploymentWhenAWhitelistIsConfigured() {
        // An additional origin is this server as much as the one the container reports, so a URL of this
        // deployment on it needs no whitelist either; anything else on that host is still left to the list.
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        FailSafeRedirectUrlValidator<String> published = new PublishedAtSsoExampleCom(extractor);

        assertThat(published.isRedirectUrlValid(request, "https://sso.example.com/openam/console", "realm")).isTrue();
        assertThat(published.isRedirectUrlValid(request, "https://sso.example.com/otherapp/x", "realm")).isFalse();
    }

    @Test
    public void shouldCompareTheRequestOriginWithTheDefaultPortItNamesNone() {
        // A container answering an HTTP/1.0 request, or a request wrapper filling in nothing, reports no port at
        // all. Comparing that against the 443 the URL side is normalised to would drop every absolute URL naming
        // this very server on a realm that has no whitelist - the deployments this validator is here to keep
        // working.
        HttpServletRequest noPort = mock(HttpServletRequest.class);
        when(noPort.getScheme()).thenReturn("https");
        when(noPort.getServerName()).thenReturn("openam.example.com");
        when(noPort.getServerPort()).thenReturn(-1);

        assertThat(validator.isRedirectUrlValid(noPort, "https://openam.example.com/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(noPort, "https://openam.example.com:443/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(noPort, "https://openam.example.com:8443/openam/", "realm"))
                .isFalse();
        assertThat(validator.isRedirectUrlValid(noPort, "https://attacker.example.com/", "realm")).isFalse();
    }

    @Test
    public void shouldRefuseARedirectWhenTheRequestWillNotSayWhereItWasAddressed() {
        // Every other input this class reads is allowed to be missing without throwing, and a wrapped request is
        // somebody else's code: a request with no host is a redirect to refuse, not a NullPointerException to
        // throw out of the login path.
        HttpServletRequest hostless = mock(HttpServletRequest.class);
        when(hostless.getScheme()).thenReturn("https");
        when(hostless.getServerName()).thenReturn(null);
        when(hostless.getServerPort()).thenReturn(443);

        assertThat(validator.isRedirectUrlValid(hostless, "https://openam.example.com/openam/", "realm")).isFalse();
        assertThat(validator.isRedirectUrlValid(hostless, "/openam/XUI/", "realm")).isTrue();
    }

    @Test
    public void shouldCompareAnIpv6LiteralHostWithoutItsBrackets() {
        // java.net.URI hands back "[::1]" while the container may report "::1" for the same host.
        HttpServletRequest ipv6Request = mock(HttpServletRequest.class);
        when(ipv6Request.getScheme()).thenReturn("https");
        when(ipv6Request.getServerName()).thenReturn("::1");
        when(ipv6Request.getServerPort()).thenReturn(443);

        assertThat(validator.isRedirectUrlValid(ipv6Request, "https://[::1]/openam/", "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(ipv6Request, "https://[::2]/openam/", "realm")).isFalse();
    }

    @Test
    public void shouldApplyTheSameUrlLengthLimitAsTheLegacyValidator() {
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        RedirectUrlValidator<String> legacy = new RedirectUrlValidator<String>(extractor);
        String atTheLimit = whitelistedUrlOfLength(RedirectUrlValidator.MAX_URL_LENGTH);
        String overTheLimit = whitelistedUrlOfLength(RedirectUrlValidator.MAX_URL_LENGTH + 1);

        // Both validators take the limit from RedirectUrlValidator.MAX_URL_LENGTH, which reads
        // org.forgerock.openam.redirecturlvalidator.maxUrlLength once, at class initialisation. Nothing sets that
        // property in this JVM, so these rows pin the boundary at the default and that the two agree on it - not
        // that a raised limit reaches both.
        assertThat(legacy.isRedirectUrlValid(atTheLimit, "realm")).isTrue();
        assertThat(validator.isRedirectUrlValid(request, atTheLimit, "realm")).isTrue();
        assertThat(legacy.isRedirectUrlValid(overTheLimit, "realm")).isFalse();
        assertThat(validator.isRedirectUrlValid(request, overTheLimit, "realm")).isFalse();
    }

    @Test
    public void shouldMatchAgainstTheWhitelistItAlreadyRead() {
        // ValidGotoUrlExtractor caches the patterns, drops the entry whenever the configuration changes and hands
        // back nothing at all when the configuration store is unreachable, so two reads of the same realm can
        // disagree. Reading twice would let the second one come back empty and turn a configured realm into the
        // open redirect this validator exists to close.
        List<String> patterns = Collections.singletonList("https://partner.example.com/*");
        VanishingExtractor vanishing = new VanishingExtractor(patterns);
        FailSafeRedirectUrlValidator<String> validator = new FailSafeRedirectUrlValidator<String>(vanishing);

        assertThat(validator.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
        assertThat(vanishing.reads).isEqualTo(1);
    }

    @Test
    public void shouldFallBackToOriginWhenTheWhitelistCouldNotBeRead() {
        // The extractor answers null - the store is unreachable - rather than an empty collection. Nothing is
        // known about the patterns, so the URL only passes when it points back at this server.
        assertThat(validator.isRedirectUrlValid(request, "https://partner.example.com/slo", UNREADABLE)).isFalse();
        assertThat(validator.isRedirectUrlValid(request, "https://openam.example.com/openam/", UNREADABLE)).isTrue();
        assertThat(validator.isRedirectUrlValid(request, "/openam/XUI/", UNREADABLE)).isTrue();
    }

    @Test
    public void shouldAcceptAnyUrlWhenTheWhitelistIsEmptyAndTheEscapeHatchIsOn() {
        FailSafeRedirectUrlValidator<String> hatched = new WithEscapeHatch(extractor);

        assertThat(hatched.isRedirectUrlValid(request, "https://partner.example.com/slo", EMPTY)).isTrue();
    }

    @Test
    public void shouldKeepAPopulatedWhitelistAuthoritativeWhenTheEscapeHatchIsOn() {
        // The hatch only decides what an empty list means, so it may not be consulted before the list is read.
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        FailSafeRedirectUrlValidator<String> hatched = new WithEscapeHatch(extractor);

        assertThat(hatched.isRedirectUrlValid(request, "https://attacker.example.com/", "realm")).isFalse();
        assertThat(hatched.isRedirectUrlValid(request, "https://partner.example.com/slo", "realm")).isTrue();
    }

    @Test
    public void shouldNotLetTheEscapeHatchActOnAWhitelistItCouldNotRead() {
        // The extractors swallow the exception the configuration store throws and answer null, which used to be
        // indistinguishable from "no list is configured": a store that blinked handed the hatch a deployment that
        // did populate its list, and with it the open redirect this validator exists to close.
        FailSafeRedirectUrlValidator<String> hatched = new WithEscapeHatch(extractor);

        assertThat(hatched.isRedirectUrlValid(request, "https://attacker.example.com/", UNREADABLE)).isFalse();
        assertThat(hatched.isRedirectUrlValid(request, "https://openam.example.com/openam/", UNREADABLE)).isTrue();
    }

    @Test
    public void shouldStillFallBackToOriginWhenOneOfSeveralConfigurationsCouldNotBeRead() {
        List<String> configInfos = Arrays.asList(UNREADABLE, EMPTY);
        FailSafeRedirectUrlValidator<String> hatched = new WithEscapeHatch(extractor);

        // One of them holds no list and the other could not be read, so it is not known that none of them has one.
        assertThat(hatched.isRedirectUrlValid(request, "https://attacker.example.com/", configInfos)).isFalse();
    }

    @Test
    public void shouldStillApplyTheWhitelistOfAConfigurationThatCouldBeRead() {
        extractor.patterns.put("realm", Collections.singletonList("https://partner.example.com/*"));
        List<String> configInfos = Arrays.asList(UNREADABLE, "realm");

        assertThat(validator.isRedirectUrlValid(request, "https://partner.example.com/slo", configInfos)).isTrue();
    }

    @Test
    public void shouldOnlyAcceptOwnOriginWhenTheCallerHasNoWhitelistToOffer() {
        // The caller could not even work out which configuration holds the list, so the hatch has no say either.
        FailSafeRedirectUrlValidator<String> hatched = new WithEscapeHatch(extractor);

        assertThat(hatched.isOwnOriginRedirectUrl(request, "https://attacker.example.com/")).isFalse();
        assertThat(hatched.isOwnOriginRedirectUrl(request, "https://openam.example.com/openam/")).isTrue();
        assertThat(hatched.isOwnOriginRedirectUrl(request, "/openam/XUI/")).isTrue();
        assertThat(hatched.isOwnOriginRedirectUrl(request, "javascript:alert(1)")).isFalse();
    }

    private static String whitelistedUrlOfLength(int length) {
        StringBuilder url = new StringBuilder("https://partner.example.com/");
        while (url.length() < length) {
            url.append('a');
        }
        return url.toString();
    }

    /**
     * Hands back the patterns once and nothing afterwards, the way the extractor behaves when the cached entry is
     * dropped between two reads and the configuration store cannot be reached for the second one.
     */
    private static final class VanishingExtractor implements ValidDomainExtractor<String> {

        private final Collection<String> patterns;

        private int reads;

        VanishingExtractor(Collection<String> patterns) {
            this.patterns = patterns;
        }

        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            return reads++ == 0 ? patterns : null;
        }
    }

    /**
     * A validator whose endpoints offer the escape hatch that reads an unconfigured whitelist the way
     * RedirectUrlValidator reads it, as permission to redirect anywhere.
     */
    private static final class WithEscapeHatch extends FailSafeRedirectUrlValidator<String> {

        WithEscapeHatch(ValidDomainExtractor<String> extractor) {
            super(extractor);
        }

        @Override
        protected boolean allowAnyUrlWithoutWhitelist() {
            return true;
        }
    }

    /**
     * A validator for an OpenAM published at https://sso.example.com while the container answers on another origin.
     */
    private static final class PublishedAtSsoExampleCom extends FailSafeRedirectUrlValidator<String> {

        PublishedAtSsoExampleCom(ValidDomainExtractor<String> extractor) {
            super(extractor);
        }

        @Override
        protected Collection<String> getAdditionalOrigins(HttpServletRequest request, Collection<String> configInfos) {
            Collection<String> origins = super.getAdditionalOrigins(request, configInfos);
            addOrigin(origins, "https://sso.example.com/openam");
            return origins;
        }
    }
}
