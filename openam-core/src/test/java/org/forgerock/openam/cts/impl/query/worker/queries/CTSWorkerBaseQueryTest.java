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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query.worker.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerRuntimeException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CTSWorkerBaseQueryTest {

    private FakeQuery query;
    private QueryBuilder<Connection, Filter> mockBuilder;
    private ConnectionFactory<Connection> mockConnectionFactory;
    private Connection mockConnection;

    @BeforeMethod
    public void setup() throws Exception {
        mockBuilder = mock(QueryBuilder.class);
        mockConnectionFactory = mock(ConnectionFactory.class);
        mockConnection = mock(Connection.class);
        given(mockConnectionFactory.create()).willReturn(mockConnection);
        query = new FakeQuery(mockConnectionFactory, mockBuilder);
    }

    @AfterMethod
    public void teardown() {
        // Clear interrupt status
        Thread.interrupted();
    }

    @Test
    public void shouldQueryForNextPage() throws CoreTokenException {
        //given
        Iterator<Collection<PartialToken>> mockIterator = mock(Iterator.class);
        given(mockIterator.hasNext()).willReturn(true);
        PartialToken token = partialToken();
        given(mockIterator.next()).willReturn(Arrays.asList(token));
        given(mockBuilder.executeRawResults(mockConnection, PartialToken.class)).willReturn(mockIterator);

        // when
        Collection<PartialToken> page = query.nextPage();

        // then
        verify(mockIterator).hasNext();
        verify(mockIterator).next();
        assertThat(page).containsOnly(token);
    }

    @Test
    public void shouldLoopUntilIteratorEmpty() throws CoreTokenException {
        // Given
        Iterator<Collection<PartialToken>> mockIterator = mock(Iterator.class);
        given(mockBuilder.executeRawResults(mockConnection, PartialToken.class)).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true, true, false);
        PartialToken token1 = partialToken();
        PartialToken token2 = partialToken();

        given(mockIterator.next()).willReturn(Arrays.asList(token1), Arrays.asList(token2));

        // When
        Collection<PartialToken> page1 = query.nextPage();
        Collection<PartialToken> page2 = query.nextPage();
        Collection<PartialToken> page3 = query.nextPage();

        // Then
        assertThat(page1).containsOnly(token1);
        assertThat(page2).containsOnly(token2);
        assertThat(page3).isNull();
        verify(mockIterator, times(2)).next();
    }

    @Test
    public void shouldRespondToInterrupt() throws CoreTokenException {
        Thread.currentThread().interrupt();
        query.nextPage();
        verify(mockBuilder, never()).executeRawResults(mockConnection, PartialToken.class);
    }

    @Test
    public void shouldOpenConnectionOnFirstCall() throws Exception {
        Iterator<Collection<PartialToken>> mockIterator = mock(Iterator.class);
        given(mockBuilder.executeRawResults(mockConnection, PartialToken.class)).willReturn(mockIterator);
        query.nextPage();
        verify(mockConnectionFactory).create();
    }

    @Test
    public void shouldNotOpenNewConnectionOnSubsequentCalls() throws Exception {
        Iterator<Collection<PartialToken>> mockIterator = mock(Iterator.class);
        given(mockIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockBuilder.executeRawResults(mockConnection, PartialToken.class)).willReturn(mockIterator);
        query.nextPage();
        verify(mockConnectionFactory).create();
        query.nextPage();
        verifyNoMoreInteractions(mockConnectionFactory);
    }

    @Test
    public void shouldSupportTryWithResources() throws Exception {
        Iterator<Collection<PartialToken>> mockIterator = mock(Iterator.class);
        given(mockBuilder.executeRawResults(mockConnection, PartialToken.class)).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true);
        given(mockIterator.next()).willThrow(DataLayerRuntimeException.class);
        try (CTSWorkerQuery worker = query) {
            worker.nextPage();
        } catch(DataLayerRuntimeException dlre) {
        }

        verify(mockConnection).close();
    }

    private PartialToken partialToken() {
        return mock(PartialToken.class);
    }

    /**
     * Subclass which exists purely to allow the inherited behaviour of CTSWorkerBaseQuery to be tested.
     */
    private class FakeQuery extends CTSWorkerBaseQuery {

        private final QueryBuilder<Connection, Filter> mockBuilder;

        private FakeQuery( ConnectionFactory<Connection> factory, QueryBuilder<Connection, Filter> mockBuilder) {
            super(factory);
            this.mockBuilder = mockBuilder;
        }

        @Override
        public QueryBuilder getQuery() {
            return mockBuilder;
        }

    }
}