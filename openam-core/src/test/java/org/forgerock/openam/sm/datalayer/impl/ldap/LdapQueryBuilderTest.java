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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.impl.ldap;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.CTSDataLayerConfiguration;
import org.forgerock.openam.sm.SMSDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class LdapQueryBuilderTest {

    private LdapDataLayerConfiguration config;
    private LdapSearchHandler searchHandler;
    private QueryBuilder builder;
    private Connection mockConnection;
    private EntryConverter<PartialToken> partialTokenEntryConverter;
    private EntryConverter<Token> tokenEntryConverter;

    @BeforeMethod
    public void setUp() throws Exception {
        config = new CTSDataLayerConfiguration("ou=test-case");
        searchHandler = mock(LdapSearchHandler.class);
        mockConnection = mock(Connection.class);
        partialTokenEntryConverter = mock(EntryConverter.class);
        tokenEntryConverter = mock(EntryConverter.class);
        Map<Class, EntryConverter> converterMap = new HashMap<Class, EntryConverter>();
        converterMap.put(PartialToken.class, partialTokenEntryConverter);
        converterMap.put(Token.class, tokenEntryConverter);

        builder = new LdapQueryBuilder(
                config,
                searchHandler,
                mock(Debug.class),
                converterMap
                );
    }

    @Test
    public void shouldUseHandlerToPerformSearch() throws CoreTokenException, IOException {
        // Given
        Result mockResult = mock(Result.class);
        given(searchHandler.performSearch(any(Connection.class), any(SearchRequest.class), any(Collection.class))).willReturn(mockResult);

        // When
        Iterator iterator = builder.executeRawResults(mockConnection, PartialToken.class);

        // Then
        verifyZeroInteractions(searchHandler);
        iterator.next();
        verify(searchHandler).performSearch(eq(mockConnection), any(SearchRequest.class), any(Collection.class));
    }

    @Test
    public void shouldReturnTokensFromSearch() throws CoreTokenException {
        // Given
        final Collection<Entry> entries = new LinkedList<Entry>();
        entries.add(new LinkedHashMapEntry());
        entries.add(new LinkedHashMapEntry());

        // Slightly more fiddly mocking to provide behaviour when the mock is called.
        given(searchHandler.performSearch(any(Connection.class), any(SearchRequest.class), any(Collection.class))).will(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Collection<Entry> list = (Collection<Entry>) invocationOnMock.getArguments()[2];
                list.addAll(entries);
                return null;
            }
        });

        // Ensure that the Token Conversion returns a Token
        given(tokenEntryConverter.convert(any(Entry.class), any(String[].class))).willReturn(
                new Token(Long.toString(currentTimeMillis()), TokenType.SESSION));

        // When
        Iterator<Collection<Token>> results = builder.execute(mockConnection);

        // Then
        verifyZeroInteractions(tokenEntryConverter);
        assertThat(results.next().size()).isEqualTo(entries.size());
        verify(tokenEntryConverter, times(2)).convert(any(Entry.class), any(String[].class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventSettingReturnAttributesWithEmptyArray() {
        builder.returnTheseAttributes();
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventSettingReturnAttributesWithNullArray() {
        CoreTokenField[] fields = null;
        builder.returnTheseAttributes(fields);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventSettingReturnAttributesWithEmptyCollection() {
        builder.returnTheseAttributes(Collections.<CoreTokenField>emptySet());
    }
}
