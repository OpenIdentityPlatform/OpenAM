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
package com.sun.identity.common;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Tests the SSRF URL guard. Only IP-literal hosts are used, so no DNS resolution or network
 * access is performed.
 */
public class SsrfUrlValidatorTest {

    /**
     * Leaves {@link SsrfUrlValidator#NAT64_PREFIXES_PROPERTY} undeclared before every test, so that
     * no method depends on the order the others ran in. Note that this is not the same as declaring
     * an empty list: the validator only consults the JVM properties while the
     * {@code ISystemProperties} provider holds no value for the key at all.
     */
    @BeforeMethod
    public void undeclareNat64Prefixes() {
        clearNat64Prefixes();
        System.clearProperty(SsrfUrlValidator.NAT64_PREFIXES_PROPERTY);
    }

    @DataProvider(name = "unsafeUrls")
    public Object[][] unsafeUrls() {
        return new Object[][]{
                {null},
                {""},
                {"not a url"},
                {"file:///etc/passwd"},                         // non-http(s) scheme
                {"ftp://8.8.8.8/x"},                            // non-http(s) scheme
                {"gopher://8.8.8.8/x"},                         // non-http(s) scheme
                {"http://127.0.0.1/x"},                         // loopback
                {"https://127.0.0.1/x"},                        // loopback
                {"https://[::1]/x"},                            // IPv6 loopback
                {"https://0.0.0.0/x"},                          // any-local
                {"http://169.254.169.254/latest/meta-data/"},   // link-local / cloud metadata
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
                // The whole RFC 8215 /48 is blocked, whatever the sub-prefix and embedding offset.
                {"https://[64:ff9b:1:1::7f00:1]/x"},            // another /96 inside the /48
                {"https://[64:ff9b:1:abcd::7f00:1]/x"},         // another /96 inside the /48
                {"https://[64:ff9b:1:0:1::7f00:1]/x"},          // non-zero bits between prefix and IPv4
                {"https://[64:ff9b:1:1:7f:0:100:0]/x"},         // /64 embedding inside the /48
                {"https://[64:ff9b:1:1:0:7f00:100:0]/x"},       // arbitrary offset inside the /48
                {"https://[64:ff9b:1:ffff:ffff:ffff:7f00:1]/x"},
                {"https://[64:ff9b:1::808:808]/x"},             // translation-only space: blocked even
                {"https://[64:ff9b:1:ffff::808:808]/x"},        // when the embedded IPv4 is public
                {"https://[2002:c0a8:0101::]/x"},               // 6to4 -> 192.168.1.1
                {"https://[2002:0a00:0001::]:8443/internal"},   // 6to4 -> 10.0.0.1
                {"https://[2002:6440:0001::]/x"},               // 6to4 -> 100.64.0.1 (shared addr)
                {"https://[2001:0000:4136:e378:8000:63bf:3f57:fefe]/x"}, // Teredo -> 192.168.1.1
                {"https://[2001:db8::5efe:c0a8:101]/x"},        // ISATAP -> 192.168.1.1
                {"https://[2001:db8::200:5efe:7f00:1]/x"},      // ISATAP, global-bit IID -> 127.0.0.1
                {"https://[2001:db8::5efe:a9fe:a9fe]/x"},       // ISATAP -> 169.254.169.254 (metadata)
                {"https://[::5efe:a9fe:a9fe]/x"},               // ISATAP under the unspecified prefix
                {"https://[fe80::5efe:c0a8:101]/x"},            // ISATAP under the link-local prefix
                // 6to4 of public 8.8.8.8, but the interface identifier is an ISATAP one: both
                // embedded addresses are reachable, so the internal one still has to be caught.
                {"https://[2002:808:808:0:0:5efe:c0a8:101]/x"},
                {"https://[::ffff:127.0.0.1]/x"},               // IPv4-mapped -> 127.0.0.1
                {"https://[::c0a8:0101]/x"},                    // IPv4-compatible -> 192.168.1.1
        };
    }

