/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.cts.impl;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.exceptions.LDAPOperationFailedException;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


/**
 * Unit tests for the {@link MonitoredCoreTokenAdapter}. Extends from the CoreTokenAdapterTest to ensure the decorator
 * behaves identically to the CoreTokenAdapter but with added monitoring tests.
 */
public class MonitoredCoreTokenAdapterTest extends CoreTokenAdapterTest {

    private CTSOperationsMonitoringStore monitoringStore;

    @Override
    protected CoreTokenAdapter getTestObject() {
        this.monitoringStore = mock(CTSOperationsMonitoringStore.class);
        return new MonitoredCoreTokenAdapter(mockConnectionFactory,
                mockQueryFactory,
                mockLDAPAdapter,
                mockObserver,
                mockListener,
                mockDebug,
                monitoringStore);
    }

    // Test that monitoring calls are made at the appropriate times.

    @Test
    public void shouldRecordOperationForMonitoringOnCreateSuccess() throws CoreTokenException {
        // Given
        Token token = new Token("badger", TokenType.SESSION);

        // When
        adapter.create(token);

        // Then
        verify(monitoringStore).addTokenOperation(token, CTSOperation.CREATE, true);
    }

    @Test(expectedExceptions = TestException.class)
    public void shouldRecordOperationForMonitoringOnCreateFailure() throws CoreTokenException, ErrorResultException {
        // Given
        Token token = new Token("bodger", TokenType.SESSION);
        doThrow(new TestException()).when(mockLDAPAdapter).create(any(Connection.class), eq(token));

        // When
        try { adapter.create(token); }
        finally {
            // Then
            verify(monitoringStore).addTokenOperation(token, CTSOperation.CREATE, false);
        }
    }

    @Test
    public void shouldRecordOperationForMonitoringOnReadSuccess() throws CoreTokenException, ErrorResultException {
        // Given
        String tokenId = "my token";
        Token token = new Token(tokenId, TokenType.OAUTH);
        given(mockLDAPAdapter.read(any(Connection.class), eq(tokenId))).willReturn(token);

        // When
        adapter.read(tokenId);

        // Then
        verify(monitoringStore).addTokenOperation(token, CTSOperation.READ, true);
    }

    @Test
    public void shouldRecordOperationForMonitoringOnReadSuccessWithNoResult() throws CoreTokenException, ErrorResultException {
        // Given
        String tokenId = "my token";
        given(mockLDAPAdapter.read(any(Connection.class), eq(tokenId))).willReturn(null);

        // When
        adapter.read(tokenId);

        // Then
        // Should be recorded as a successful operation even if no result - system worked correctly.
        verify(monitoringStore).addTokenOperation(null, CTSOperation.READ, true);
    }

    @Test(expectedExceptions = TestException.class)
    public void shouldRecordOperationForMonitoringOnReadFailure() throws CoreTokenException, ErrorResultException {
        // Given
        String tokenId = "my token";
        given(mockLDAPAdapter.read(any(Connection.class), eq(tokenId))).willThrow(new TestException());

        // When
        try {
            adapter.read(tokenId);
        } finally {

            // Then
            verify(monitoringStore).addTokenOperation(null, CTSOperation.READ, false);
        }
    }

    @Test
    public void shouldRecordOperationForMonitoringOnUpdateCreateSuccess() throws CoreTokenException, ErrorResultException {
        // Given
        Token token = new Token("flibble", TokenType.REST);
        given(mockLDAPAdapter.read(any(Connection.class), eq(token.getTokenId()))).willReturn(null);

        // When
        adapter.updateOrCreate(token);

        // Then
        verify(monitoringStore).addTokenOperation(token, CTSOperation.CREATE, true);
    }

    @Test
    public void shouldRecordOperationForMonitoringOnUpdateSuccess() throws CoreTokenException, ErrorResultException {
        // Given
        Token token = new Token("bobby", TokenType.SAML2);
        given(mockLDAPAdapter.read(any(Connection.class), eq(token.getTokenId()))).willReturn(token);

        // When
        adapter.updateOrCreate(token);

        // Then
        verify(monitoringStore).addTokenOperation(token, CTSOperation.UPDATE, true);
    }

    @Test(expectedExceptions = TestException.class)
    public void shouldRecordOperationForMonitoringOnUpdateFailure() throws CoreTokenException, ErrorResultException {
        // Given
        Token token = new Token("glooop", TokenType.SAML2);
        given(mockLDAPAdapter.read(any(Connection.class), eq(token.getTokenId()))).willThrow(new TestException());

        // When
        try {
            adapter.updateOrCreate(token);
        } finally {
            // Then
            verify(monitoringStore).addTokenOperation(token, CTSOperation.UPDATE, false);
        }
    }

    @Test
    public void shouldRecordOperationForMonitoringOnDeleteSuccess() throws DeleteFailedException {
        // Given
        String tokenId = "my first token";

        // When
        adapter.delete(tokenId);

        // Then
        verify(monitoringStore).addTokenOperation(null, CTSOperation.DELETE, true);
    }

    @Test(expectedExceptions = TestException.class)
    public void shouldRecordOperationForMonitoringOnDeleteFailure() throws DeleteFailedException, LDAPOperationFailedException, ErrorResultException {
        // Given
        String tokenId = "bad token";
        doThrow(new TestException()).when(mockLDAPAdapter).delete(any(Connection.class), eq(tokenId));

        try {
            // When
            adapter.delete(tokenId);
        } finally {
            // Then
            verify(monitoringStore).addTokenOperation(null, CTSOperation.DELETE, false);
        }
    }

    static class TestException extends RuntimeException {}
}
