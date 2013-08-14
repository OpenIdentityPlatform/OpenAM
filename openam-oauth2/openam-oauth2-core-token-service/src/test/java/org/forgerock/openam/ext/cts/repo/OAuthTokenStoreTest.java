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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.ext.cts.repo;

import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.ldap.adapters.TokenAdapter;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.opendj.ldap.Filter;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OAuthTokenStoreTest {

    private OAuthTokenStore oAuthTokenStore;

    private CTSPersistentStore cts;
    private TokenAdapter<JsonValue> tokenAdapter;
    private TokenIdFactory tokenIdFactory;

    @BeforeMethod
    public void setUp() {
        cts = mock(CTSPersistentStore.class);
        tokenAdapter = mock(TokenAdapter.class);
        tokenIdFactory = mock(TokenIdFactory.class);

        oAuthTokenStore = new OAuthTokenStore(cts, tokenAdapter, tokenIdFactory);
    }

    @Test
    public void shouldCreate() throws CoreTokenException {

        //Given
        JsonValue oAuthToken = mock(JsonValue.class);
        Token token = mock(Token.class);

        given(tokenAdapter.toToken(oAuthToken)).willReturn(token);

        //When
        oAuthTokenStore.create(oAuthToken);

        //Then
        verify(cts).create(token);
    }

    @Test
    public void shouldRead() throws CoreTokenException {

        //Given
        Token token = mock(Token.class);

        given(tokenIdFactory.getOAuthTokenId("ID")).willReturn("ID2");
        given(cts.read("ID2")).willReturn(token);

        //When
        JsonValue oAuthToken = oAuthTokenStore.read("ID");

        //Then
        verify(tokenAdapter).fromToken(token);
    }

    @Test
    public void shouldUpdate() throws CoreTokenException {

        //Given
        JsonValue oAuthToken = mock(JsonValue.class);
        Token token = mock(Token.class);

        given(tokenAdapter.toToken(oAuthToken)).willReturn(token);

        //When
        oAuthTokenStore.update(oAuthToken);

        //Then
        verify(cts).update(token);
    }

    @Test
    public void shouldDelete() throws CoreTokenException {

        //Given

        //When
        oAuthTokenStore.delete("ID");

        //Then
        verify(cts).delete("ID");
    }

    @Test
    public void shouldQuery() throws CoreTokenException {

        //Given
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        Collection<Token> tokens = new HashSet<Token>();

        given(cts.list(Matchers.<Filter>anyObject())).willReturn(tokens);

        //When
        oAuthTokenStore.query(queryParameters);

        //Then
        verify(cts).list(Matchers.<Filter>anyObject());
    }
}