    @Test(dataProvider = "unsafeUrls")
    public void rejectsUnsafeUrl(String url) {
        assertFalse(SsrfUrlValidator.isSafeRemoteUrl(url), "should reject: " + url);
    }

    @DataProvider(name = "safeUrls")
    public Object[][] safeUrls() {
        return new Object[][]{
                {"http://8.8.8.8/callback"},        // public IPv4 literal, http (allowed here)
                {"https://8.8.8.8/callback"},       // public IPv4 literal, https
                {"https://93.184.216.34/callback"}, // public IPv4 literal
                {"https://100.63.255.254/callback"}, // just below 100.64.0.0/10, public
                {"https://100.128.0.1/callback"},   // just above 100.64.0.0/10, public
                {"https://198.20.0.1/callback"},    // just above 198.18.0.0/15, public
                {"https://[2002:0808:0808::]/callback"}, // 6to4 of public 8.8.8.8 stays allowed
                {"https://[64:ff9b::808:808]/callback"}, // NAT64 WKP of public 8.8.8.8 stays allowed:
                                                         // RFC 6052 3.1 bars non-global IPv4 there
                {"https://[64:ff9b:2::808:808]/callback"}, // outside the RFC 8215 /48, not over-blocked
                {"https://[2001:db8::5efe:808:808]/callback"}, // ISATAP of public 8.8.8.8
                {"https://[2001:db8:122:344::c0a8:101]/callback"}, // NAT64 NSP, not declared: see
                                                                   // nat64NetworkSpecificPrefixes*
        };
    }

    @Test(dataProvider = "safeUrls")
    public void acceptsPublicHttpAndHttpsUrl(String url) {
        assertTrue(SsrfUrlValidator.isSafeRemoteUrl(url), "should accept: " + url);
    }

    @Test
    public void requireHttpsRejectsHttpButKeepsAddressChecks() {
        assertFalse(SsrfUrlValidator.isSafeRemoteUrl("http://8.8.8.8/x", true),
                "http must be rejected when requireHttps=true");
        assertTrue(SsrfUrlValidator.isSafeRemoteUrl("https://8.8.8.8/x", true),
                "public https host must be accepted when requireHttps=true");
        assertFalse(SsrfUrlValidator.isSafeRemoteUrl("https://[64:ff9b::7f00:0001]/x", true),
                "transition-address bypass must stay blocked when requireHttps=true");
    }

    /**
     * The JVM-property fallback only applies while the {@code ISystemProperties} provider has no
     * value for the key, which {@link #undeclareNat64Prefixes()} guarantees for every test.
     */
    @Test
    public void nat64NetworkSpecificPrefixIsReadFromJvmProperty() {
        final String url = "https://[2001:db8:122:344::c0a8:101]/x"; // NSP /96 -> 192.168.1.1
        assertTrue(SsrfUrlValidator.isSafeRemoteUrl(url), "undeclared NSP cannot be inferred");
        try {
            System.setProperty(SsrfUrlValidator.NAT64_PREFIXES_PROPERTY, "2001:db8:122:344::/96");
            assertFalse(SsrfUrlValidator.isSafeRemoteUrl(url), "declared NSP must be honoured");
        } finally {
            System.clearProperty(SsrfUrlValidator.NAT64_PREFIXES_PROPERTY);
        }
        assertTrue(SsrfUrlValidator.isSafeRemoteUrl(url),
                "clearing the property must take effect without a restart");
    }

