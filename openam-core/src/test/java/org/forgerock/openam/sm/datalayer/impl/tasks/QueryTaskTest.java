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
package org.forgerock.openam.sm.datalayer.impl.tasks;

import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.LdapAdapter;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.DataLayerRuntimeException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapFilterConversion;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QueryTaskTest {

    private QueryTask task;
    private Connection mockConnection;
    private LdapAdapter mockAdapter;
    private QueryFactory mockQueryFactory;
    private TokenFilter mockTokenFilter;
    private ResultHandler<Collection<Token>, ?> mockResultHandler;
    private LdapFilterConversion mockFilterConversion;
    private QueryBuilder<Connection, Filter> mockQueryBuilder;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setup() {
        mockAdapter = mock(LdapAdapter.class);
        mockConnection = mock(Connection.class);
        mockTokenFilter = mock(TokenFilter.class);
        mockResultHandler = mock(ResultHandler.class);

        // Query Factory
        mockQueryFactory = mock(QueryFactory.class);
        mockQueryBuilder = mock(QueryBuilder.class);
        given(mockQueryFactory.createInstance()).willReturn(mockQueryBuilder);
        given(mockQueryBuilder.withFilter(any(Filter.class))).willReturn(mockQueryBuilder);
        given(mockQueryBuilder.returnTheseAttributes(any(Collection.class))).willReturn(mockQueryBuilder);

        // Filter Conversion
        mockFilterConversion = mock(LdapFilterConversion.class);
        given(mockFilterConversion.convert(any(TokenFilter.class))).willReturn(Filter.alwaysTrue());

        task = new QueryTask(mockQueryFactory, mockFilterConversion, mockTokenFilter, mockResultHandler);
    }

    @Test
    public void shouldExecuteTokenQuery() throws Exception {
        // given
        given(mockTokenFilter.getReturnFields()).willReturn(Collections.<CoreTokenField>emptySet());
        given(mockQueryBuilder.execute(eq(mockConnection))).willReturn(
                Arrays.asList((Collection<Token>)new ArrayList<Token>()).iterator());

        // when
        task.execute(mockConnection, mockAdapter);

        // then
        verify(mockQueryBuilder).execute(eq(mockConnection));
        verify(mockResultHandler).processResults(any(ArrayList.class));
    }

    @Test (expectedExceptions = DataLayerException.class)
    public void shouldHandleQueryFailure() throws Exception {
        // Given
        Iterator<Collection<Token>> iterator = mock(Iterator.class);
        given(mockQueryBuilder.execute(any(Connection.class))).willReturn(iterator);
        given(iterator.next()).willThrow(new DataLayerRuntimeException("test"));

        // When
        task.execute(mockConnection, mockAdapter);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventTokenFilterWithReturnFieldsDefined() throws Exception {
        HashSet<CoreTokenField> returnFields = new HashSet<CoreTokenField>(Arrays.asList(CoreTokenField.TOKEN_ID));
        given(mockTokenFilter.getReturnFields()).willReturn(returnFields);
        task.execute(mockConnection, mockAdapter);
    }
}