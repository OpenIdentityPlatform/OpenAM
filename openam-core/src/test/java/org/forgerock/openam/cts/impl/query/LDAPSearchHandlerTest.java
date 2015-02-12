/**
 * Copyright 2013-2015 ForgeRock AS.
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
package org.forgerock.openam.cts.impl.query;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ErrorResultIOException;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LDAPSearchHandlerTest {

    private LDAPConfig mockConstants;
    private ConnectionFactory mockFactory;
    private Connection mockConnection;
    private SearchRequest mockRequest;
    private LDAPSearchHandler handler;
    private Debug debug;

    @BeforeMethod
    public void setUp() throws Exception {
        mockConstants = mock(LDAPConfig.class);
        mockFactory = mock(ConnectionFactory.class);
        mockConnection = mock(Connection.class);
        mockRequest = mock(SearchRequest.class);
        debug = mock(Debug.class);

        handler = new LDAPSearchHandler(mockFactory, mockConstants, debug);

        given(mockFactory.getConnection()).willReturn(mockConnection);
    }

    @Test
    public void shouldGetConnectionFromFactory() throws QueryFailedException, ErrorResultException, ErrorResultIOException {
        // Given
        given(mockFactory.getConnection()).willReturn(mockConnection);

        // When
        handler.performSearch(mockRequest, mock(Collection.class));

        // Then
        verify(mockFactory).getConnection();
    }

    @Test(expectedExceptions = QueryFailedException.class)
    public void shouldHandleException() throws Exception {
        // Given
        given(mockFactory.getConnection()).willThrow(ErrorResultException.class);

        // When / Then
        handler.performSearch(mockRequest, new ArrayList<Entry>(0));
    }

    @Test
    public void shouldNotFailWithExceptionIfEntriesWerePresent() throws Exception {
        // Given
        given(mockFactory.getConnection()).willReturn(mockConnection);
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(null);
        given(mockConnection.search(mockRequest, entries)).willThrow(ErrorResultException.class);

        // When / Then
        handler.performSearch(mockRequest, new ArrayList<Entry>(0));
    }

    @Test
    public void shouldReturnResult() throws ErrorResultException, QueryFailedException {
        // Given
        Result mockResult = mock(Result.class);
        given(mockConnection.search(eq(mockRequest), any(Collection.class))).willReturn(mockResult);

        // When
        Result result = handler.performSearch(mockRequest, mock(Collection.class));

        // Then
        assertThat(result).isEqualTo(mockResult);
    }
}
