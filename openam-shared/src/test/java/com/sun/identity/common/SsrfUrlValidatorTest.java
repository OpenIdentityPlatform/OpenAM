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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the SSRF URL guard. Only IP-literal hosts are used, so no DNS resolution or network
 * access is performed.
 */
public class SsrfUrlValidatorTest {

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
        };
    }

    @Test(dataProvider = "safeUrls")
    public void acceptsPublicHttpAndHttpsUrl(String url) {
        assertTrue(SsrfUrlValidator.isSafeRemoteUrl(url), "should accept: " + url);
    }
}
