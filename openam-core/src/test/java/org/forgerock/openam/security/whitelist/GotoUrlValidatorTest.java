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
package org.forgerock.openam.security.whitelist;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.forgerock.openam.security.whitelist.GotoUrlValidator.BaseUrlSupplier;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.XForwardedHeadersBaseURLProvider;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the goto URL validation of the authentication endpoints, both for a realm with no
 * <code>Valid goto URL Resources</code> configured - the default - and for one that has a list.
 */
public class GotoUrlValidatorTest {

    private static final String REALM = "/";

    /**
     * Stands for a realm whose Valid goto URL Resources list was never populated, the way production sees it:
     * {@link ValidGotoUrlExtractor} hands back an empty collection rather than <code>null</code> for a realm that
     * has the service but no resources in it.
     */
    private static final ValidDomainExtractor<String> EMPTY_WHITELIST = new ValidDomainExtractor<String>() {
        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            return emptySet();
        }
    };

    /**
     * Stands for a realm whose list could not be read at all: {@link ValidGotoUrlExtractor} answers
     * <code>null</code> when the configuration store throws, which says nothing about what the realm allows.
     */
    private static final ValidDomainExtractor<String> UNREADABLE_WHITELIST = new ValidDomainExtractor<String>() {
        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            return null;
        }
    };

    /** Stands for a realm that allows one external application. */
    private static final ValidDomainExtractor<String> WHITELISTS_THE_APP = new ValidDomainExtractor<String>() {
        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            return singletonList("https://app.example.com:443/*");
        }
    };

    /** Stands for a realm with no Base URL Source service configured. */
    private static final BaseUrlSupplier NO_BASE_URL = new BaseUrlSupplier() {
        @Override
        public String getBaseUrl(HttpServletRequest request, String realm) {
            return null;
        }
    };

    /**
     * The properties this test drives, each restored to what it found. The installation ones are set rather than
     * left alone on purpose: the origin fallback reads them, so leaving them to whatever else runs in this JVM
     * would make the assertions below depend on test ordering.
     */
    private static final String[] PROPERTIES = {
        Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST,
        Constants.AM_SERVER_PROTOCOL,
        Constants.AM_SERVER_HOST,
        Constants.AM_SERVER_PORT,
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR,
    };

    private final Map<String, String> previousValues = new HashMap<String, String>();

    private HttpServletRequest request;
    private GotoUrlValidator unconfigured;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        unconfigured = new GotoUrlValidator(EMPTY_WHITELIST, NO_BASE_URL);

        previousValues.clear();
        for (String property : PROPERTIES) {
            previousValues.put(property, SystemProperties.get(property));
        }
        SystemProperties.initializeProperties(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        SystemProperties.initializeProperties(Constants.AM_SERVER_PROTOCOL, "https");
        SystemProperties.initializeProperties(Constants.AM_SERVER_HOST, "installed.example.com");
        SystemProperties.initializeProperties(Constants.AM_SERVER_PORT, "443");
        // Without a request the deployment descriptor is what bounds the path an absolute URL of this server may
        // take, so it is pinned along with the origin.
        SystemProperties.initializeProperties(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, "/openam");
    }

    @AfterMethod
    public void tearDown() {
        for (String property : PROPERTIES) {
            String previous = previousValues.get(property);
            SystemProperties.initializeProperties(property, previous == null ? "" : previous);
        }
    }

    @DataProvider(name = "urlsLeavingThisServer")
    public Object[][] urlsLeavingThisServer() {
        return new Object[][] {
            { "https://attacker.example.com/phishing" },
            { "http://attacker.example.com/" },
            { "//attacker.example.com/" },
            { "https://openam.example.com@attacker.example.com/" },
            { "https://openam.example.com:8443/openam/" },
            { "http://openam.example.com/openam/" },
        };
    }

    @Test(dataProvider = "urlsLeavingThisServer")
    public void shouldRejectGotoUrlLeavingThisServerWhenNoWhitelistIsConfigured(String gotoUrl) {
        assertThat(unconfigured.validate(request, gotoUrl, REALM)).isFalse();
    }

    @Test(dataProvider = "urlsLeavingThisServer")
    public void shouldRejectGotoUrlLeavingThisServerWhenTheWhitelistIsEmpty(String gotoUrl) {
        // The realm has the Validation Service but nothing in it, which is what a fresh installation looks like.
        GotoUrlValidator empty = new GotoUrlValidator(EMPTY_WHITELIST, NO_BASE_URL);

        assertThat(empty.validate(request, gotoUrl, REALM)).isFalse();
    }

    @Test(dataProvider = "urlsLeavingThisServer")
    public void shouldRejectGotoUrlLeavingThisServerWhenTheWhitelistCouldNotBeRead(String gotoUrl) {
        GotoUrlValidator unreadable = new GotoUrlValidator(UNREADABLE_WHITELIST, NO_BASE_URL);

        assertThat(unreadable.validate(request, gotoUrl, REALM)).isFalse();
    }

    @DataProvider(name = "urlsPointingBackAtThisServer")
    public Object[][] urlsPointingBackAtThisServer() {
        return new Object[][] {
            { "/openam/console" },
            { "/openam/XUI/#login/" },
            { "/openam/console?realm=%2Fsub&locale=en" },
            { "https://openam.example.com/openam/" },
            { "https://OPENAM.EXAMPLE.COM:443/openam/" },
        };
    }

    @Test(dataProvider = "urlsPointingBackAtThisServer")
    public void shouldAcceptGotoUrlPointingBackAtThisServer(String gotoUrl) {
        assertThat(unconfigured.validate(request, gotoUrl, REALM)).isTrue();
    }

    @Test(dataProvider = "urlsPointingBackAtThisServer")
    public void shouldAcceptGotoUrlPointingBackAtThisServerWhenTheWhitelistIsEmpty(String gotoUrl) {
        GotoUrlValidator empty = new GotoUrlValidator(EMPTY_WHITELIST, NO_BASE_URL);

        assertThat(empty.validate(request, gotoUrl, REALM)).isTrue();
    }

    @Test(dataProvider = "urlsLeavingThisServer")
    public void shouldAcceptAnyHttpUrlWhenTheLegacyPropertyIsSet(String gotoUrl) {
        SystemProperties.initializeProperties(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");

        assertThat(unconfigured.validate(request, gotoUrl, REALM)).isTrue();
    }

    @Test(dataProvider = "urlsLeavingThisServer")
    public void shouldNotAcceptAnyUrlWhenTheWhitelistCouldNotBeReadEvenWithTheLegacyPropertySet(String gotoUrl) {
        // ValidGotoUrlExtractor answers null when the configuration store throws, and the escape hatch used to
        // hand that answer the old allow-anything behaviour: a store that blinked turned a realm with a populated
        // list into the open redirect this validator closes.
        SystemProperties.initializeProperties(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        GotoUrlValidator unreadable = new GotoUrlValidator(UNREADABLE_WHITELIST, NO_BASE_URL);

        assertThat(unreadable.validate(request, gotoUrl, REALM)).isFalse();
    }

    @Test
    public void shouldStillAcceptOwnOriginWhenTheWhitelistCouldNotBeReadWithTheLegacyPropertySet() {
        SystemProperties.initializeProperties(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        GotoUrlValidator unreadable = new GotoUrlValidator(UNREADABLE_WHITELIST, NO_BASE_URL);

        assertThat(unreadable.validate(request, "https://openam.example.com/openam/", REALM)).isTrue();
        assertThat(unreadable.validate(request, "/openam/console", REALM)).isTrue();
    }

    @Test
    public void shouldReadANullRealmAsTheRootRealmWhenTheLegacyPropertyIsSet() {
        // RedirectUrlValidator, the path the hatch restores, hands a null realm to the extractor, which reads it
        // as the root realm - so turning the hatch on may not stop the root realm's list from applying.
        SystemProperties.initializeProperties(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        RecordingExtractor recording = new RecordingExtractor(singletonList("https://app.example.com:443/*"));
        GotoUrlValidator configured = new GotoUrlValidator(recording, NO_BASE_URL);

        assertThat(configured.validate(request, "https://attacker.example.com/", null)).isFalse();
        assertThat(configured.validate(request, "https://app.example.com/landing", null)).isTrue();
        assertThat(recording.realms).containsOnly("/");
    }

    @Test
    public void shouldNotLookUpARealmTheCallerDidNotNameWithoutTheLegacyProperty() {
        // Without the hatch a null realm keeps its stricter reading: no whitelist, compare origins.
        RecordingExtractor recording = new RecordingExtractor(singletonList("https://app.example.com:443/*"));
        GotoUrlValidator configured = new GotoUrlValidator(recording, NO_BASE_URL);

        assertThat(configured.validate(request, "https://app.example.com/landing", null)).isFalse();
        assertThat(configured.validate(request, "https://openam.example.com/openam/", null)).isTrue();
        assertThat(recording.realms).isEmpty();
    }

    @Test
    public void shouldRefuseANonHttpSchemeEvenWithTheLegacyPropertySet() {
        SystemProperties.initializeProperties(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");

        assertThat(unconfigured.validate(request, "javascript:alert(1)", REALM)).isFalse();
    }

    @Test
    public void shouldApplyTheWhitelistWhenTheRealmHasOne() {
        GotoUrlValidator configured = new GotoUrlValidator(WHITELISTS_THE_APP, NO_BASE_URL);

        assertThat(configured.validate(request, "https://app.example.com/landing", REALM)).isTrue();
        assertThat(configured.validate(request, "https://attacker.example.com/", REALM)).isFalse();
    }

    @Test
    public void shouldKeepTheWhitelistAuthoritativeWhenTheLegacyPropertyIsSet() {
        SystemProperties.initializeProperties(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        GotoUrlValidator configured = new GotoUrlValidator(WHITELISTS_THE_APP, NO_BASE_URL);

        // The escape hatch only decides what an *empty* list means; a configured list still rules.
        assertThat(configured.validate(request, "https://attacker.example.com/", REALM)).isFalse();
    }

    @Test
    public void shouldFallBackToTheAlternateUrlWhenTheGotoUrlIsRefused() {
        String alternate = "/openam/console";

        assertThat(unconfigured.redirectUrl(request, REALM, "https://attacker.example.com/", alternate))
                .isEqualTo(alternate);
    }

    @Test
    public void shouldReturnTheGotoUrlWhenItPointsBackAtThisServer() {
        assertThat(unconfigured.redirectUrl(request, REALM, "/openam/console", "/openam/XUI"))
                .isEqualTo("/openam/console");
    }

    @Test
    public void shouldReturnTheAlternateUrlWhenThereIsNoGotoUrl() {
        assertThat(unconfigured.redirectUrl(request, REALM, null, "/openam/console"))
                .isEqualTo("/openam/console");
    }

    @Test
    public void shouldRejectAnAbsoluteGotoUrlWhenThereIsNoRequestToCompareItAgainst() {
        assertThat(unconfigured.validate(null, "https://openam.example.com/openam/", REALM)).isFalse();
        assertThat(unconfigured.validate(null, "/openam/console", REALM)).isTrue();
    }

    @Test
    public void shouldAcceptTheUrlOpenAmWasInstalledUnder() {
        // The container may report an origin of its own - here openam.example.com - while the URL the deployment
        // installed OpenAM under is the published one. Both count, and neither depends on the request headers.
        assertThat(unconfigured.validate(request, "https://installed.example.com/openam/console", REALM)).isTrue();
        assertThat(unconfigured.validate(request, "https://installed.example.com:8443/openam/", REALM)).isFalse();
        assertThat(unconfigured.validate(request, "http://installed.example.com/openam/", REALM)).isFalse();
        // It is the one origin left to compare against when there is no request at all.
        assertThat(unconfigured.validate(null, "https://installed.example.com/openam/console", REALM)).isTrue();
    }

    @Test
    public void shouldAcceptTheOriginTheRealmIsPublishedUnder() {
        // A TLS terminating reverse proxy in front of the container: OpenAM answers on https://sso.example.com
        // while the container reports the internal http://openam-1.internal:8080 for the very same request.
        HttpServletRequest proxied = mock(HttpServletRequest.class);
        when(proxied.getScheme()).thenReturn("http");
        when(proxied.getServerName()).thenReturn("openam-1.internal");
        when(proxied.getServerPort()).thenReturn(8080);
        GotoUrlValidator published = new GotoUrlValidator(EMPTY_WHITELIST, new BaseUrlSupplier() {
            @Override
            public String getBaseUrl(HttpServletRequest ignored, String realm) {
                return "https://sso.example.com/openam";
            }
        });

        assertThat(published.validate(proxied, "https://sso.example.com/openam/console", REALM)).isTrue();
        assertThat(published.validate(proxied, "http://openam-1.internal:8080/openam/console", REALM)).isTrue();
        assertThat(published.validate(proxied, "https://attacker.example.com/", REALM)).isFalse();
        // The published origin is an origin, not a prefix: another port on the same host is still off limits.
        assertThat(published.validate(proxied, "https://sso.example.com:8443/openam/console", REALM)).isFalse();
    }

    /*
     * The two below drive the supplier production installs - the one that reads the realm's Base URL Source service
     * - rather than the decision it makes on its own, which is BaseURLProvidersTest's subject. Without them the
     * call site could be dropped and nothing would go red.
     */

    @Test
    public void shouldNotTakeAnOriginFromABaseUrlBuiltOutOfTheRequest() {
        // X-Forwarded-Proto/Host are set by the caller, so a base URL built out of them says nothing about the
        // origins this server answers on. The redirect being fixed comes straight back if one is trusted.
        BaseUrlSupplier configured = supplierOf(new XForwardedHeadersBaseURLProvider());

        assertThat(configured.getBaseUrl(request, REALM)).isNull();
    }

    @Test
    public void shouldTakeTheOriginFromABaseUrlTheDeploymentConfigured() {
        BaseURLProvider pinned = mock(BaseURLProvider.class);
        when(pinned.getRootURL(request)).thenReturn("https://sso.example.com/openam");

        assertThat(supplierOf(pinned).getBaseUrl(request, REALM)).isEqualTo("https://sso.example.com/openam");
    }

    /*
     * The tests above that go through the static entry points pass a null realm, so nothing is read behind them.
     * These put a recording validator in place and check the realm the entry points hand on, and that production
     * builds its validator on the base URL source that rejects request derived origins.
     */

    @Test
    public void shouldForwardTheRealmThroughTheStaticEntryPoints() {
        RecordingExtractor recording = new RecordingExtractor(singletonList("https://app.example.com:443/*"));
        GotoUrlValidator.setInstance(new GotoUrlValidator(recording, NO_BASE_URL));
        try {
            assertThat(GotoUrlValidator.isValid(request, "https://app.example.com/x", "o=sub,ou=services")).isTrue();
            assertThat(GotoUrlValidator.getRedirectUrl(request, "o=other,ou=services",
                    "https://attacker.example.com/", "/openam/console")).isEqualTo("/openam/console");
        } finally {
            GotoUrlValidator.setInstance(null);
        }

        assertThat(recording.realms).containsExactly("o=sub,ou=services", "o=other,ou=services");
    }

    @Test
    public void shouldBuildTheSharedValidatorOnTheConfiguredBaseUrl() {
        assertThat(GotoUrlValidator.defaultInstance().baseUrls())
                .isInstanceOf(GotoUrlValidator.ConfiguredBaseUrl.class);
    }

    /**
     * Hands back one list of patterns and records which realm it was asked for.
     */
    private static final class RecordingExtractor implements ValidDomainExtractor<String> {

        private final Collection<String> patterns;

        private final List<String> realms = new ArrayList<String>();

        RecordingExtractor(Collection<String> patterns) {
            this.patterns = patterns;
        }

        @Override
        public Collection<String> extractValidDomains(String configInfo) {
            realms.add(configInfo);
            return patterns;
        }
    }

    private BaseUrlSupplier supplierOf(final BaseURLProvider provider) {
        return new GotoUrlValidator.ConfiguredBaseUrl() {
            @Override
            BaseURLProvider providerFor(String realm) {
                return provider;
            }
        };
    }

    @Test
    public void shouldNotLetTheBaseUrlWidenAConfiguredWhitelistBeyondThisDeployment() {
        // The published origin is this server as much as the one the container reports, so a URL of this
        // deployment on it needs no whitelist entry, the same as one on the container's origin. Anything else on
        // that host is still left to the realm's list.
        when(request.getContextPath()).thenReturn("/openam");
        GotoUrlValidator configured = new GotoUrlValidator(WHITELISTS_THE_APP, new BaseUrlSupplier() {
            @Override
            public String getBaseUrl(HttpServletRequest ignored, String realm) {
                return "https://sso.example.com/openam";
            }
        });

        assertThat(configured.validate(request, "https://sso.example.com/openam/console", REALM)).isTrue();
        assertThat(configured.validate(request, "https://sso.example.com/otherapp/x", REALM)).isFalse();
    }

    @Test
    public void shouldSurviveABaseUrlLookupThatBlowsUp() {
        GotoUrlValidator broken = new GotoUrlValidator(EMPTY_WHITELIST, new BaseUrlSupplier() {
            @Override
            public String getBaseUrl(HttpServletRequest ignored, String realm) {
                throw new IllegalStateException("no injector here");
            }
        });

        assertThat(broken.validate(request, "https://attacker.example.com/", REALM)).isFalse();
        assertThat(broken.validate(request, "https://openam.example.com/openam/", REALM)).isTrue();
        // The request's own origin is accepted before the base URL is ever asked for, so it cannot tell whether the
        // failed lookup kept the other origins. The installed URL can: it is collected alongside the base URL.
        assertThat(broken.validate(request, "https://installed.example.com/openam/console", REALM)).isTrue();
    }

    /*
     * The static entry points below are the ones every call site uses. They are exercised with a null realm, which
     * is the one realm that needs no Validation Service lookup, so that they run without a configuration behind
     * them - what is being pinned here is the wiring, not the whitelist handling covered above.
     */

    @Test
    public void shouldValidateThroughTheStaticEntryPoint() {
        assertThat(GotoUrlValidator.isValid(request, "/openam/console", null)).isTrue();
        assertThat(GotoUrlValidator.isValid(request, "https://openam.example.com/openam/", null)).isTrue();
        assertThat(GotoUrlValidator.isValid(request, "https://attacker.example.com/", null)).isFalse();
        assertThat(GotoUrlValidator.isValid(request, "//attacker.example.com/", null)).isFalse();
    }

    @Test
    public void shouldFallBackToTheAlternateUrlThroughTheStaticEntryPoint() {
        assertThat(GotoUrlValidator.getRedirectUrl(request, null, "https://attacker.example.com/", "/openam/console"))
                .isEqualTo("/openam/console");
        assertThat(GotoUrlValidator.getRedirectUrl(request, null, "https://openam.example.com/openam/", "/openam/XUI"))
                .isEqualTo("https://openam.example.com/openam/");
        assertThat(GotoUrlValidator.getRedirectUrl(request, null, null, "/openam/console"))
                .isEqualTo("/openam/console");
    }
}
