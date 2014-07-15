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
package org.forgerock.openam.cts.impl.task;

import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.query.FilterConversion;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.BDDMockito.*;

public class QueryTaskTest {

    private QueryTask task;
    private Connection mockConnection;
    private LDAPAdapter mockAdapter;
    private QueryFactory mockQueryFactory;
    private TokenFilter mockTokenFilter;
    private ResultHandler mockResultHandler;
    private FilterConversion mockFilterConversion;
    private QueryBuilder mockQueryBuilder;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setup() {
        mockAdapter = mock(LDAPAdapter.class);
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
        mockFilterConversion = mock(FilterConversion.class);
        given(mockFilterConversion.convert(any(TokenFilter.class))).willReturn(Filter.alwaysTrue());

        task = new QueryTask(mockQueryFactory, mockFilterConversion, mockTokenFilter, mockResultHandler);
    }

    @Test
    public void shouldExecuteTokenQuery() throws CoreTokenException {
        given(mockTokenFilter.getReturnFields()).willReturn(Collections.<CoreTokenField>emptySet());
        task.execute(mockConnection, mockAdapter);
        verify(mockQueryBuilder).execute(eq(mockConnection));
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldHandleQueryFailure() throws CoreTokenException {
        given(mockQueryBuilder.execute(any(Connection.class))).willThrow(new CoreTokenException("test"));
        task.execute(mockConnection, mockAdapter);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventTokenFilterWithReturnFieldsDefined() throws CoreTokenException {
        HashSet<CoreTokenField> returnFields = new HashSet<CoreTokenField>(Arrays.asList(CoreTokenField.TOKEN_ID));
        given(mockTokenFilter.getReturnFields()).willReturn(returnFields);
        task.execute(mockConnection, mockAdapter);
    }
}