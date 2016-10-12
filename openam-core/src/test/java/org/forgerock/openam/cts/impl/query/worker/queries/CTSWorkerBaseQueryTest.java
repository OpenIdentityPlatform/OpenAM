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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CTSWorkerBaseQueryTest {

    private FakeQuery query;
    private QueryBuilder<Connection, Filter> mockBuilder;
    private Connection mockConnection;

    @BeforeMethod
    public void setup() {
        mockBuilder = mock(QueryBuilder.class);
        mockConnection = mock(Connection.class);
        query = new FakeQuery(mockBuilder);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldPreventNullConnection() {
        query.setConnection(null);
    }

    @Test
    public void shouldQueryForNextPage() throws CoreTokenException {
        //given
        query.setConnection(mockConnection);
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
        query.setConnection(mockConnection);
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

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotStartUnlessConnectionIsProvided() throws CoreTokenException {
        query.nextPage();
    }

    private PartialToken partialToken() {
        return mock(PartialToken.class);
    }

    /**
     * Subclass which exists purely to allow the inherited behaviour of CTSWorkerBaseQuery to be tested.
     */
    private class FakeQuery extends CTSWorkerBaseQuery {

        private final QueryBuilder<Connection, Filter> mockBuilder;

        private FakeQuery(QueryBuilder<Connection, Filter> mockBuilder) {
            this.mockBuilder = mockBuilder;
        }

        @Override
        public QueryBuilder getQuery() {
            return mockBuilder;
        }

    }
}