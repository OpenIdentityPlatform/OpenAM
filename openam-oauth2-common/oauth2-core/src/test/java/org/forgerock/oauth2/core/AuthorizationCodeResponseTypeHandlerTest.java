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

import org.forgerock.oauth2.core.exceptions.ServerException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @since 12.0.0
 */
public class AuthorizationCodeResponseTypeHandlerTest {

    private AuthorizationCodeResponseTypeHandler responseTypeHandler;

    private TokenStore tokenStore;

    @BeforeMethod
    public void setUp() {

        tokenStore = mock(TokenStore.class);

        responseTypeHandler = new AuthorizationCodeResponseTypeHandler(tokenStore);
    }

    @Test
    public void shouldHandle() throws ServerException {

        //Given
        String tokenType = "TOKEN_TYPE";
        Set<String> scope = new HashSet<String>();
        String resourceOwnerId = "RESOURCE_OWNER_ID";
        String clientId = "CLIENT_ID";
        String redirectUri = "REDIRECT_URI";
        String nonce = "NONCE";
        OAuth2Request request = mock(OAuth2Request.class);
        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);

        given(tokenStore.createAuthorizationCode(scope, resourceOwnerId, clientId, redirectUri, nonce, request))
                .willReturn(authorizationCode);

        //When
        final Map.Entry<String, Token> tokenEntry = responseTypeHandler.handle(tokenType, scope, resourceOwnerId,
                clientId, redirectUri, nonce, request);

        //Then
        assertEquals(tokenEntry.getKey(), "code");
        assertEquals(tokenEntry.getValue(), authorizationCode);
    }

    @Test
    public void shouldGetReturnLocation() {
        assertEquals(responseTypeHandler.getReturnLocation(), OAuth2Constants.UrlLocation.QUERY);
    }
}
