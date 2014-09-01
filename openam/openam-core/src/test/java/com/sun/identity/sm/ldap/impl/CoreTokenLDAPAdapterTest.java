/**
 * Copyright 2013 ForgeRock, Inc.
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
package com.sun.identity.sm.ldap.impl;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.exceptions.DeleteFailedException;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import com.sun.identity.sm.ldap.utils.TokenAttributeConversion;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenLDAPAdapterTest {
    @Test
    public void shouldCreateToken() throws CoreTokenException, ErrorResultException {
        // Given
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);
        Connection connection = mock(Connection.class);
        given(connectionFactory.getConnection()).willReturn(connection);

        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = new TokenAttributeConversion(constants, dataConversion);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                connectionFactory,
                dataConversion,
                attributeConversion,
                constants,
                new QueryFactory(),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the Connection add is a success
        Result result = successResult();
        given(connection.add(any(Entry.class))).willReturn(result);

        // When
        adapter.create(token);

        // Then
        verify(connection).add(any(Entry.class));
    }

    @Test
    public void shouldGenerateAnInstanceOfQueryBuilder() throws ErrorResultException {
        // Given
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);
        Connection connection = mock(Connection.class);
        given(connectionFactory.getConnection()).willReturn(connection);

        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = new TokenAttributeConversion(constants, dataConversion);
        QueryFactory factory = mock(QueryFactory.class);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                connectionFactory,
                dataConversion,
                attributeConversion,
                constants,
                factory,
                mock(Debug.class));

        // When
        adapter.query();

        // Then
        verify(factory).createInstance(connectionFactory, constants);
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldFailIfMultipleTokensFoundWhilstUpdating() throws CoreTokenException, ErrorResultException {
        // Given
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);
        Connection connection = mock(Connection.class);
        given(connectionFactory.getConnection()).willReturn(connection);

        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = new TokenAttributeConversion(constants, dataConversion);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                connectionFactory,
                dataConversion,
                attributeConversion,
                constants,
                mockQueryBuilderFactory(2),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // When / Then
        adapter.update(token);

    }

    @Test
    public void shouldCreateTokenIfNotPresentWhilstUpdating() throws CoreTokenException, ErrorResultException {
        // Given
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);
        Connection connection = mock(Connection.class);
        given(connectionFactory.getConnection()).willReturn(connection);

        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = new TokenAttributeConversion(constants, dataConversion);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                connectionFactory,
                dataConversion,
                attributeConversion,
                constants,
                mockQueryBuilderFactory(0),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the Connection add is a success
        Result success = successResult();
        given(connection.add(any(Entry.class))).willReturn(success);

        // When
        adapter.update(token);

        // Then
        verify(connection).add(any(Entry.class));
    }

    @Test
    public void shouldPerformUpdate() throws ErrorResultException, CoreTokenException {
        // Given
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);
        Connection connection = mock(Connection.class);
        given(connectionFactory.getConnection()).willReturn(connection);

        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = new TokenAttributeConversion(constants, dataConversion);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                connectionFactory,
                dataConversion,
                attributeConversion,
                constants,
                mockQueryBuilderFactory(1),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the Connection add is a success
        Result success = successResult();
        given(connection.modify(any(ModifyRequest.class))).willReturn(success);

        // When
        adapter.update(token);

        // Then
        verify(connection).modify(any(ModifyRequest.class));
    }

    @Test
    public void shouldDeleteToken() throws ErrorResultException, DeleteFailedException {
        // Given
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);
        Connection connection = mock(Connection.class);
        given(connectionFactory.getConnection()).willReturn(connection);

        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = mock(TokenAttributeConversion.class);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                connectionFactory,
                dataConversion,
                attributeConversion,
                constants,
                new QueryFactory(),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the delete operation is a success
        Result success = successResult();
        given(connection.delete(anyString())).willReturn(success);

        // Ensure conversion via TokenAttributeConversion is successful
        DN dn = DN.rootDN();
        given(attributeConversion.generateTokenDN(eq(token))).willReturn(dn);

        // When
        adapter.delete(token);

        // Then
        verify(connection).delete(anyString());
    }

    private QueryFactory mockQueryBuilderFactory(int entriesToReturn) throws CoreTokenException {
        QueryFactory factory = mock(QueryFactory.class);
        QueryBuilder queryBuilder = mock(QueryBuilder.class);
        given(factory.createInstance(
                any(DataLayerConnectionFactory.class),
                any(CoreTokenConstants.class)))
                .willReturn(queryBuilder);
        given(factory.createFilter()).willReturn(new QueryFilter(mock(LDAPDataConversion.class)));

        List<Entry> entries = new LinkedList<Entry>();
        for (int ii = 0; ii < entriesToReturn; ii++) {
            Entry e = new LinkedHashMapEntry();
            entries.add(e);
        }
        given(queryBuilder.withFilter(any(Filter.class))).willReturn(queryBuilder);
        given(queryBuilder.executeRawResults()).willReturn(entries);

        return factory;
    }

    private Result successResult() {
        Result result = mock(Result.class);
        ResultCode resultCode = ResultCode.SUCCESS;
        given(result.getResultCode()).willReturn(resultCode);
        return result;
    }
}
