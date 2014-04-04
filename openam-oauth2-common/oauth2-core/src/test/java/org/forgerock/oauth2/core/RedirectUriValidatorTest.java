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

import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @since 12.0.0
 */
public class RedirectUriValidatorTest {

    private RedirectUriValidator redirectUriValidator;

    @BeforeMethod
    public void setUp() {
        redirectUriValidator = new RedirectUriValidator();
    }

    @Test
    public void validateShouldThrowInvalidRequestExceptionWhenRedirectUriIsNull()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = null;
        final Set<URI> registeredRedirectUris = Collections.emptySet();

        given(clientRegistration.getRedirectUris()).willReturn(registeredRedirectUris);

        //When
        try {
            redirectUriValidator.validate(clientRegistration, redirectUri);
        } catch (InvalidRequestException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter: redirect_uri");
        }
    }

    @Test
    public void validateShouldThrowInvalidRequestExceptionWhenRedirectUriIsEmpty()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = "";
        final Set<URI> registeredRedirectUris = Collections.singleton(URI.create("http://localhost"));

        given(clientRegistration.getRedirectUris()).willReturn(registeredRedirectUris);

        //When
        try {
            redirectUriValidator.validate(clientRegistration, redirectUri);
        } catch (InvalidRequestException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter: redirect_uri");
        }
    }

    @Test
    public void shouldValidateSuccessfullyWhenRedirectUriIsNullAndOnlyOneRegisteredRedirectUri()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = null;
        final Set<URI> registeredRedirectUris = Collections.singleton(URI.create("http://localhost"));

        given(clientRegistration.getRedirectUris()).willReturn(registeredRedirectUris);

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // All good
    }

    @Test
    public void shouldValidateSuccessfullyWhenRedirectUriIsEmptyAndOnlyOneRegisteredRedirectUri()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = "";
        final Set<URI> registeredRedirectUris = Collections.singleton(URI.create("http://localhost"));

        given(clientRegistration.getRedirectUris()).willReturn(registeredRedirectUris);

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // All good
    }

    @Test (expectedExceptions = RedirectUriMismatchException.class)
    public void validateShouldThrowRedirectUriMismatchExceptionWhenRedirectUriContainsAFragment()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = URI.create("http://localhost#fragment").toString();

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then

    }

    @Test (expectedExceptions = RedirectUriMismatchException.class)
    public void validateShouldThrowRedirectUriMismatchExceptionWhenRedirectUriIsNotAbsoulte()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = URI.create("localhost").toString();

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then

    }

    @Test (expectedExceptions = RedirectUriMismatchException.class)
    public void validateShouldThrowRedirectUriMismatchExceptionWhenRedirectUriDoesNotMatch()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = URI.create("http://localhost/").toString();
        final Set<URI> registeredRedirectUris = Collections.singleton(URI.create("http://localhost"));

        given(clientRegistration.getRedirectUris()).willReturn(registeredRedirectUris);

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
    }

    @Test
    public void shouldValidateSuccessfullyWhenRedirectUriMatches() throws InvalidRequestException,
            RedirectUriMismatchException {

        //Given
        final ClientRegistration clientRegistration = mock(ClientRegistration.class);
        final String redirectUri = URI.create("http://localhost").toString();
        final Set<URI> registeredRedirectUris = Collections.singleton(URI.create("http://localhost"));

        given(clientRegistration.getRedirectUris()).willReturn(registeredRedirectUris);

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
    }
}
