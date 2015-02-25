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
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

/**
 * @since 12.0.0
 */
public class ResponseTypeValidatorTest {

    private ResponseTypeValidator responseTypeValidator;

    @BeforeMethod
    public void setUp() {
        responseTypeValidator = new ResponseTypeValidator();
    }

    @Test (expectedExceptions = UnsupportedResponseTypeException.class)
    public void validateShouldThrowUnsupportedResponseTypeExceptionWhenRequestedResponseTypesAreEmpty()
            throws ServerException, UnsupportedResponseTypeException, InvalidRequestException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> requestedRequestTypes = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);

        //When
        responseTypeValidator.validate(clientRegistration, requestedRequestTypes, providerSettings);

        //Then
        // Expect UnsupportedResponseTypeException
    }

    @Test (expectedExceptions = UnsupportedResponseTypeException.class)
    public void validateShouldThrowUnsupportedResponseTypeExceptionWhenRequestedResponseTypesAreNull()
            throws ServerException, UnsupportedResponseTypeException, InvalidRequestException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> requestedRequestTypes = null;
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);

        //When
        responseTypeValidator.validate(clientRegistration, requestedRequestTypes, providerSettings);

        //Then
        // Expect UnsupportedResponseTypeException
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void validateShouldThrowInvalidRequestExceptionWhenProviderAllowedRequestedResponseTypesAreEmpty()
            throws ServerException, UnsupportedResponseTypeException, InvalidRequestException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> requestedRequestTypes = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        Map<String, ResponseTypeHandler> providerResponseTypes = new HashMap<String, ResponseTypeHandler>();

        requestedRequestTypes.add("RESPONSE_TYPE_A");
        given(providerSettings.getAllowedResponseTypes()).willReturn(providerResponseTypes);

        //When
        responseTypeValidator.validate(clientRegistration, requestedRequestTypes, providerSettings);

        //Then
        // Expect InvalidRequestException
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void validateShouldThrowInvalidRequestExceptionWhenProviderAllowedRequestedResponseTypesAreNull()
            throws ServerException, UnsupportedResponseTypeException, InvalidRequestException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> requestedRequestTypes = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        Map<String, ResponseTypeHandler> providerResponseTypes = null;

        requestedRequestTypes.add("RESPONSE_TYPE_A");
        given(providerSettings.getAllowedResponseTypes()).willReturn(providerResponseTypes);

        //When
        responseTypeValidator.validate(clientRegistration, requestedRequestTypes, providerSettings);

        //Then
        // Expect InvalidRequestException
    }

    @Test (expectedExceptions = UnsupportedResponseTypeException.class)
    public void validateShouldThrowUnsupportedResponseTypeExceptionWhenNotAllResponseTypesAreAllowedByProvider()
            throws ServerException, UnsupportedResponseTypeException, InvalidRequestException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> requestedRequestTypes = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        Map<String, ResponseTypeHandler> providerResponseTypes = new HashMap<String, ResponseTypeHandler>();

        requestedRequestTypes.add("RESPONSE_TYPE_A");
        requestedRequestTypes.add("RESPONSE_TYPE_B");
        given(providerSettings.getAllowedResponseTypes()).willReturn(providerResponseTypes);
        providerResponseTypes.put("RESPONSE_TYPE_A", null);

        //When
        responseTypeValidator.validate(clientRegistration, requestedRequestTypes, providerSettings);

        //Then
        // Expect UnsupportedResponseTypeException
    }

    @Test (expectedExceptions = UnsupportedResponseTypeException.class)
    public void validateShouldThrowUnsupportedResponseTypeExceptionWhenNotAllResponseTypesAreAllowedByClient()
            throws ServerException, UnsupportedResponseTypeException, InvalidRequestException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> requestedRequestTypes = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        Map<String, ResponseTypeHandler> providerResponseTypes = new HashMap<String, ResponseTypeHandler>();
        Set<String> clientAllowedResponseTypes = new HashSet<String>();

        requestedRequestTypes.add("RESPONSE_TYPE_A");
        requestedRequestTypes.add("RESPONSE_TYPE_B");
        given(providerSettings.getAllowedResponseTypes()).willReturn(providerResponseTypes);
        providerResponseTypes.put("RESPONSE_TYPE_A", null);
        providerResponseTypes.put("RESPONSE_TYPE_B", null);
        given(clientRegistration.getAllowedResponseTypes()).willReturn(clientAllowedResponseTypes);
        clientAllowedResponseTypes.add("RESPONSE_TYPE_A");

        //When
        responseTypeValidator.validate(clientRegistration, requestedRequestTypes, providerSettings);

        //Then
        // Expect UnsupportedResponseTypeException
    }

    @Test
    public void validateShouldValidateResponseType() throws ServerException, UnsupportedResponseTypeException,
            InvalidRequestException {

        //Given
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> requestedRequestTypes = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        Map<String, ResponseTypeHandler> providerResponseTypes = new HashMap<String, ResponseTypeHandler>();
        Set<String> clientAllowedResponseTypes = new HashSet<String>();

        requestedRequestTypes.add("RESPONSE_TYPE_A");
        requestedRequestTypes.add("RESPONSE_TYPE_B");
        given(providerSettings.getAllowedResponseTypes()).willReturn(providerResponseTypes);
        providerResponseTypes.put("RESPONSE_TYPE_A", null);
        providerResponseTypes.put("RESPONSE_TYPE_B", null);
        given(clientRegistration.getAllowedResponseTypes()).willReturn(clientAllowedResponseTypes);
        clientAllowedResponseTypes.add("RESPONSE_TYPE_A");
        clientAllowedResponseTypes.add("RESPONSE_TYPE_B");

        //When
        responseTypeValidator.validate(clientRegistration, requestedRequestTypes, providerSettings);

        //Then
        // Expect no exceptions
    }
}
