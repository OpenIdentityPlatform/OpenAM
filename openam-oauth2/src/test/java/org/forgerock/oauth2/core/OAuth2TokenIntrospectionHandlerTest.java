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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.ACCESS_TOKEN_TYPE;
import static org.mockito.BDDMockito.given;

import java.util.Collections;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link OAuth2TokenIntrospectionHandler}.
 *
 * @since 14.0.0
 */
public final class OAuth2TokenIntrospectionHandlerTest {

    private OAuth2TokenIntrospectionHandler handler;

    @Mock
    private TokenStore tokenStore;
    @Mock
    private OAuth2UrisFactory uriFactory;
    @Mock
    private OAuth2Request request;
    @Mock
    private AccessToken token;
    @Mock
    private OAuth2Uris oAuth2Uris;


    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new OAuth2TokenIntrospectionHandler(tokenStore, uriFactory);
    }

    @Test
    public void whenCnfIsPresentItIsReturned() throws ServerException, NotFoundException, InvalidGrantException {
        // Given
        given(tokenStore.readAccessToken(request, "abc-def-hij")).willReturn(token);
        given(token.isExpired()).willReturn(false);
        given(token.getClientId()).willReturn("some-client-id");
        given(token.getRealm()).willReturn("/abc");
        given(request.getParameter("realm")).willReturn("/abc");
        given(token.getScope()).willReturn(Collections.singleton("open"));
        given(uriFactory.get(request)).willReturn(oAuth2Uris);

        JsonValue confirmationKey = json(object(field("jwk", object())));
        given(token.getConfirmationKey()).willReturn(confirmationKey);

        // When
        JsonValue json = handler.introspect(request, "some-client-id", ACCESS_TOKEN_TYPE, "abc-def-hij");

        // Then
        assertThat(json).hasObject("/cnf/jwk");
    }

    @Test
    public void whenCnfIsNotPresentItIsNotReturned() throws ServerException, NotFoundException, InvalidGrantException {
        // Given
        given(tokenStore.readAccessToken(request, "abc-def-hij")).willReturn(token);
        given(token.isExpired()).willReturn(false);
        given(token.getClientId()).willReturn("some-client-id");
        given(token.getRealm()).willReturn("/abc");
        given(request.getParameter("realm")).willReturn("/abc");
        given(token.getScope()).willReturn(Collections.singleton("open"));
        given(uriFactory.get(request)).willReturn(oAuth2Uris);

        JsonValue confirmationKey = json(null);
        given(token.getConfirmationKey()).willReturn(confirmationKey);

        // When
        JsonValue json = handler.introspect(request, "some-client-id", ACCESS_TOKEN_TYPE, "abc-def-hij");

        // Then
        assertThat(json.isDefined("/cnf")).isFalse();
    }

}