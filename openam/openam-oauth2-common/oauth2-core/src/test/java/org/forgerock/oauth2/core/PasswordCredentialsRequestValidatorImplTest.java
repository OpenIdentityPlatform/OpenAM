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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

/**
 * @since 12.0.0
 */
public class PasswordCredentialsRequestValidatorImplTest {

    private PasswordCredentialsRequestValidatorImpl requestValidator;

    @BeforeMethod
    public void setUp() {
        requestValidator = new PasswordCredentialsRequestValidatorImpl();
    }

    @Test
    public void shouldValidateValidRequest() {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("username")).willReturn("USERNAME");
        given(request.getParameter("password")).willReturn("PASSWORD");

        //When
        requestValidator.validateRequest(request, clientRegistration);

        //Then
        // Expect no exceptions
    }

    @Test
    public void shouldValidateRequestWithEmptyUsername() {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("username")).willReturn("");

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'username'");
        }
    }

    @Test
    public void shouldValidateRequestWithMissingUsername() {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'username'");
        }
    }

    @Test
    public void shouldValidateRequestWithEmptyPassword() {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("username")).willReturn("USERNAME");
        given(request.getParameter("password")).willReturn("");

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'password'");
        }
    }

    @Test
    public void shouldValidateRequestWithMissingPassword() {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(request.getParameter("username")).willReturn("USERNAME");

        try {
            //When
            requestValidator.validateRequest(request, clientRegistration);
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'password'");
        }
    }
}
