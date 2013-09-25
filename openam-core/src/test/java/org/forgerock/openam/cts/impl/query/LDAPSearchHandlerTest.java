/**
 * Copyright 2013 ForgeRock AS.
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

import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ErrorResultIOException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class LDAPSearchHandlerTest {

    private LDAPConfig mockConstants;
    private ConnectionFactory mockFactory;
    private Connection mockConnection;
    private SearchRequest mockRequest;
    private LDAPSearchHandler handler;

    @BeforeMethod
    public void setUp() throws Exception {
        mockConstants = mock(LDAPConfig.class);
        mockFactory = mock(ConnectionFactory.class);
        mockConnection = mock(Connection.class);
        mockRequest = mock(SearchRequest.class);

        handler = new LDAPSearchHandler(mockFactory, mockConstants);

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
    public void shouldHandleException() throws ErrorResultException, SearchResultReferenceIOException, ErrorResultIOException, QueryFailedException {
        // Given
        given(mockFactory.getConnection()).willThrow(ErrorResultException.class);

        // When / Then
        handler.performSearch(mockRequest, mock(Collection.class));
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
