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
 * Copyright 2013 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import org.fest.assertions.ComparisonFailureFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Unit test for URLResourceName.
 *
 * @author andrew.forrest@forgerock.com
 */
public class URLResourceNameTest {

    private URLResourceName resourceName;

    @BeforeMethod
    public void setUp() {
        resourceName = new URLResourceName();
    }

    /**
     * Tests that the normalisation process adheres to the expected rules.
     */
    @Test
    public void verifyPortHandling() {
        // Previous ports are maintained.
        assertNormalisation("http://www.example.com:80/hello", "http://www.example.com:80/hello");
        assertNormalisation("http://www.example.com:12345/hello", "http://www.example.com:12345/hello");

        // Verify default ports are added where appropriate.
        assertNormalisation("http://www.example.com/hello", "http://www.example.com:80/hello");
        assertNormalisation("https://www.example.com/hello", "https://www.example.com:443/hello");
        assertNormalisation("protocol://www.example.com/hello", "protocol://www.example.com/hello");

        // With wildcards.
        assertNormalisation("http://www.example.com:80/hello*", "http://www.example.com:80/hello*");
        assertNormalisation("http://www.example*:80/hello", "http://www.example*:80/hello");
        assertNormalisation("http*://www.example.com:80/hello", "http*://www.example.com:80/hello");

        // Special cases: Due to the wildcard positions it is not possible to determine the port.
        assertNormalisation("http*://www.example.com/hello", "http*://www.example.com/hello");
        assertNormalisation("http://www.example.com:*/hello", "http://www.example.com:*/hello");
    }

    /**
     * Convenient assertion method for verifying normalisation of resources.
     *
     * @param untreatedResource
     *         The resource before it's been normalised.
     * @param treatedResource
     *         The expected resource after it's been normalised.
     */
    private void assertNormalisation(String untreatedResource, String treatedResource) {
        try {
            assertThat(resourceName.canonicalize(untreatedResource)).isEqualTo(treatedResource);
        } catch (Exception e) {
            String message = "Normalisation failed: " + e.getMessage();
            throw ComparisonFailureFactory.comparisonFailure(message, treatedResource, "");
        }
    }

}
