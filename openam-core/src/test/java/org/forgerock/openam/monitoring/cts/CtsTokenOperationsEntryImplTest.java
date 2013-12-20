/*
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
 *
 */

package org.forgerock.openam.monitoring.cts;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import java.util.ArrayList;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.query.QueryFilter;
import org.forgerock.opendj.ldap.Entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CtsTokenOperationsEntryImplTest {

    private CtsTokenOperationsEntryImpl entryImpl;

    private QueryFactory factory;
    private Debug debug;

    @BeforeMethod
    public void setUp() {

        factory = mock(QueryFactory.class);
        debug = mock(Debug.class);

        SnmpMib myMib = mock(SnmpMib.class);

        entryImpl = new CtsTokenOperationsEntryImpl(factory, myMib, debug);
    }

    /**
     * Sets up the implementation to execute and query the system in a mocked fashion.
     *
     * @param queryFilterBuilder
     * @param queryBuilder
     * @throws CoreTokenException
     */
    private void performSetup(QueryFilter.QueryFilterBuilder queryFilterBuilder,
                              QueryBuilder queryBuilder, TokenType token) throws CoreTokenException {
        entryImpl.TokenTableIndex = 1l;

        QueryFilter queryFilter = mock(QueryFilter.class);

        given(factory.createFilter()).willReturn(queryFilter);
        given(queryFilter.and()).willReturn(queryFilterBuilder);
        given(queryFilterBuilder.attribute(CoreTokenField.TOKEN_TYPE, token)).willReturn(queryFilterBuilder);

        given(factory.createInstance()).willReturn(queryBuilder);
        given(queryBuilder.returnTheseAttributes(CoreTokenField.TOKEN_ID)).willReturn(queryBuilder);
        given(queryBuilder.withFilter(null)).willReturn(queryBuilder);
    }

    @Test
    public void getTotalCountTest() throws CoreTokenException {

        //given
        QueryFilter.QueryFilterBuilder queryFilterBuilder = mock(QueryFilter.QueryFilterBuilder.class);
        TokenType token = TokenType.values()[0];
        QueryBuilder queryBuilder = mock(QueryBuilder.class);

        ArrayList<Entry> results = new ArrayList<Entry>();
        Entry mockEntry = mock(Entry.class);
        results.add(mockEntry);

        performSetup(queryFilterBuilder, queryBuilder, token);
        given(queryBuilder.executeRawResults()).willReturn(results);

        //when
        long result = entryImpl.getTotalCount();

        //then
        verify(queryFilterBuilder).attribute(CoreTokenField.TOKEN_TYPE, token);
        verify(queryBuilder).returnTheseAttributes(CoreTokenField.TOKEN_ID);

        assertEquals(1l, result);
    }

    @Test(expectedExceptions = InvalidSNMPQueryException.class)
    public void getTotalCountTestErrorPersistentStoreFail() throws CoreTokenException {

        //given
        QueryFilter.QueryFilterBuilder queryFilterBuilder = mock(QueryFilter.QueryFilterBuilder.class);
        TokenType token = TokenType.values()[0];
        QueryBuilder queryBuilder = mock(QueryBuilder.class);

        performSetup(queryFilterBuilder, queryBuilder, token);

        given(queryBuilder.executeRawResults()).willThrow(new CoreTokenException("Error"));

        //when
        entryImpl.getTotalCount();

        //then
        //throw exception
    }

    @Test(expectedExceptions = InvalidSNMPQueryException.class)
    public void getTotalCountTestErrorInvalidToken() {
        //given
        entryImpl.TokenTableIndex = -1l;

        //when
        entryImpl.getTotalCount();

        //then
        //throw exception
    }

}