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
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @since 12.0.0
 */
public class AuthorizationTokenIssuerTest {

    private AuthorizationTokenIssuer tokenIssuer;

    @BeforeMethod
    public void setUp() {
        tokenIssuer = new AuthorizationTokenIssuer();
    }

    @Test (expectedExceptions = UnsupportedResponseTypeException.class)
    public void issueTokensShouldThrowUnsupportedResponseTypeExceptionIfResponseTypeMissing() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        Set<String> authorizationScope = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);

        //When
        tokenIssuer.issueTokens(request, clientRegistration, resourceOwner, authorizationScope, providerSettings);

        //Then
        // Expect UnsupportedResponseTypeException
    }

    @Test (expectedExceptions = UnsupportedResponseTypeException.class)
    public void issueTokensShouldThrowUnsupportedResponseTypeExceptionIfResponseTypeIsEmpty() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        Set<String> authorizationScope = new HashSet<String>();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);

        given(request.getParameter("response_type")).willReturn("");

        //When
        tokenIssuer.issueTokens(request, clientRegistration, resourceOwner, authorizationScope, providerSettings);

        //Then
        // Expect UnsupportedResponseTypeException
    }
}
