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

    @BeforeMethod
    @AfterMethod
    public void resetAllowAnyProperty() {
        SystemProperties.initializeProperties(SsrfUrlValidator.ALLOW_ANY_URL_PROPERTY, "false");
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
        };
    }

    @Test(dataProvider = "safeUrls")
    public void acceptsPublicHttpsUrl(String url) throws ValidationException {
        validator.validate(url);
    }

    @Test
    public void allowAnyUrlPropertyBypassesValidation() throws ValidationException {
        SystemProperties.initializeProperties(SsrfUrlValidator.ALLOW_ANY_URL_PROPERTY, "true");
        validator.validate("http://127.0.0.1/x");
        validator.validate("file:///etc/passwd");
    }
}
