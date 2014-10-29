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

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @since 12.0.0
 */
public class OpenIdConnectAuthorizeRequestValidatorTest {

    private OpenIdConnectAuthorizeRequestValidator requestValidator;
    private ClientRegistration clientRegistration;

    @BeforeMethod
    public void setUp() throws InvalidClientException {
        ClientRegistrationStore clientRegistrationStore = mock(ClientRegistrationStore.class);
        clientRegistration = mock(ClientRegistration.class);
        given(clientRegistrationStore.get(anyString(), Matchers.<OAuth2Request>anyObject())).willReturn(clientRegistration);
        requestValidator = new OpenIdConnectAuthorizeRequestValidator(clientRegistrationStore);
    }

    @Test
    public void validateShouldPassForRequestWithOpenidScopeOnOidcClient() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(clientRegistration.getAllowedScopes()).willReturn(Collections.singleton("openid"));

        given(request.getParameter("client_id")).willReturn("CLIENT_ID");
        given(request.getParameter("scope")).willReturn("openid");

        //When
        requestValidator.validateRequest(request);
    }

    @Test (expectedExceptions = InvalidRequestException.class)
    public void validateShouldFailForRequestWithNoOpenidScopeOnOidcClient() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(clientRegistration.getAllowedScopes()).willReturn(Collections.singleton("openid"));

        given(request.getParameter("client_id")).willReturn("CLIENT_ID");
        given(request.getParameter("scope")).willReturn("nothing");

        //When
        requestValidator.validateRequest(request);
    }

    @Test
    public void validateShouldFailWithInvalidRequestExceptionAndFragmentParameters() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(clientRegistration.getAllowedScopes()).willReturn(Collections.singleton("openid"));

        given(request.getParameter("client_id")).willReturn("CLIENT_ID");
        given(request.getParameter("scope")).willReturn("nothing");
        given(request.getParameter("response_type")).willReturn("id_token");

        //When
        try {
            requestValidator.validateRequest(request);
            fail();
        } catch (InvalidRequestException e) {
            //Then
            assertEquals(e.getParameterLocation(), OAuth2Constants.UrlLocation.FRAGMENT);
        }
    }

    @Test
    public void validateShouldFailWithInvalidRequestExceptionAndQueryParameters() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(clientRegistration.getAllowedScopes()).willReturn(Collections.singleton("openid"));

        given(request.getParameter("client_id")).willReturn("CLIENT_ID");
        given(request.getParameter("scope")).willReturn("nothing");
        given(request.getParameter("response_type")).willReturn("code");

        //When
        try {
            requestValidator.validateRequest(request);
            fail();
        } catch (InvalidRequestException e) {
            //Then
            assertEquals(e.getParameterLocation(), OAuth2Constants.UrlLocation.QUERY);
        }
    }

}
