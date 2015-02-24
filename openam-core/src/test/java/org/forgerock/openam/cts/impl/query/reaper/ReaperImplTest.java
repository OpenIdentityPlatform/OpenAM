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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query.reaper;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.isNull;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ReaperImplTest {

    private QueryFactory<Connection, Filter> mockFactory;
    private CoreTokenConfig mockConfig;
    private ReaperImpl<Connection, Filter> impl;
    private QueryBuilder<Connection, Filter> mockBuilder;
    private Connection mockConnection;
    private QueryFilterVisitor<Filter, Void, CoreTokenField> mockQueryFilterConverter;

    @BeforeMethod
    public void setup() {

        mockBuilder = mock(QueryBuilder.class);
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.pageResultsBy(anyInt())).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(any(CoreTokenField.class))).willReturn(mockBuilder);

        mockQueryFilterConverter = mock(QueryFilterVisitor.class);
        given(mockQueryFilterConverter.visitLessThanFilter((Void)isNull(), eq(CoreTokenField.EXPIRY_DATE), any(Calendar.class)))
                .willReturn(Filter.alwaysTrue());

        mockFactory = mock(QueryFactory.class);
        given(mockFactory.createFilterConverter()).willReturn(mockQueryFilterConverter);
        given(mockFactory.createInstance()).willReturn(mockBuilder);

        mockConnection = mock(Connection.class);

        mockConfig = mock(CoreTokenConfig.class);
        given(mockConfig.getCleanupPageSize()).willReturn(1);

        impl = new ReaperImpl<Connection, Filter>(mockFactory, mockConfig);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldPreventNullConnection() {
        impl.setConnection(null);
    }

    @Test
    public void shouldQueryForNextPage() throws CoreTokenException {
        //given
        impl.setConnection(mockConnection);
        Iterator<Collection<String>> mockIterator = mock(Iterator.class);
        given(mockIterator.hasNext()).willReturn(true);
        given(mockIterator.next()).willReturn(Arrays.asList("fred"));
        given(mockBuilder.executeRawResults(mockConnection, String.class)).willReturn(mockIterator);

        // when
        Collection<String> page = impl.nextPage();

        // then
        verify(mockIterator).hasNext();
        verify(mockIterator).next();
        assertThat(page).containsOnly("fred");
    }

    @Test
    public void shouldLoopUntilIteratorEmpty() throws CoreTokenException {
        // Given
        impl.setConnection(mockConnection);
        Iterator<Collection<String>> mockIterator = mock(Iterator.class);
        given(mockBuilder.executeRawResults(mockConnection, String.class)).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true, true, false);
        given(mockIterator.next()).willReturn(Arrays.asList("fred"), Arrays.asList("barney"));

        // When
        Collection<String> page1 = impl.nextPage();
        Collection<String> page2 = impl.nextPage();
        Collection<String> page3 = impl.nextPage();

        // Then
        assertThat(page1).containsOnly("fred");
        assertThat(page2).containsOnly("barney");
        assertThat(page3).isNull();
        verify(mockIterator, times(2)).next();
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotStartUnlessConnectionIsProvided() throws CoreTokenException {
        impl.nextPage();
    }
}