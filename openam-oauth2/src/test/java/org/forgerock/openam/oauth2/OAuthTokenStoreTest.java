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
 * Copyright 2026 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.utils.KeyConversion;
import org.forgerock.openam.tokens.TokenType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OAuthTokenStoreTest {

    private static final String TOKEN_ID = "legacy-token-id";

    private CTSPersistentStore cts;
    private TokenAdapter<JsonValue> tokenAdapter;
    private TokenIdFactory tokenIdFactory;
    private OAuthTokenStore tokenStore;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setUp() {
        cts = mock(CTSPersistentStore.class);
        tokenAdapter = mock(TokenAdapter.class);
        tokenIdFactory = new TokenIdFactory(new KeyConversion());
        tokenStore = new OAuthTokenStore(cts, tokenIdFactory, tokenAdapter, mock(OAuth2AuditLogger.class),
                mock(Debug.class));
    }

    @Test
    public void shouldReadLegacyOAuthTokenWhenNamespacedTokenDoesNotExist() throws Exception {
        // Given
        Token legacyToken = new Token(TOKEN_ID, TokenType.OAUTH);
        JsonValue expected = new JsonValue(singletonMap("id", TOKEN_ID));
        given(cts.read(tokenIdFactory.toOAuthTokenStoreId(TOKEN_ID))).willReturn(null);
        given(cts.read(TOKEN_ID)).willReturn(legacyToken);
        given(tokenAdapter.fromToken(legacyToken)).willReturn(expected);

        // When
        JsonValue result = tokenStore.read(TOKEN_ID);

        // Then
        assertEquals(result, expected);
    }

    @Test
    public void shouldNotReadLegacyNonOAuthToken() throws Exception {
        // Given
        Token legacyToken = new Token(TOKEN_ID, TokenType.PUSH);
        given(cts.read(tokenIdFactory.toOAuthTokenStoreId(TOKEN_ID))).willReturn(null);
        given(cts.read(TOKEN_ID)).willReturn(legacyToken);

        // When
        JsonValue result = tokenStore.read(TOKEN_ID);

        // Then
        assertNull(result);
        verify(tokenAdapter, never()).fromToken(legacyToken);
    }

    @Test
    public void shouldNotFallbackToLegacyWhenNamespacedTokenIsNonOAuth() throws Exception {
        // Given
        Token namespacedToken = new Token(tokenIdFactory.toOAuthTokenStoreId(TOKEN_ID), TokenType.PUSH);
        Token legacyToken = new Token(TOKEN_ID, TokenType.OAUTH);
        given(cts.read(tokenIdFactory.toOAuthTokenStoreId(TOKEN_ID))).willReturn(namespacedToken);
        given(cts.read(TOKEN_ID)).willReturn(legacyToken);

        // When
        JsonValue result = tokenStore.read(TOKEN_ID);

        // Then
        assertNull(result);
        verify(tokenAdapter, never()).fromToken(namespacedToken);
        verify(tokenAdapter, never()).fromToken(legacyToken);
    }

    @Test
    public void shouldDeleteLegacyOAuthTokenWhenNamespacedTokenDoesNotExist() throws Exception {
        // Given
        Token legacyToken = new Token(TOKEN_ID, TokenType.OAUTH);
        String tokenStoreId = tokenIdFactory.toOAuthTokenStoreId(TOKEN_ID);
        given(cts.read(tokenStoreId)).willReturn(null);
        given(cts.read(TOKEN_ID)).willReturn(legacyToken);

        // When
        tokenStore.delete(TOKEN_ID);

        // Then
        verify(cts).delete(TOKEN_ID);
        verify(cts, never()).delete(tokenStoreId);
    }

    @Test
    public void shouldNotDeleteLegacyNonOAuthToken() throws Exception {
        // Given
        Token legacyToken = new Token(TOKEN_ID, TokenType.PUSH);
        String tokenStoreId = tokenIdFactory.toOAuthTokenStoreId(TOKEN_ID);
        given(cts.read(tokenStoreId)).willReturn(null);
        given(cts.read(TOKEN_ID)).willReturn(legacyToken);

        // When
        tokenStore.delete(TOKEN_ID);

        // Then
        verify(cts, never()).delete(TOKEN_ID);
        verify(cts, never()).delete(tokenStoreId);
    }
}
