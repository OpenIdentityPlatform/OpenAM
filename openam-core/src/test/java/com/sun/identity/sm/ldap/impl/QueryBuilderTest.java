/**
 * Copyright 2013 ForgeRock, AS.
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
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import com.sun.identity.sm.ldap.utils.TokenAttributeConversion;
import org.apache.commons.collections.CollectionUtils;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author robert.wapshott@forgerock.com
 */
public class QueryBuilderTest {
    @Test
    public void shouldPerformSearch() throws CoreTokenException, IOException {
        // Given
        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        LDAPDataConversion conversion = new LDAPDataConversion();
        TokenAttributeConversion attributeConversion = new TokenAttributeConversion(constants, conversion);
        LDAPSearchHandler searchHandler = mock(LDAPSearchHandler.class);
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);

        QueryBuilder builder = new QueryBuilder(
                connectionFactory,
                attributeConversion,
                constants,
                searchHandler,
                mock(Debug.class));

        // Simulate search results
        List<Entry> entries= new LinkedList<Entry>();
        entries.add(new LinkedHashMapEntry());
        entries.add(new LinkedHashMapEntry());

        given(searchHandler.performSearch(any(SearchRequest.class))).willReturn(entries);

        // When
        Collection<Entry> results = builder.executeRawResults();

        // Then
        assertTrue(CollectionUtils.isEqualCollection(results, entries));
    }

    @Test
    public void shouldReturnTokensFromSearch() throws CoreTokenException {
        // Given
        CoreTokenConstants constants = new CoreTokenConstants("cn=test");
        TokenAttributeConversion attributeConversion = mock(TokenAttributeConversion.class);
        LDAPSearchHandler searchHandler = mock(LDAPSearchHandler.class);
        DataLayerConnectionFactory connectionFactory = mock(DataLayerConnectionFactory.class);

        QueryBuilder builder = new QueryBuilder(
                connectionFactory,
                attributeConversion,
                constants,
                searchHandler,
                mock(Debug.class));

        // Simulate search results
        List<Entry> entries= new LinkedList<Entry>();
        entries.add(new LinkedHashMapEntry());
        entries.add(new LinkedHashMapEntry());
        given(searchHandler.performSearch(any(SearchRequest.class))).willReturn(entries);

        // Ensure that the Token Conversion returns a Token
        given(attributeConversion.tokenFromEntry(any(Entry.class))).willReturn(
                        new Token(Long.toString(System.currentTimeMillis()),
                        TokenType.SESSION));

        // When
        Collection<Token> results = builder.execute();

        // Then
        assertEquals(results.size(), entries.size());
    }
}