    @DataProvider(name = "nat64Embeddings")
    public Object[][] nat64Embeddings() {
        // Prefix, then the RFC 6052 section 2.2 embedding of 192.168.1.1 and of public 8.8.8.8.
        return new Object[][]{
                {"2001:db8::/32", "2001:db8:c0a8:101::", "2001:db8:808:808::"},
                {"2001:db8:100::/40", "2001:db8:1c0:a801:1::", "2001:db8:108:808:8::"},
                {"2001:db8:122::/48", "2001:db8:122:c0a8:1:100::", "2001:db8:122:808:8:800::"},
                {"2001:db8:122:300::/56", "2001:db8:122:3c0:a8:101::", "2001:db8:122:308:8:808::"},
                {"2001:db8:122:344::/64", "2001:db8:122:344:c0:a801:100:0",
                        "2001:db8:122:344:8:808:800:0"},
                {"2001:db8:122:344::/96", "2001:db8:122:344::c0a8:101", "2001:db8:122:344::808:808"},
        };
    }

    @Test(dataProvider = "nat64Embeddings")
    public void nat64NetworkSpecificPrefixesCoverEveryEmbeddingLength(
            String prefix, String internal, String publicAddress) {
        try {
            setNat64Prefixes(prefix);
            assertFalse(SsrfUrlValidator.isSafeRemoteUrl("https://[" + internal + "]/x"),
                    "internal IPv4 embedded under declared prefix " + prefix + " must be blocked");
            assertTrue(SsrfUrlValidator.isSafeRemoteUrl("https://[" + publicAddress + "]/x"),
                    "public IPv4 embedded under declared prefix " + prefix + " stays allowed");
        } finally {
            clearNat64Prefixes();
        }
    }

    @Test
    public void invalidNat64PrefixEntriesAreSkippedWithoutDisablingTheRest() {
        try {
            setNat64Prefixes("not-an-address/96, 2001:db8:1::/33, 2001:db8:2::, /96, "
                    + "2001:db8:122:344::/96 ,");
            assertFalse(SsrfUrlValidator.isSafeRemoteUrl("https://[2001:db8:122:344::c0a8:101]/x"),
                    "the one valid entry must still apply");
            assertTrue(SsrfUrlValidator.isSafeRemoteUrl("https://[2001:db8:122:344::808:808]/x"),
                    "public embedded IPv4 stays allowed");
        } finally {
            clearNat64Prefixes();
        }
        assertTrue(SsrfUrlValidator.isSafeRemoteUrl("https://[2001:db8:122:344::c0a8:101]/x"),
                "no prefixes declared any more");
    }

    @Test
    public void declaredNat64PrefixDoesNotWeakenTheBuiltInRules() {
        try {
            setNat64Prefixes("2001:db8::/32");
            assertFalse(SsrfUrlValidator.isSafeRemoteUrl("https://[64:ff9b::7f00:0001]/x"),
                    "NAT64 Well-Known Prefix stays blocked");
            assertFalse(SsrfUrlValidator.isSafeRemoteUrl("https://[64:ff9b:1::808:808]/x"),
                    "RFC 8215 local-use prefix stays blocked");
            assertFalse(SsrfUrlValidator.isSafeRemoteUrl("https://127.0.0.1/x"),
                    "loopback stays blocked");
        } finally {
            clearNat64Prefixes();
        }
    }

    /**
     * Publishes the property through the {@code ISystemProperties} provider, which is how a
     * deployment sets it via {@code AMConfig.properties}, a {@code -D} argument or the SMS.
     */
    private static void setNat64Prefixes(String value) {
        SystemPropertiesManager.initializeProperties(
                SsrfUrlValidator.NAT64_PREFIXES_PROPERTY, value);
    }

    /**
     * Takes the property back to undeclared. The test provider stores this as an explicit
     * {@code null}, which is what {@code SystemPropertiesManager.get} reports as "not set" — an
     * empty string would not, and would leave the JVM-property fallback permanently disabled for
     * every test that follows. Beware that the provider's {@code getOrDefault} keys off
     * {@code containsKey}, so it returns {@code null} rather than the default for a cleared key.
     */
    private static void clearNat64Prefixes() {
        setNat64Prefixes(null);
    }
}
