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

package org.forgerock.oauth2.restlet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.Mockito.*;

public class RestletFormBodyAccessTokenVerifierTest {
    private RestletFormBodyAccessTokenVerifier verifier = new RestletFormBodyAccessTokenVerifier();
    private TokenStore tokenStore;

    @BeforeMethod
    public void setup() throws Exception {
        tokenStore = mock(TokenStore.class);
        verifier.setTokenStore(tokenStore);
    }

    @Test
    public void shouldCheckBody() throws Exception {
        // Given
        Request request = new Request();
        OAuth2Request req = new RestletOAuth2Request(request);

        // When
        AccessTokenVerifier.TokenState result = verifier.verify(req);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    public void shouldCheckBodyType() throws Exception {
        // Given
        Request request = new Request();
        request.setEntity(new EmptyRepresentation());
        OAuth2Request req = new RestletOAuth2Request(request);

        // When
        AccessTokenVerifier.TokenState result = verifier.verify(req);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    public void shouldCheckParameter() throws Exception {
        // Given
        Form form = new Form();
        Request request = new Request();
        request.setEntity(form.getWebRepresentation());
        OAuth2Request req = new RestletOAuth2Request(request);

        // When
        AccessTokenVerifier.TokenState result = verifier.verify(req);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    public void shouldLookupValue() throws Exception {
        // Given
        Form form = new Form();
        form.add("access_token", "freddy");
        Request request = new Request();
        request.setEntity(form.getWebRepresentation());
        OAuth2Request req = new RestletOAuth2Request(request);

        // When
        AccessTokenVerifier.TokenState result = verifier.verify(req);

        // Then
        assertThat(result.isValid()).isFalse();
        verify(tokenStore).readAccessToken(req, "freddy");
    }

    @Test
    public void shouldCheckExpired() throws Exception {
        // Given
        Form form = new Form();
        form.add("access_token", "freddy");
        Request request = new Request();
        request.setEntity(form.getWebRepresentation());
        OAuth2Request req = new RestletOAuth2Request(request);

        AccessToken token = new AccessToken(json(object()), "access_token", "freddy") {
            @Override
            public boolean isExpired() {
                return true;
            }
        };
        when(tokenStore.readAccessToken(req, "freddy")).thenReturn(token);

        // When
        AccessTokenVerifier.TokenState result = verifier.verify(req);

        // Then
        assertThat(result.isValid()).isFalse();
        verify(tokenStore).readAccessToken(req, "freddy");
    }

    @Test
    public void shouldCheckValid() throws Exception {
        // Given
        Form form = new Form();
        form.add("access_token", "freddy");
        Request request = new Request();
        request.setEntity(form.getWebRepresentation());
        OAuth2Request req = new RestletOAuth2Request(request);

        AccessToken token = new AccessToken(json(object()), "access_token", "freddy") {
            @Override
            public boolean isExpired() {
                return false;
            }
        };
        when(tokenStore.readAccessToken(req, "freddy")).thenReturn(token);

        // When
        AccessTokenVerifier.TokenState result = verifier.verify(req);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getTokenId()).isEqualTo("freddy");
        verify(tokenStore).readAccessToken(req, "freddy");
    }

}