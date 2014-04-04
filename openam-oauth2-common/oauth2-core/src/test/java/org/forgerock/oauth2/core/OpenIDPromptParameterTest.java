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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @since 12.0.0
 */
public class OpenIDPromptParameterTest {

    @Test
    public void isValidShouldReturnFalseWithPromptNoneConsent() {

        //Given
        final OpenIDPromptParameter prompt = new OpenIDPromptParameter("none consent");

        //When
        final boolean valid = prompt.isValid();

        //Then
        assertFalse(valid);
    }

    @Test
    public void isValidShouldReturnFalseWithPromptNoneLogin() {

        //Given
        final OpenIDPromptParameter prompt = new OpenIDPromptParameter("none login");

        //When
        final boolean valid = prompt.isValid();

        //Then
        assertFalse(valid);
    }

    @Test
    public void isValidShouldReturnFalseWithPromptNoneLoginConsent() {

        //Given
        final OpenIDPromptParameter prompt = new OpenIDPromptParameter("none login consent");

        //When
        final boolean valid = prompt.isValid();

        //Then
        assertFalse(valid);
    }

    @Test
    public void isValidShouldReturnTrueWithPromptNone() {

        //Given
        final OpenIDPromptParameter prompt = new OpenIDPromptParameter("none");

        //When
        final boolean valid = prompt.isValid();
        final boolean noPrompts = prompt.noPrompts();
        final boolean promptConsent = prompt.promptConsent();
        final boolean promptLogin = prompt.promptLogin();

        //Then
        assertTrue(valid);
        assertTrue(noPrompts);
        assertFalse(promptConsent);
        assertFalse(promptLogin);
    }

    @Test
    public void isValidShouldReturnTrueWithPromptLogin() {

        //Given
        final OpenIDPromptParameter prompt = new OpenIDPromptParameter("login");

        //When
        final boolean valid = prompt.isValid();
        final boolean noPrompts = prompt.noPrompts();
        final boolean promptConsent = prompt.promptConsent();
        final boolean promptLogin = prompt.promptLogin();

        //Then
        assertTrue(valid);
        assertFalse(noPrompts);
        assertFalse(promptConsent);
        assertTrue(promptLogin);
    }

    @Test
    public void isValidShouldReturnTrueWithPromptConsent() {

        //Given
        final OpenIDPromptParameter prompt = new OpenIDPromptParameter("consent");

        //When
        final boolean valid = prompt.isValid();
        final boolean noPrompts = prompt.noPrompts();
        final boolean promptConsent = prompt.promptConsent();
        final boolean promptLogin = prompt.promptLogin();

        //Then
        assertTrue(valid);
        assertFalse(noPrompts);
        assertTrue(promptConsent);
        assertFalse(promptLogin);
    }

    @Test
    public void isValidShouldReturnTrueWithPromptConsentLogin() {

        //Given
        final OpenIDPromptParameter prompt = new OpenIDPromptParameter("consent login");

        //When
        final boolean valid = prompt.isValid();
        final boolean noPrompts = prompt.noPrompts();
        final boolean promptConsent = prompt.promptConsent();
        final boolean promptLogin = prompt.promptLogin();

        //Then
        assertTrue(valid);
        assertFalse(noPrompts);
        assertTrue(promptConsent);
        assertTrue(promptLogin);
    }
}
