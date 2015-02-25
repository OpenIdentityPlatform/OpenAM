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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link LicenseSet}.
 */
public class LicenseSetTest {
    private List<License> defaultLicenses;
    private LicenseSet defaultLicenseSet;

    @BeforeMethod
    public void givenDefaults() {
        defaultLicenses = Arrays.asList(new License("a", "a"), new License("b", "b"), new License("c", "c"));
        defaultLicenseSet = new LicenseSet(defaultLicenses);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLicenseList() {
        new LicenseSet(null);
    }

    @Test
    public void shouldIterateLicensesInOrder() {
        // Given

        // When
        List<License> result = new ArrayList<License>();
        for (License license : defaultLicenseSet) { result.add(license); }

        // Then
        assertEquals(result, defaultLicenses);
    }

    @Test
    public void shouldReturnLicensesInOrder() {
        // Given

        // When
        List<License> result = defaultLicenseSet.getLicenses();

        // Then
        assertEquals(result, defaultLicenses);
    }

    @Test
    public void shouldNotBeAcceptedByDefault() {
        // Given

        // When

        // Then
        assertFalse(defaultLicenseSet.isAccepted());
    }

    @Test
    public void shouldAcceptAll() {
        // Given

        // When
        defaultLicenseSet.acceptAll();

        // Then
        assertTrue(defaultLicenseSet.isAccepted());
    }

    @Test
    public void shouldAcceptEachIndividualLicense() {
        // Given
        for (License license : defaultLicenses) {
            assertFalse(license.isAccepted());
        }

        // When
        defaultLicenseSet.acceptAll();

        // Then
        for (License license : defaultLicenses) {
            assertTrue(license.isAccepted());
        }
    }
}
