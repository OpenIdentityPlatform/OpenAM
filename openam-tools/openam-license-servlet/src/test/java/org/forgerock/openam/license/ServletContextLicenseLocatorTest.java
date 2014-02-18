/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.license;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Unit tests for {@link ServletContextLicenseLocator}.
 *
 * @since 12.0.0
 */
public class ServletContextLicenseLocatorTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ServletContext mockContext;
    private ServletContextLicenseLocator testLocator;


    @BeforeMethod
    public void setup() {
        mockContext = mock(ServletContext.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullCharset() {
        new ServletContextLicenseLocator(mockContext, null, "a");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullServletContext() {
        new ServletContextLicenseLocator(null, UTF8, "a");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptyLicenseList() {
        new ServletContextLicenseLocator(mockContext, UTF8);
    }

    @Test(expectedExceptions = MissingLicenseException.class)
    public void shouldThrowExceptionForMissingLicense() {
        // Given
        testLocator = new ServletContextLicenseLocator(mockContext, UTF8, "nosuchfile.txt");

        // When
        testLocator.getRequiredLicenses();

        // Then
    }

    @Test
    public void shouldReturnFullLicenseContents() {
        // Given
        String licenseName = "aaa";
        String licenseText = "some\nsample license\ntext";
        testLocator = new ServletContextLicenseLocator(mockContext, UTF8, licenseName);
        given(mockContext.getResourceAsStream(licenseName)).willReturn(new ByteArrayInputStream(licenseText.getBytes(UTF8)));

        // When
        LicenseSet result = testLocator.getRequiredLicenses();

        // Then
        assertNotNull(result);
        assertEquals(result.getLicenses().size(), 1);
        assertEquals(result.getLicenses().get(0), new License(licenseName, licenseText));
    }

}
