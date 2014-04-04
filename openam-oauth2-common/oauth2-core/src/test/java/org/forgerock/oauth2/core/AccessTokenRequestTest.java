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

import static org.forgerock.oauth2.core.AccessTokenRequest.AuthorizationCodeAccessTokenRequestBuilder;
import static org.forgerock.oauth2.core.AccessTokenRequest.ClientCredentialsAccessTokenRequestBuilder;
import static org.forgerock.oauth2.core.AccessTokenRequest.PasswordCredentialsAccessTokenRequestBuilder;
import static org.forgerock.oauth2.core.AccessTokenRequest.createAuthorizationCodeAccessTokenRequest;
import static org.forgerock.oauth2.core.AccessTokenRequest.createClientCredentialsAccessTokenRequest;
import static org.forgerock.oauth2.core.AccessTokenRequest.createPasswordAccessTokenRequest;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.AUTHORIZATION_CODE;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.CLIENT_CREDENTIALS;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.PASSWORD;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class AccessTokenRequestTest {

    @Test
    public void shouldFailToCreateAuthorizationCodeAccessTokenRequestWhenClientAuthenticationNotSet() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        //When
        try {
            builder.build();
            fail();
        } catch (NullPointerException e) {
            //Then
            assertEquals(e.getMessage(), "Client Authentication must be set.");
        }
    }

    @Test
    public void shouldFailToCreateAuthorizationCodeAccessTokenRequestWhenClientIdIsNull() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials(null, "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldFailToCreateAuthorizationCodeAccessTokenRequestWhenClientIdIsEmpty() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("", "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldFailToCreateAuthorizationCodeAccessTokenRequestWhenCodeIsNotSet() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'code'");
        }
    }

    @Test
    public void shouldFailToCreateAuthorizationCodeAccessTokenRequestWhenCodeIsEmpty() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.code("");

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'code'");
        }
    }

    @Test
    public void shouldFailToCreateAuthorizationCodeAccessTokenRequestWhenRedirectUriIsNotSet() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.code("CODE");

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'redirect_uri'");
        }
    }

    @Test
    public void shouldFailToCreateAuthorizationCodeAccessTokenRequestWhenRedirectUriIsEmpty() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.code("CODE");
        builder.redirectUri("");

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'redirect_uri'");
        }
    }

    @Test
    public void shouldCreateAuthorizationCodeAccessTokenRequestWhenAllRequiredParametersSet() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.code("CODE");
        builder.redirectUri("REDIRECT_URI");

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertNotNull(accessTokenRequest);
    }

    @Test
    public void shouldCreateAuthorizationCodeAccessTokenRequestWhenAllParametersSet() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.code("CODE");
        builder.redirectUri("REDIRECT_URI");
        builder.clientId("CLIENT_ID");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertNotNull(accessTokenRequest);
    }

    @Test
    public void shouldGetSetAuthorizationCodeAccessTokenRequestParameters() {

        //Given
        final AuthorizationCodeAccessTokenRequestBuilder builder = createAuthorizationCodeAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.code("CODE");
        builder.redirectUri("REDIRECT_URI");
        builder.clientId("CLIENT_ID");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertEquals(accessTokenRequest.getGrantType(), AUTHORIZATION_CODE);
        assertEquals(accessTokenRequest.getCode(), "CODE");
        assertEquals(accessTokenRequest.getRedirectUri(), "REDIRECT_URI");
        assertEquals(accessTokenRequest.getClientId(), "CLIENT_ID");
        assertEquals(accessTokenRequest.getClientCredentials(), clientCredentials);
        assertEquals(accessTokenRequest.getContext(), Collections.<String, Object>emptyMap());
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenClientAuthenticationNotSet() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        //When
        try {
            builder.build();
            fail();
        } catch (NullPointerException e) {
            //Then
            assertEquals(e.getMessage(), "Client Authentication must be set.");
        }
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenClientIdIsNull() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials(null, "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenClientIdIsEmpty() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("", "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenAuthenticationHandlerNotSet() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (NullPointerException e) {
            //Then
            assertEquals(e.getMessage(), "Authentication Handler must be set.");
        }
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenUsernameIsNull() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());
        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler =
                mock(ResourceOwnerPasswordAuthenticationHandler.class);

        builder.clientCredentials(clientCredentials);
        builder.authenticationHandler(authenticationHandler);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'username'");
        }
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenUsernameIsEmpty() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());
        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler =
                mock(ResourceOwnerPasswordAuthenticationHandler.class);

        given(authenticationHandler.getUsername()).willReturn("");

        builder.clientCredentials(clientCredentials);
        builder.authenticationHandler(authenticationHandler);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'username'");
        }
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenPasswordIsNull() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());
        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler =
                mock(ResourceOwnerPasswordAuthenticationHandler.class);

        given(authenticationHandler.getUsername()).willReturn("USER");

        builder.clientCredentials(clientCredentials);
        builder.authenticationHandler(authenticationHandler);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'password'");
        }
    }

    @Test
    public void shouldFailToCreatePasswordCredentialsAccessTokenRequestWhenPasswordIsEmpty() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());
        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler =
                mock(ResourceOwnerPasswordAuthenticationHandler.class);

        given(authenticationHandler.getUsername()).willReturn("USER");
        given(authenticationHandler.getPassword()).willReturn("".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.authenticationHandler(authenticationHandler);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'password'");
        }
    }

    @Test
    public void shouldCreatePasswordCredentialsAccessTokenRequestWhenAllRequiredParametersSet() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());
        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler =
                mock(ResourceOwnerPasswordAuthenticationHandler.class);

        given(authenticationHandler.getUsername()).willReturn("USER");
        given(authenticationHandler.getPassword()).willReturn("PASSWORD".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.authenticationHandler(authenticationHandler);

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertNotNull(accessTokenRequest);
    }

    @Test
    public void shouldCreatePasswordCredentialsAccessTokenRequestWhenAllParametersSet() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());
        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler =
                mock(ResourceOwnerPasswordAuthenticationHandler.class);

        given(authenticationHandler.getUsername()).willReturn("USER");
        given(authenticationHandler.getPassword()).willReturn("PASSWORD".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.authenticationHandler(authenticationHandler);
        builder.scope("SCOPE");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertNotNull(accessTokenRequest);
    }

    @Test
    public void shouldGetSetPasswordCredentialsAccessTokenRequestParameters() {

        //Given
        final PasswordCredentialsAccessTokenRequestBuilder builder = createPasswordAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());
        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler =
                mock(ResourceOwnerPasswordAuthenticationHandler.class);

        given(authenticationHandler.getUsername()).willReturn("USER");
        given(authenticationHandler.getPassword()).willReturn("PASSWORD".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.authenticationHandler(authenticationHandler);
        builder.scope("SCOPE");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertEquals(accessTokenRequest.getGrantType(), PASSWORD);
        assertEquals(accessTokenRequest.getScope(), Collections.singleton("SCOPE"));
        assertEquals(accessTokenRequest.getClientCredentials(), clientCredentials);
        assertEquals(accessTokenRequest.getAuthenticationHandler(), authenticationHandler);
        assertEquals(accessTokenRequest.getContext(), Collections.<String, Object>emptyMap());
    }

    @Test
    public void shouldFailToCreateClientCredentialsAccessTokenRequestWhenClientAuthenticationNotSet() {

        //Given
        final ClientCredentialsAccessTokenRequestBuilder builder = createClientCredentialsAccessTokenRequest();

        //When
        try {
            builder.build();
            fail();
        } catch (NullPointerException e) {
            //Then
            assertEquals(e.getMessage(), "Client Authentication must be set.");
        }
    }

    @Test
    public void shouldFailToCreateClientCredentialsAccessTokenRequestWhenClientIdIsNull() {

        //Given
        final ClientCredentialsAccessTokenRequestBuilder builder = createClientCredentialsAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials(null, "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldFailToCreateClientCredentialsAccessTokenRequestWhenClientIdIsEmpty() {

        //Given
        final ClientCredentialsAccessTokenRequestBuilder builder = createClientCredentialsAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("", "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        try {
            builder.build();
            fail();
        } catch (IllegalArgumentException e) {
            //Then
            assertEquals(e.getMessage(), "Missing parameter, 'client_id'");
        }
    }

    @Test
    public void shouldCreateClientCredentialsAccessTokenRequestWhenAllRequiredParametersSet() {

        //Given
        final ClientCredentialsAccessTokenRequestBuilder builder = createClientCredentialsAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertNotNull(accessTokenRequest);
    }

    @Test
    public void shouldCreateClientCredentialsAccessTokenRequestWhenAllParametersSet() {

        //Given
        final ClientCredentialsAccessTokenRequestBuilder builder = createClientCredentialsAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.scope("SCOPE");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertNotNull(accessTokenRequest);
    }

    @Test
    public void shouldGetSetClientCredentialsAccessTokenRequestParameters() {

        //Given
        final ClientCredentialsAccessTokenRequestBuilder builder = createClientCredentialsAccessTokenRequest();

        final ClientCredentials clientCredentials = new ClientCredentials("CLIENT", "".toCharArray());

        builder.clientCredentials(clientCredentials);
        builder.scope("SCOPE");
        builder.context(Collections.<String, Object>emptyMap());

        //When
        final AccessTokenRequest accessTokenRequest = builder.build();

        //Then
        assertEquals(accessTokenRequest.getGrantType(), CLIENT_CREDENTIALS);
        assertEquals(accessTokenRequest.getScope(), Collections.singleton("SCOPE"));
        assertEquals(accessTokenRequest.getClientCredentials(), clientCredentials);
        assertEquals(accessTokenRequest.getContext(), Collections.<String, Object>emptyMap());
    }
}
