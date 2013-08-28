/**
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package com.sun.identity.sm.ldap.impl;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.exceptions.DeleteFailedException;
import com.sun.identity.sm.ldap.exceptions.LDAPOperationFailedException;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultCode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenAdapterTest {
    private Connection mockConnection;
    private DataLayerConnectionFactory mockConnectionFactory;
    private QueryFactory mockQueryFactory;
    private Debug mockDebug;
    private LDAPAdapter mockLDAPAdapter;

    @BeforeMethod
    public void setup() {
        mockDebug = mock(Debug.class);
        mockLDAPAdapter = mock(LDAPAdapter.class);
        mockQueryFactory = mock(QueryFactory.class);

        mockConnection = mock(Connection.class);
        mockConnectionFactory = mock(DataLayerConnectionFactory.class);
        try {
            given(mockConnectionFactory.getConnection()).willReturn(mockConnection);
        } catch (ErrorResultException e) {
            throw new IllegalStateException(e);
        }
    }


    @Test
    public void shouldCreateToken() throws CoreTokenException, ErrorResultException {
        // Given
        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        Token token = new Token("badger", TokenType.SESSION);

        // When
        adapter.create(token);

        // Then
        verify(mockLDAPAdapter).create(any(Connection.class), eq(token));
    }

    @Test
    public void shouldReadToken() throws CoreTokenException, ErrorResultException {
        // Given
        String tokenId = "badger";
        Token token = new Token(tokenId, TokenType.SESSION);

        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        given(mockLDAPAdapter.read(any(Connection.class), eq(tokenId))).willReturn(token);

        // When
        Token result = adapter.read(tokenId);

        // Then
        assertThat(result.getTokenId()).isEqualTo(tokenId);
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldFailReadIfOtherExceptionIsEncountered() throws CoreTokenException, ErrorResultException {
        // Given
        String tokenId = "badger";
        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        ErrorResultException exception = ErrorResultException.newErrorResult(ResultCode.ADMIN_LIMIT_EXCEEDED);
        given(mockLDAPAdapter.read(any(Connection.class), anyString())).willThrow(exception);

        // When / Then
        adapter.read(tokenId);
    }

    @Test
    public void shouldReturnNullIfNoTokenFoundWhenRead() throws ErrorResultException, CoreTokenException {
        // Given
        String tokenId = "badger";
        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        ErrorResultException exception = ErrorResultException.newErrorResult(ResultCode.NO_SUCH_OBJECT, "test");
        given(mockLDAPAdapter.read(any(Connection.class), eq(tokenId))).willThrow(exception);

        // When
        Token result = adapter.read(tokenId);

        // Then
        assertNull(result);
    }

    @Test
    public void shouldGenerateAnInstanceOfQueryBuilder() throws ErrorResultException {
        // Given
        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        // When
        adapter.query();

        // Then
        verify(mockQueryFactory).createInstance();
    }

    @Test
    public void shouldPerformUpdate() throws CoreTokenException, ErrorResultException {
        // Given
        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        Token token = new Token("badger", TokenType.SESSION);

        given(mockLDAPAdapter.update(any(Connection.class), any(Token.class), any(Token.class))).willReturn(true);
        given(mockLDAPAdapter.read(any(Connection.class), anyString())).willReturn(mock(Token.class));

        // When
        adapter.updateOrCreate(token);

        // Then
        verify(mockLDAPAdapter).update(any(Connection.class), any(Token.class), eq(token));
    }

    @Test
    public void shouldPerformCreateWhenPreviousMissingDuringUpdate() throws ErrorResultException, CoreTokenException {
        // Given
        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        Token token = new Token("badger", TokenType.SESSION);
        given(mockLDAPAdapter.read(any(Connection.class), anyString())).willReturn(null);

        // When
        adapter.updateOrCreate(token);

        // Then
        verify(mockLDAPAdapter, never()).update(any(Connection.class), any(Token.class), eq(token));
        verify(mockLDAPAdapter).create(any(Connection.class), eq(token));
    }

    @Test
    public void shouldPerformDelete() throws LDAPOperationFailedException, ErrorResultException, DeleteFailedException {
        // Given
        CoreTokenAdapter adapter = new CoreTokenAdapter(
                mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockDebug);

        String tokenId = "badger";

        // When
        adapter.delete(tokenId);

        // Then
        verify(mockLDAPAdapter).delete(any(Connection.class), eq(tokenId));
    }
}
