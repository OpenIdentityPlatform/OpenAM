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

package org.forgerock.openam.oauth2;

import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.Mockito.*;

public class OpenAMScopeValidatorTest {

    private OpenAMScopeValidator validator;
    private OAuth2Request request;
    private OAuth2ProviderSettings providerSettings;
    private ClientRegistration client;

    @BeforeMethod
    public void setup() throws Exception {
        client = mock(ClientRegistration.class);
        request = mock(OAuth2Request.class);
        providerSettings = mock(OAuth2ProviderSettings.class);
        OAuth2ProviderSettingsFactory factory = mock(OAuth2ProviderSettingsFactory.class);
        when(factory.get(request)).thenReturn(providerSettings);
        this.validator = new OpenAMScopeValidator(null, null, factory);
    }

    @Test
    public void shouldReturnValidAuthorizationScopes() throws Exception {
        // Given
        when(client.getAllowedScopes()).thenReturn(asSet("a", "b", "c"));

        // When
        Set<String> scopes = validator.validateAuthorizationScope(client, asSet("a", "b"), request);

        // Then
        assertThat(scopes).containsOnly("a", "b");
    }

    @Test
    public void shouldReturnValidAccessTokenScopes() throws Exception {
        // Given
        when(client.getAllowedScopes()).thenReturn(asSet("a", "b", "c"));

        // When
        Set<String> scopes = validator.validateAccessTokenScope(client, asSet("a", "b"), request);

        // Then
        assertThat(scopes).containsOnly("a", "b");
    }

    @Test
    public void shouldReturnValidRefreshTokenScopes() throws Exception {
        // Given
        when(client.getAllowedScopes()).thenReturn(asSet("x", "y", "z"));

        // When
        Set<String> scopes = validator.validateRefreshTokenScope(client, asSet("a", "b"), asSet("a", "b", "c"), request);

        // Then
        assertThat(scopes).containsOnly("a", "b");
    }

    @Test
    public void shouldReturnDefaultScopesWhenNoneRequested() throws Exception {
        // Given
        when(client.getDefaultScopes()).thenReturn(asSet("a", "b"));

        // When
        Set<String> scopes = validator.validateAuthorizationScope(client, new HashSet<String>(), request);

        // Then
        assertThat(scopes).containsOnly("a", "b");
    }

    @Test(expectedExceptions = InvalidScopeException.class)
    public void shouldThrowExceptionForUnknownScopes() throws Exception {
        // Given
        when(client.getAllowedScopes()).thenReturn(asSet("a", "b", "c"));

        // When
        validator.validateAuthorizationScope(client, asSet("a", "b", "d"), request);
    }

    @Test(expectedExceptions = InvalidScopeException.class)
    public void shouldThrowExceptionForNoScopes() throws Exception {
        // Given
        when(client.getAllowedScopes()).thenReturn(asSet("a", "b", "c"));

        // When
        validator.validateAuthorizationScope(client, Collections.<String>emptySet(), request);
    }
}