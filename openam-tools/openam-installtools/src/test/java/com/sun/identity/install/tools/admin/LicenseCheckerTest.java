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

package com.sun.identity.install.tools.admin;

import org.forgerock.openam.install.tools.logs.DebugLog;
import org.forgerock.openam.license.License;
import org.forgerock.openam.license.LicenseLocator;
import org.forgerock.openam.license.LicenseLog;
import org.forgerock.openam.license.LicenseSet;
import org.forgerock.openam.license.User;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link LicenseChecker}.
 *
 * @since 12.0.0
 */
public class LicenseCheckerTest {
    private static final String USER = "testuser";
    private LicenseLocator mockLocator;
    private LicenseLog mockLog;
    private User mockUser;
    private DebugLog mockDebug;
    private License license;

    private LicenseChecker checker;


    @BeforeMethod
    public void setup() {
        mockLocator = mock(LicenseLocator.class);
        mockLog = mock(LicenseLog.class);
        mockUser = mock(User.class);
        mockDebug = mock(DebugLog.class);

        // Defaults
        given(mockUser.getName()).willReturn(USER);
        license = new License("aaa", "aaa");
        given(mockLocator.getRequiredLicenses()).willReturn(new LicenseSet(Collections.singletonList(license)));

        checker = new LicenseChecker(mockLocator, mockLog, mockUser, mockDebug);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLicenseLocator() {
        new LicenseChecker(null, mockLog, mockUser, mockDebug);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLicenseLog() {
        new LicenseChecker(mockLocator, null, mockUser, mockDebug);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullUser() {
        new LicenseChecker(mockLocator, mockLog, null, mockDebug);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDebugLog() {
        new LicenseChecker(mockLocator, mockLog, mockUser, null);
    }

    @Test
    public void shouldDisplayLicenseHeader() {
        // Given
        given(mockLocator.getRequiredLicenses()).willReturn(new LicenseSet(Collections.<License>emptyList()));

        // When
        checker.checkLicenseAcceptance();

        // Then
        verify(mockUser).tell(LicenseChecker.MSG_LICENSE_HEADER);
    }

    @Test
    public void shouldPresentLicensesWhenNotAlreadyAccepted() {
        // Given

        // When
        checker.checkLicenseAcceptance();

        // Then
        verify(mockUser).show(license.getLicenseText());
    }

    @Test
    public void shouldReportFailureIfLicenseRejected() {
        // Given
        given(mockUser.ask("prompt")).willReturn("n");

        // When
        boolean accepted = checker.checkLicenseAcceptance();

        // Then
        assertFalse(accepted);
    }

    @Test
    public void shouldReportSuccessIfLicenseAccepted() {
        // Given
        given(mockUser.ask("prompt")).willReturn("y");

        // When
        boolean accepted = checker.checkLicenseAcceptance();

        // Then
        assertTrue(accepted);
    }

    @Test
    public void shouldNotPromptIfLicensesAlreadyAccepted() {
        // Given
        given(mockUser.ask("prompt")).willReturn("y");
        checker.checkLicenseAcceptance(); // Accept first time

        // Should never be asked - if we are, then abort the test
        given(mockUser.ask("prompt")).willThrow(new AssertionError());

        // When
        boolean accepted = checker.checkLicenseAcceptance();

        // Then
        assertTrue(accepted);
    }

    @Test
    public void shouldRecordLicenseAcceptance() {
        // Given
        given(mockUser.ask("prompt")).willReturn("y");

        // When
        checker.checkLicenseAcceptance();

        // Then
        license.accept();
        verify(mockLog).logLicenseAccepted(eq(license), eq(USER), any(Date.class));
    }

    @Test
    public void shouldNotPromptIfLicenseAcceptanceRecorded() {
        // Given
        given(mockLog.isLicenseAccepted(license, USER)).willReturn(true);

        // When
        boolean accepted = checker.checkLicenseAcceptance();

        // Then
        assertTrue(accepted);
    }

    @Test
    public void shouldNotLogIfUsernameIsNull() {
        // Given
        given(mockUser.getName()).willReturn(null);
        checker = new LicenseChecker(mockLocator, mockLog, mockUser, mockDebug);

        // When
        checker.checkLicenseAcceptance();

        // Then
        verifyZeroInteractions(mockLog);
    }

    @Test
    public void shouldNotLogIfUsernameIsEmpty() {
        // Given
        given(mockUser.getName()).willReturn("  ");
        checker = new LicenseChecker(mockLocator, mockLog, mockUser, mockDebug);

        // When
        checker.checkLicenseAcceptance();

        // Then
        verifyZeroInteractions(mockLog);
    }
}
