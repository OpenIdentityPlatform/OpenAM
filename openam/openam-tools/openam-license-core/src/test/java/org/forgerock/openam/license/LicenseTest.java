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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link License}.
 */
public class LicenseTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLicenseFilename() {
        new License(null, "...");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptyLicenseFilename() {
        new License("", "...");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLicenseText() {
        new License("...", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptyLicenseText() {
        new License("...", "");
    }

    @Test
    public void shouldReturnCorrectLicenseText() {
        // Given
        String filename = "...";
        String licenseText = "Some license text";
        License license = new License(filename, licenseText);

        // When
        String result = license.getLicenseText();

        // Then
        assertEquals(result, licenseText);
    }

    @Test
    public void shouldNotBeAcceptedByDefault() {
        // Given
        License license = new License("...", "...");

        // When
        boolean result = license.isAccepted();

        // Then
        assertFalse(result);
    }

    @Test
    public void shouldRecordAcceptance() {
        // Given
        License license = new License("...", "...");

        // When
        license.accept();

        // Then
        assertTrue(license.isAccepted());
    }

    @Test(expectedExceptions = LicenseRejectedException.class)
    public void shouldThrowExceptionOnRejection() {
        // Given
        License license = new License("...", "...");

        // When
        license.reject();

        // Then this should never be reached
    }

    @Test
    public void shouldRecordRejection() {
        // Given
        License license = new License("...", "...");
        license.accept();

        // When
        try { license.reject(); } catch (LicenseRejectedException _) {}

        // Then: last action should win
        assertFalse(license.isAccepted());
    }

    @Test(expectedExceptions = LicenseRejectedException.class)
    public void shouldIncludeRejectedLicenseInException() throws Exception {
        // Given
        License license = new License("...", "...");

        // When
        try { license.reject(); }

        // Then
        catch (LicenseRejectedException ex) {
            assertEquals(ex.getRejectedLicense(), license);
            throw ex;
        }

    }

    @Test
    public void shouldReturnCorrectLines() {
        // Given
        String licenseText = "line1\nline2\nline3\n";
        License license = new License("...", licenseText);

        // When
        Iterable<String> lines = license.lines();
        StringBuilder sb = new StringBuilder();
        for (String line : lines) { sb.append(line).append('\n'); }

        // Then
        assertEquals(sb.toString(), licenseText);
    }

    @Test
    public void shouldGetLicenseFilename() {
        // Given
        String licenseFilename = "filename";
        License license = new License(licenseFilename, "...");

        // When
        String filename = license.getFilename();

        // Then
        assertEquals(filename, licenseFilename);
    }

    @Test
    public void shouldCopyLicensesCorrectly() {
        // Given
        License original = new License("aaa", "bbb");

        // When
        License copy = new License(original);

        // Then
        assertEquals(copy, original);
    }
}
