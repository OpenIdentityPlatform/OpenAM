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

import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @since 12.0.0
 */
public class AuthorizationCodeRequestValidatorImplTest {

    private AuthorizationCodeRequestValidatorImpl requestValidator;

    private RedirectUriValidator redirectUriValidator;

    @BeforeMethod
    public void setUp() {
        redirectUriValidator = mock(RedirectUriValidator.class);

        requestValidator = new AuthorizationCodeRequestValidatorImpl(redirectUriValidator);
    }

    @Test
    public void shouldValidateValidRequest() throws RedirectUriMismatchException, InvalidClientException,
            InvalidRequestException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("code")).willReturn("CODE");
        given(request.getParameter("redirect_uri")).willReturn("REDIRECT_URI");

        //When
        requestValidator.validateRequest(request, clientRegistration);

        //Then
        // Expect no exceptions
        verify(redirectUriValidator).validate(Matchers.<ClientRegistration>anyObject(), anyString());
    }

    @Test
    public void shouldValidateRequestWithEmptyClientId() throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("code")).willReturn("");

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'code'");
        }
    }

    @Test
    public void shouldValidateRequestWithMissingClientId() throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'code'");
        }
    }

    @Test
    public void shouldValidateRequestWithEmptyResponseType() throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("code")).willReturn("CODE");
        given(request.getParameter("redirect_uri")).willReturn("");

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'redirect_uri'");
        }
    }

    @Test
    public void shouldValidateRequestWithMissingResponseType() throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("code")).willReturn("CODE");

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'redirect_uri'");
        }
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void shouldThrowInvalidRequestExceptionWhenScopeRequested() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("scope")).willReturn("fred");
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        //When
        requestValidator.validateRequest(request, clientRegistration);

        //Then
        // Expect InvalidRequestException
    }

}
