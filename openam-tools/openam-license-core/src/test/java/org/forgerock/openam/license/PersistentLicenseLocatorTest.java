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

import java.util.Arrays;
import java.util.Date;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PersistentLicenseLocatorTest {
    private static final String USER = "testuser";
    private LicenseLocator mockLocator;
    private LicenseLog mockLog;
    private PersistentLicenseLocator testLocator;

    @BeforeMethod
    public void setup() {
        mockLocator = mock(LicenseLocator.class);
        mockLog = mock(LicenseLog.class);
        testLocator = new PersistentLicenseLocator(mockLocator, mockLog, USER);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLocator() {
        new PersistentLicenseLocator(null, mockLog, USER);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLog() {
        new PersistentLicenseLocator(mockLocator, null, USER);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullUserName() {
        new PersistentLicenseLocator(mockLocator, mockLog, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptyUserName() {
        new PersistentLicenseLocator(mockLocator, mockLog, " ");
    }

    @Test
    public void shouldReturnEqualLicenseSet() {
        // Given
        LicenseSet originalLicenses = new LicenseSet(Arrays.asList(new License("aaa", "aaa"), new License("bbb", "bbb")));
        given(mockLocator.getRequiredLicenses()).willReturn(originalLicenses);

        // When
        LicenseSet result = testLocator.getRequiredLicenses();

        // Then
        // Wrapped license set should be indistinguishable from the originals
        assertEquals(result, originalLicenses);
    }

    @Test
    public void shouldLogLicenseAcceptance() {
        // Given
        License one = new License("aaa", "aaa");
        License two = new License("bbb", "bbb");
        LicenseSet originalLicenses = new LicenseSet(Arrays.asList(one, two));
        given(mockLocator.getRequiredLicenses()).willReturn(originalLicenses);

        // When
        LicenseSet result = testLocator.getRequiredLicenses();
        result.acceptAll();

        // Then
        one.accept(); two.accept(); // Copies will be accepted and this is significant for equals() check
        verify(mockLog).logLicenseAccepted(eq(one), eq(USER), any(Date.class));
        verify(mockLog).logLicenseAccepted(eq(two), eq(USER), any(Date.class));
    }

    @Test
    public void shouldCheckLogForAcceptance() {
        // Given
        License l = new License("aaa", "aaa");
        LicenseSet originalLicense = new LicenseSet(Arrays.asList(l));
        given(mockLocator.getRequiredLicenses()).willReturn(originalLicense);
        given(mockLog.isLicenseAccepted(l, USER)).willReturn(true);

        // When
        LicenseSet result = testLocator.getRequiredLicenses();
        boolean accepted = result.isAccepted();

        // Then
        verify(mockLog).isLicenseAccepted(l, USER);
        assertTrue(accepted);
    }

}
