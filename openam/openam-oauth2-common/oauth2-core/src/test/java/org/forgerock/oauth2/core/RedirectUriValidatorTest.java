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
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Collections;

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
    public void validateShouldThrowInvalidRequestExceptionWhenRedirectUriIsNullAndNoClientRegistrationRedirectUris()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = null;

        given(clientRegistration.getRedirectUris()).willReturn(Collections.<URI>emptySet());

        try {
            //When
            redirectUriValidator.validate(clientRegistration, redirectUri);
        } catch (InvalidRequestException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter: redirect_uri");
        }
    }

    @Test
    public void validateShouldThrowInvalidRequestExceptionWhenRedirectUriIsEmptyAndNoClientRegistrationRedirectUris()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = "";

        given(clientRegistration.getRedirectUris()).willReturn(Collections.<URI>emptySet());

        try {
            //When
            redirectUriValidator.validate(clientRegistration, redirectUri);
        } catch (InvalidRequestException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter: redirect_uri");
        }
    }

    @Test
    public void validateShouldReturnSuccessfullyWhenRedirectUriIsNullAndSingleClientRegistrationRedirectUri()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = null;

        given(clientRegistration.getRedirectUris())
                .willReturn(Collections.<URI>singleton(URI.create("http://localhost:8080/")));

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // Expect no exceptions
    }

    @Test
    public void validateShouldReturnSuccessfullyWhenRedirectUriIsEmptyAndSingleClientRegistrationRedirectUri()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = "";

        given(clientRegistration.getRedirectUris())
                .willReturn(Collections.<URI>singleton(URI.create("http://localhost:8080/")));

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // Expect no exceptions
    }

    @Test (expectedExceptions = RedirectUriMismatchException.class)
    public void validateShouldThrowRedirectUriMismatchExceptionWhenRedirectUriContainsFragment()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = "http://localhost:8080/#fragment";

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // Expect RedirectUriMismatchException
    }

    @Test (expectedExceptions = RedirectUriMismatchException.class)
    public void validateShouldThrowRedirectUriMismatchExceptionWhenRedirectUriIsNotAbsolute()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = "localhost:8080/";

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // Expect RedirectUriMismatchException
    }

    @Test (expectedExceptions = RedirectUriMismatchException.class)
    public void validateShouldThrowRedirectUriMismatchExceptionWhenNoClientRegistrationRedirectUris()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = "http://localhost:8080/";

        given(clientRegistration.getRedirectUris()).willReturn(Collections.<URI>emptySet());

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // Expect RedirectUriMismatchException
    }

    @Test (expectedExceptions = RedirectUriMismatchException.class)
    public void validateShouldThrowRedirectUriMismatchExceptionWhenNoMatchingClientRegistrationRedirectUri()
            throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = "http://localhost:8080/";

        given(clientRegistration.getRedirectUris())
                .willReturn(Collections.<URI>singleton(URI.create("http://localhost:8080/other")));

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // Expect RedirectUriMismatchException
    }

    @Test
    public void shouldValidateRedirectUri() throws InvalidRequestException, RedirectUriMismatchException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        String redirectUri = "http://localhost:8080/";

        given(clientRegistration.getRedirectUris())
                .willReturn(Collections.<URI>singleton(URI.create("http://localhost:8080/")));

        //When
        redirectUriValidator.validate(clientRegistration, redirectUri);

        //Then
        // Expect no exceptions
    }
}
