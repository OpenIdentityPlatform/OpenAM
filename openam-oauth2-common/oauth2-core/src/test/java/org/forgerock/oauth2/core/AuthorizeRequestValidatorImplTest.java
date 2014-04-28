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

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @since 12.0.0
 */
public class AuthorizeRequestValidatorImplTest {

    private AuthorizeRequestValidatorImpl requestValidator;

    private RedirectUriValidator redirectUriValidator;
    private ResponseTypeValidator responseTypeValidator;

    @BeforeMethod
    public void setUp() {

        ClientRegistrationStore clientRegistrationStore = mock(ClientRegistrationStore.class);
        redirectUriValidator = mock(RedirectUriValidator.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        responseTypeValidator = mock(ResponseTypeValidator.class);

        requestValidator = new AuthorizeRequestValidatorImpl(clientRegistrationStore, redirectUriValidator,
                providerSettingsFactory, responseTypeValidator);

        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(Matchers.<OAuth2Request>anyObject())).willReturn(providerSettings);
    }

    @Test
    public void shouldValidateValidRequest() throws InvalidClientException, UnsupportedResponseTypeException,
            InvalidRequestException, RedirectUriMismatchException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        given(request.getParameter("client_id")).willReturn("CLIENT_ID");
        given(request.getParameter("response_type")).willReturn("RESPONSE_TYPE");

        //When
        requestValidator.validateRequest(request);

        //Then
        // Expect no exceptions
        verify(redirectUriValidator).validate(Matchers.<ClientRegistration>anyObject(), anyString());
        verify(responseTypeValidator).validate(Matchers.<ClientRegistration>anyObject(), anySetOf(String.class),
                Matchers.<OAuth2ProviderSettings>anyObject());
    }

    @Test
    public void shouldValidateRequestWithEmptyClientId() throws InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        given(request.getParameter("client_id")).willReturn("");

        try {
            //When
            requestValidator.validateRequest(request);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldValidateRequestWithMissingClientId() throws InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        try {
            //When
            requestValidator.validateRequest(request);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldValidateRequestWithEmptyResponseType() throws InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        given(request.getParameter("client_id")).willReturn("CLIENT_ID");
        given(request.getParameter("response_type")).willReturn("");

        try {
            //When
            requestValidator.validateRequest(request);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'response_type'");
        }
    }

    @Test
    public void shouldValidateRequestWithMissingResponseType() throws InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);

        given(request.getParameter("client_id")).willReturn("CLIENT_ID");

        try {
            //When
            requestValidator.validateRequest(request);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'response_type'");
        }
    }
}
