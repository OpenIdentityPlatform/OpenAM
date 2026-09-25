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
package org.forgerock.openam.oauth2.validation;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.validation.ValidationException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertThrows;

/**
 * Tests the SSRF guard for the OIDC {@code jwks_uri} fetch (GHSA-g7cv-hh35-cc7c).
 * Only IP-literal hosts are used, so the tests never perform DNS resolution or network access.
 */
public class JwksUriValidatorTest {

    private final JwksUriValidator validator = JwksUriValidator.getInstance();

    @BeforeMethod
    @AfterMethod
    public void resetAllowAnyAddressProperty() {
        SystemProperties.initializeProperties(JwksUriValidator.ALLOW_ANY_ADDRESS_PROPERTY, "false");
    }

    @DataProvider(name = "unsafeUrls")
    public Object[][] unsafeUrls() {
        return new Object[][]{
                {null},
                {""},
                {"not a url"},
                // Non-HTTP schemes: URLConnection would happily open these, turning a registered
                // jwks_uri into a local file read.
                {"file:///etc/passwd"},
                {"ftp://8.8.8.8/keys"},
                {"jar:file:///opt/openam/lib/some.jar!/jwks.json"},
                {"http://127.0.0.1/jwks"},                      // loopback
                {"https://127.0.0.1/jwks"},                     // loopback
                {"https://[::1]/jwks"},                         // IPv6 loopback
                {"https://0.0.0.0/jwks"},                       // any-local
                {"https://169.254.169.254/latest/meta-data/"},  // link-local / cloud metadata
                {"https://10.1.2.3/jwks"},                      // RFC1918
                {"https://172.16.5.4/jwks"},                    // RFC1918
                {"https://192.168.1.1/jwks"},                   // RFC1918
                {"https://100.100.100.200/latest/meta-data/"},  // 100.64.0.0/10 shared addr
                {"https://0.1.2.3/jwks"},                       // 0.0.0.0/8
                {"https://192.0.0.1/jwks"},                     // 192.0.0.0/24
                {"https://198.18.0.1/jwks"},                    // 198.18.0.0/15 benchmarking
                {"https://240.0.0.1/jwks"},                     // 240.0.0.0/4 reserved
                {"https://255.255.255.255/jwks"},               // limited broadcast
                {"https://[64:ff9b::7f00:0001]/jwks"},          // NAT64 WKP -> 127.0.0.1
                {"https://[2002:c0a8:0101::]/jwks"},            // 6to4 -> 192.168.1.1
                {"https://[2001:0000:4136:e378:8000:63bf:3f57:fefe]/jwks"}, // Teredo -> 192.168.1.1
                {"https://[::ffff:127.0.0.1]/jwks"},            // IPv4-mapped -> 127.0.0.1
        };
    }

    @Test(dataProvider = "unsafeUrls")
    public void rejectsUnsafeUrl(String url) {
        assertThrows(ValidationException.class, () -> validator.validate(url));
    }

    @DataProvider(name = "safeUrls")
    public Object[][] safeUrls() {
        return new Object[][]{
                {"https://8.8.8.8/jwks"},          // public IPv4 literal (no DNS)
                {"https://93.184.216.34/jwks"},    // public IPv4 literal (no DNS)
                {"https://100.128.0.1/jwks"},      // just above 100.64.0.0/10, public
                {"https://[2002:0808:0808::]/jwks"}, // 6to4 of public 8.8.8.8 stays allowed
        };
    }

    @Test(dataProvider = "safeUrls")
    public void acceptsPublicHttpsUrl(String url) throws ValidationException {
        validator.validate(url);
    }

    /**
     * Plain {@code http} to a public host is allowed, unlike {@code sector_identifier_uri}: the
     * OpenID Connect specification does not mandate TLS for {@code jwks_uri}, and rejecting it
     * would break working registrations without preventing any SSRF.
     */
    @Test
    public void acceptsPublicHttpUrl() throws ValidationException {
        validator.validate("http://8.8.8.8/jwks");
    }

    @Test
    public void allowAnyAddressPropertyPermitsInternalAddresses() throws ValidationException {
        SystemProperties.initializeProperties(JwksUriValidator.ALLOW_ANY_ADDRESS_PROPERTY, "true");
        validator.validate("http://127.0.0.1/jwks");
        validator.validate("https://10.1.2.3/jwks");
        validator.validate("https://169.254.169.254/latest/meta-data/");
    }

    /**
     * The escape hatch exists for relying parties hosted on internal addresses. It must not hand
     * back the non-HTTP schemes, which have no legitimate use for a {@code jwks_uri} at all.
     */
    @DataProvider(name = "nonHttpUrls")
    public Object[][] nonHttpUrls() {
        return new Object[][]{
                {"file:///etc/passwd"},
                {"ftp://8.8.8.8/keys"},
                {"jar:file:///opt/openam/lib/some.jar!/jwks.json"},
                {"not a url"},
                {null},
        };
    }

    @Test(dataProvider = "nonHttpUrls")
    public void allowAnyAddressPropertyStillRejectsNonHttpSchemes(String url) {
        SystemProperties.initializeProperties(JwksUriValidator.ALLOW_ANY_ADDRESS_PROPERTY, "true");
        assertThrows(ValidationException.class, () -> validator.validate(url));
    }
}
