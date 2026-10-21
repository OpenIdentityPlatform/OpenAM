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
package org.openidentityplatform.openam.federation.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sun.identity.configuration.FedLibSystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RealmGotoUrlValidatorTest {

    private static final String REALM = "/";

    /**
     * The properties these tests drive, each restored to what it found. The installation ones are set rather than
     * left alone on purpose: the origin fallback reads them, so leaving them to whatever else runs in this JVM would
     * make the assertions below depend on test ordering.
     */
    private static final String[] PROPERTIES = {
        Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST,
        Constants.AM_SERVER_PROTOCOL,
        Constants.AM_SERVER_HOST,
        Constants.AM_SERVER_PORT,
    };

    private final Map<String, String> previousValues = new HashMap<String, String>();

    private final List<String> realmsRead = new ArrayList<String>();

    private HttpServletRequest request;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);

        previousValues.clear();
        for (String property : PROPERTIES) {
            previousValues.put(property, SystemPropertiesManager.get(property));
        }
        setProperty(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        setProperty(Constants.AM_SERVER_PROTOCOL, "https");
        setProperty(Constants.AM_SERVER_HOST, "installed.example.com");
        setProperty(Constants.AM_SERVER_PORT, "443");
        realmsRead.clear();
    }

    @AfterMethod
    public void tearDown() {
        RealmGotoUrlValidator.setExtractor(null);
        for (String property : PROPERTIES) {
            String previous = previousValues.get(property);
            setProperty(property, previous == null ? "" : previous);
        }
    }

    private void listOf(final Collection<String> patterns) {
        RealmGotoUrlValidator.setExtractor(new ValidDomainExtractor<String>() {
            @Override
            public Collection<String> extractValidDomains(String realm) {
                realmsRead.add(realm);
                return patterns;
            }
        });
    }

    private void noList() {
        listOf(Collections.<String>emptySet());
    }

    /** Stands for a realm whose configuration could not be read at all. */
    private void unreadableList() {
        listOf(null);
    }

    private boolean isValid(String url) {
        return RealmGotoUrlValidator.isValid(request, url, REALM);
    }

    @Test
    public void refusesAnAbsoluteUrlOutsideTheRealmList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(isValid("https://evil.example/")).isFalse();
    }

    @Test
    public void acceptsAUrlInTheRealmList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(isValid("https://app.example.com/after/login")).isTrue();
        assertThat(realmsRead).containsExactly(REALM);
    }

    @Test
    public void acceptsARelativeUrlWhateverTheList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(isValid("/openam/console")).isTrue();
    }

    @Test
    public void refusesASchemeRelativeUrlOutsideTheList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(isValid("//evil.example/")).isFalse();
    }

    @Test
    public void refusesANonHttpScheme() {
        noList();

        assertThat(isValid("javascript:alert(1)")).isFalse();
    }

    /**
     * The same rule as for the login goto: a realm that configures no list is not an open redirect. It used to accept
     * any http(s) URL here, which is the redirect GHSA-2gwf-vxp5-f9v2 closes.
     */
    @Test
    public void refusesAUrlLeavingThisServerWhenTheRealmConfiguresNoList() {
        noList();

        assertThat(isValid("https://anywhere.example/")).isFalse();
        assertThat(isValid("//anywhere.example/")).isFalse();
        assertThat(isValid("https://openam.example.com@anywhere.example/")).isFalse();
    }

    @Test
    public void acceptsTheOriginsOfThisServerWhenTheRealmConfiguresNoList() {
        noList();

        assertThat(isValid("https://openam.example.com/openam/console")).isTrue();
        // The URL OpenAM was installed under is the published one behind a reverse proxy.
        assertThat(isValid("https://installed.example.com/openam/console")).isTrue();
        assertThat(isValid("https://installed.example.com:8443/openam/")).isFalse();
    }

    @Test
    public void permitsAnyHttpUrlWithoutAListWhenTheGotoPropertyIsSet() {
        // The escape hatch of the login goto, since this is the same list.
        noList();
        setProperty(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");

        assertThat(isValid("https://anywhere.example/")).isTrue();
        assertThat(isValid("javascript:alert(1)")).isFalse();
    }

    @Test
    public void keepsAPopulatedListAuthoritativeWhenTheGotoPropertyIsSet() {
        listOf(Collections.singleton("https://app.example.com/*"));
        setProperty(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");

        assertThat(isValid("https://evil.example/")).isFalse();
    }

    @Test
    public void doesNotTakeAnUnreadableListForAnEmptyOneEvenWithTheGotoPropertySet() {
        // A realm that did populate its list must not be opened up by a configuration store that could not be read.
        unreadableList();
        setProperty(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");

        assertThat(isValid("https://evil.example/")).isFalse();
        assertThat(isValid("https://openam.example.com/openam/console")).isTrue();
    }

    @Test
    public void refusesAnEmptyTarget() {
        noList();

        assertThat(isValid("")).isFalse();
        assertThat(isValid(null)).isFalse();
    }

    @Test
    public void treatsAMissingRealmAsTheRoot() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(RealmGotoUrlValidator.isValid(request, "https://app.example.com/x", null)).isTrue();
        assertThat(RealmGotoUrlValidator.isValid(request, "https://app.example.com/x", "")).isTrue();
        assertThat(realmsRead).containsExactly(REALM, REALM);
    }

    private static void setProperty(String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        new FedLibSystemProperties().initializeProperties(properties);
    }
}
