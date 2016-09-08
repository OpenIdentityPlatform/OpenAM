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
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.LdapAdapter;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Connection;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QueryTaskTest {

    private QueryTask task;
    private Connection mockConnection;
    private TokenStorageAdapter<Connection> mockAdapter;
    private TokenFilter mockTokenFilter;
    private ResultHandler<Collection<Token>, ?> mockResultHandler;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setup() {
        mockAdapter = mock(LdapAdapter.class);
        mockConnection = mock(Connection.class);
        mockTokenFilter = mock(TokenFilter.class);
        mockResultHandler = mock(ResultHandler.class);

        task = new QueryTask(mockTokenFilter, mockResultHandler);
    }

    @Test
    public void shouldExecuteTokenQuery() throws Exception {
        // given
        given(mockAdapter.query(mockConnection, mockTokenFilter)).willReturn(new ArrayList<Token>());

        // when
        task.execute(mockConnection, mockAdapter);

        // then
        verify(mockResultHandler).processResults(any(ArrayList.class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventTokenFilterWithReturnFieldsDefined() throws Exception {
        HashSet<CoreTokenField> returnFields = new HashSet<CoreTokenField>(Arrays.asList(CoreTokenField.TOKEN_ID));
        given(mockTokenFilter.getReturnFields()).willReturn(returnFields);
        task.execute(mockConnection, mockAdapter);
    }
}