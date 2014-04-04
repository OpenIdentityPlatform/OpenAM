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

import org.testng.annotations.Test;

import java.util.Collections;

import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.REFRESH_TOKEN;
import static org.forgerock.oauth2.core.RefreshTokenRequest.RefreshTokenRequestBuilder;
import static org.forgerock.oauth2.core.RefreshTokenRequest.createRefreshTokenRequest;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @since 12.0.0
 */
public class RefreshTokenRequestTest {

    @Test
    public void shouldFailToCreateRefreshTokenRequestWhenRefreshTokenNotSet() {

        //Given
        final RefreshTokenRequestBuilder builder = createRefreshTokenRequest();

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'refresh_token'");
        }
    }

    @Test
    public void shouldFailToCreateRefreshTokenRequestWhenRefreshTokenEmpty() {

        //Given
        final RefreshTokenRequestBuilder builder = createRefreshTokenRequest();

        builder.refreshToken("");

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'refresh_token'");
        }
    }

    @Test
    public void shouldCreateRefreshTokenRequestWhenAllRequiredParametersSet() {

        //Given
        final RefreshTokenRequestBuilder builder = createRefreshTokenRequest();

        builder.refreshToken("REFRESH_TOKEN");

        //When
        final RefreshTokenRequest refreshTokenRequest = builder.build();

        //Then
        assertNotNull(refreshTokenRequest);
    }

    @Test
    public void shouldCreateRefreshTokenRequestWhenAllParametersSet() {

        //Given
        final RefreshTokenRequestBuilder builder = createRefreshTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.refreshToken("REFRESH_TOKEN");
        builder.clientCredentials(clientCredentials);
        builder.scope("SCOPE");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final RefreshTokenRequest refreshTokenRequest = builder.build();

        //Then
        assertNotNull(refreshTokenRequest);
    }

    @Test
    public void shouldGetSetAuthorizationCodeAccessTokenRequestParameters() {

        //Given
        final RefreshTokenRequestBuilder builder = createRefreshTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.refreshToken("REFRESH_TOKEN");
        builder.clientCredentials(clientCredentials);
        builder.scope("SCOPE");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final RefreshTokenRequest refreshTokenRequest = builder.build();

        //Then
        assertEquals(refreshTokenRequest.getGrantType(), REFRESH_TOKEN);
        assertEquals(refreshTokenRequest.getClientCredentials(), clientCredentials);
        assertEquals(refreshTokenRequest.getRefreshToken(), "REFRESH_TOKEN");
        assertEquals(refreshTokenRequest.getScope(), Collections.singleton("SCOPE"));
        assertEquals(refreshTokenRequest.getContext(), Collections.<String, Object>emptyMap());
    }
}
