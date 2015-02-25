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

import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @since 12.0.0
 */
public class ClientCredentialsRequestValidatorImplTest {

    private ClientCredentialsRequestValidatorImpl requestValidator;

    @BeforeMethod
    public void setUp() {
        requestValidator = new ClientCredentialsRequestValidatorImpl();
    }

    @Test
    public void shouldValidateValidRequest() throws UnauthorizedClientException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(clientRegistration.isConfidential()).willReturn(true);

        //When
        requestValidator.validateRequest(request, clientRegistration);

        //Then
        // Expect no exceptions
    }

    @Test (expectedExceptions = UnauthorizedClientException.class)
    public void shouldValidateRequestWhenClientRegistrationIsConfidential() throws UnauthorizedClientException {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(clientRegistration.isConfidential()).willReturn(false);

        //When
        requestValidator.validateRequest(request, clientRegistration);

        //Then
        // Expect UnauthorizedClientException
    }
}
