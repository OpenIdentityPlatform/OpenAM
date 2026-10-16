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
package org.forgerock.openam.oauth2.validation;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.validation.ValidationException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertThrows;

/**
 * Tests the SSRF guard for the OIDC {@code sector_identifier_uri} fetch (GHSA-7c7p-4mff-c9vg).
 * Only IP-literal hosts are used, so the tests never perform DNS resolution or network access.
 */
public class SsrfUrlValidatorTest {

    private final SsrfUrlValidator validator = SsrfUrlValidator.getInstance();

    /**
     * Resets both properties around every test, so that no method depends on the order the others
     * ran in. The NAT64 list is emptied rather than unset because {@code SystemProperties} stores
     * into a {@link java.util.Properties}, which rejects a null value; an empty list means the same
     * thing to the validator, and nothing here exercises its JVM-property fallback.
     */
    @BeforeMethod
    @AfterMethod
    public void resetProperties() {
        SystemProperties.initializeProperties(SsrfUrlValidator.ALLOW_ANY_URL_PROPERTY, "false");
        SystemProperties.initializeProperties(
                com.sun.identity.common.SsrfUrlValidator.NAT64_PREFIXES_PROPERTY, "");
    }

    @DataProvider(name = "unsafeUrls")
    public Object[][] unsafeUrls() {
        return new Object[][]{
                {null},
                {""},
                {"not a url"},
                {"http://8.8.8.8/file"},                        // not https
                {"file:///etc/passwd"},                         // file scheme
                {"ftp://8.8.8.8/x"},                            // ftp scheme
                {"https://127.0.0.1/x"},                        // loopback
                {"https://[::1]/x"},                            // IPv6 loopback
                {"https://0.0.0.0/x"},                          // any-local
                {"https://169.254.169.254/latest/meta-data/"},  // link-local / cloud metadata
                {"https://10.1.2.3/x"},                         // RFC1918
                {"https://172.16.5.4/x"},                       // RFC1918
                {"https://192.168.1.1/x"},                      // RFC1918
                {"https://100.100.100.200/latest/meta-data/"},  // 100.64.0.0/10 shared addr (Alibaba metadata)
                {"https://100.64.0.1/x"},                       // 100.64.0.0/10 lower boundary
                {"https://100.127.255.254/x"},                  // 100.64.0.0/10 upper boundary
                {"https://0.1.2.3/x"},                          // 0.0.0.0/8 this host on this network
                {"https://192.0.0.1/x"},                        // 192.0.0.0/24 IETF protocol assignments
                {"https://192.88.99.1/x"},                      // 192.88.99.0/24 deprecated 6to4 relay anycast
                {"https://198.18.0.1/x"},                       // 198.18.0.0/15 benchmarking
                {"https://198.19.255.254/x"},                   // 198.18.0.0/15 upper boundary
                {"https://240.0.0.1/x"},                        // 240.0.0.0/4 reserved
                {"https://255.255.255.255/x"},                  // limited broadcast
                {"https://[64:ff9b::c0a8:0101]/x"},             // NAT64 WKP -> 192.168.1.1
                {"https://[64:ff9b::ac10:fe01]/x"},             // NAT64 WKP -> 172.16.254.1
                {"https://[64:ff9b::7f00:0001]/x"},             // NAT64 WKP -> 127.0.0.1
                {"https://[64:ff9b::a9fe:a9fe]/x"},             // NAT64 WKP -> 169.254.169.254
                {"https://[64:ff9b::6464:64c8]/x"},             // NAT64 WKP -> 100.100.100.200 (shared addr)
                {"https://[64:ff9b:1::7f00:1]/x"},              // NAT64 local-use RFC 8215 -> 127.0.0.1
                {"https://[64:ff9b:1::6464:64c8]/x"},           // NAT64 local-use RFC 8215 -> 100.100.100.200
                {"https://[64:ff9b:1:abcd::7f00:1]/x"},         // another /96 inside the RFC 8215 /48
                {"https://[64:ff9b:1:1:7f:0:100:0]/x"},         // /64 embedding inside the RFC 8215 /48
                {"https://[64:ff9b:1::808:808]/x"},             // whole /48 is translation-only space
                {"https://[2002:c0a8:0101::]/x"},               // 6to4 -> 192.168.1.1
                {"https://[2002:0a00:0001::]:8443/internal"},   // 6to4 -> 10.0.0.1
                {"https://[2002:6440:0001::]/x"},               // 6to4 -> 100.64.0.1 (shared addr)
                {"https://[2001:0000:4136:e378:8000:63bf:3f57:fefe]/x"}, // Teredo -> 192.168.1.1
                {"https://[2001:db8::5efe:c0a8:101]/x"},        // ISATAP -> 192.168.1.1
                {"https://[2001:db8::200:5efe:7f00:1]/x"},      // ISATAP, global-bit IID -> 127.0.0.1
                {"https://[2002:808:808:0:0:5efe:c0a8:101]/x"}, // 6to4 of 8.8.8.8 + ISATAP 192.168.1.1
                {"https://[::ffff:127.0.0.1]/x"},               // IPv4-mapped -> 127.0.0.1
                {"https://[::c0a8:0101]/x"},                    // IPv4-compatible -> 192.168.1.1
        };
    }

    @Test(dataProvider = "unsafeUrls")
    public void rejectsUnsafeUrl(String url) {
        assertThrows(ValidationException.class, () -> validator.validate(url));
    }

    @DataProvider(name = "safeUrls")
    public Object[][] safeUrls() {
        return new Object[][]{
                {"https://8.8.8.8/sector.json"},        // public IPv4 literal (no DNS)
                {"https://93.184.216.34/sector.json"},  // public IPv4 literal (no DNS)
                {"https://100.63.255.254/sector.json"}, // just below 100.64.0.0/10, public
                {"https://100.128.0.1/sector.json"},    // just above 100.64.0.0/10, public
                {"https://198.20.0.1/sector.json"},     // just above 198.18.0.0/15, public
                {"https://[2002:0808:0808::]/sector.json"}, // 6to4 of public 8.8.8.8 stays allowed
                {"https://[64:ff9b::808:808]/sector.json"},   // NAT64 WKP of public 8.8.8.8
                {"https://[64:ff9b:2::808:808]/sector.json"}, // outside the RFC 8215 /48
                {"https://[2001:db8::5efe:808:808]/sector.json"}, // ISATAP of public 8.8.8.8
        };
    }

    @Test(dataProvider = "safeUrls")
    public void acceptsPublicHttpsUrl(String url) throws ValidationException {
        validator.validate(url);
    }

    /**
     * The NAT64 Network-Specific Prefix list is read inside the shared validator, so it applies to
     * every call site rather than only to the one that happens to read the property.
     */
    @Test
    public void nat64NetworkSpecificPrefixPropertyReachesThisCallSiteToo() throws ValidationException {
        final String url = "https://[2001:db8:122:344::c0a8:101]/sector.json"; // NSP /96 -> 192.168.1.1
        validator.validate(url);
        SystemProperties.initializeProperties(
                com.sun.identity.common.SsrfUrlValidator.NAT64_PREFIXES_PROPERTY,
                "2001:db8:122:344::/96");
        assertThrows(ValidationException.class, () -> validator.validate(url));
    }

    @Test
    public void allowAnyUrlPropertyBypassesValidation() throws ValidationException {
        SystemProperties.initializeProperties(SsrfUrlValidator.ALLOW_ANY_URL_PROPERTY, "true");
        validator.validate("http://127.0.0.1/x");
        validator.validate("file:///etc/passwd");
    }
}
