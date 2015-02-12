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
import java.util.HashSet;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.impl.LdapAdapter;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapFilterConversion;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PartialQueryTaskTest {

    private QueryFactory<Connection, Filter> mockFactory;
    private LdapFilterConversion mockConversion;
    private ResultHandler<Collection<PartialToken>, ?> mockResult;
    private QueryBuilder<Connection, Filter> mockBuilder;
    private PartialQueryTask task;
    private TokenFilter mockFilter;
    private Connection mockConnection;
    private LdapAdapter mockAdapter;

    @BeforeMethod
    public void setup() {
        mockFactory = mock(QueryFactory.class);
        mockBuilder = mock(QueryBuilder.class);
        given(mockFactory.createInstance()).willReturn(mockBuilder);
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(anyCollection())).willReturn(mockBuilder);

        mockConversion = mock(LdapFilterConversion.class);
        mockResult = mock(ResultHandler.class);

        mockConnection = mock(Connection.class);
        mockAdapter = mock(LdapAdapter.class);

        mockFilter = mock(TokenFilter.class);

        task = new PartialQueryTask(mockFactory, mockConversion, mockFilter, mockResult);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectFilterWithNoReturnFieldsSet() throws Exception {
        given(mockFilter.getReturnFields()).willReturn(new HashSet<CoreTokenField>());
        task.execute(mockConnection, mockAdapter);
    }

    @Test
    public void shouldExecuteAttributeQuery() throws Exception {
        // given
        given(mockFilter.getReturnFields()).willReturn(new HashSet<CoreTokenField>(Arrays.asList(CoreTokenField.TOKEN_ID)));
        given(mockBuilder.executeAttributeQuery(mockConnection)).willReturn(
                Arrays.asList((Collection<PartialToken>)new ArrayList<PartialToken>()).iterator());


        // when
        task.execute(mockConnection, mockAdapter);

        // then
        verify(mockBuilder).executeAttributeQuery(eq(mockConnection));
        verify(mockResult).processResults(any(ArrayList.class));
    }
}