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
package org.forgerock.openam.cts.impl.query.reaper;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.query.QueryFilter;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;

public class ReaperImplTest {

    private QueryFactory mockFactory;
    private CoreTokenConfig mockConfig;
    private ReaperImpl impl;
    private QueryBuilder mockBuilder;
    private Connection mockConnection;
    private QueryFilter mockQueryFilter;
    private QueryFilter.QueryFilterBuilder mockQueryFilterBuilder;

    @BeforeMethod
    public void setup() {
        mockQueryFilterBuilder = mock(QueryFilter.QueryFilterBuilder.class);
        given(mockQueryFilterBuilder.beforeDate(any(Calendar.class))).willReturn(mockQueryFilterBuilder);
        given(mockQueryFilterBuilder.build()).willReturn(Filter.alwaysTrue());

        mockQueryFilter = mock(QueryFilter.class);
        given(mockQueryFilter.and()).willReturn(mockQueryFilterBuilder);

        mockBuilder = mock(QueryBuilder.class);
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(any(CoreTokenField.class))).willReturn(mockBuilder);

        mockFactory = mock(QueryFactory.class);
        given(mockFactory.createFilter()).willReturn(mockQueryFilter);
        given(mockFactory.createInstance()).willReturn(mockBuilder);

        mockConnection = mock(Connection.class);

        mockConfig = mock(CoreTokenConfig.class);
        given(mockConfig.getCleanupPageSize()).willReturn(1);

        impl = new ReaperImpl(mockFactory, mockConfig);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldPreventNullConnection() {
        impl.setConnection(null);
    }

    @Test
    public void shouldQueryForNextPage() throws CoreTokenException {
        impl.setConnection(mockConnection);
        impl.nextPage();
        verify(mockBuilder).executeRawResults(mockConnection);
    }

    @Test
    public void shouldProvideCookieOnEachQuery() throws CoreTokenException {
        // Given
        impl.setConnection(mockConnection);

        ByteString byteString = ByteString.valueOf("badger");
        given(mockBuilder.getPagingCookie()).willReturn(byteString);

        ArgumentCaptor<ByteString> captor = ArgumentCaptor.forClass(ByteString.class);
        given(mockBuilder.pageResultsBy(anyInt(), captor.capture())).willReturn(mockBuilder);

        impl.nextPage();
        impl.nextPage();

        // When
        List<ByteString> results = captor.getAllValues();

        // Then
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(1)).isEqualTo(byteString);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotStartUnlessConnectionIsProvided() throws CoreTokenException {
        impl.nextPage();
    }
}