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
package org.forgerock.openam.cts.impl.query;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.cts.utils.TokenAttributeConversion;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class QueryBuilderTest {

    private LDAPConfig constants;
    private TokenAttributeConversion attributeConversion;
    private LDAPSearchHandler searchHandler;
    private QueryBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        constants = new LDAPConfig("cn=test");
        attributeConversion = mock(TokenAttributeConversion.class);
        searchHandler = mock(LDAPSearchHandler.class);

        builder = new QueryBuilder(
                attributeConversion,
                constants,
                searchHandler,
                mock(Debug.class));
    }

    @Test
    public void shouldUseHandlerToPerformSearch() throws CoreTokenException, IOException {
        // Given
        Result mockResult = mock(Result.class);
        given(searchHandler.performSearch(any(SearchRequest.class), any(Collection.class))).willReturn(mockResult);

        // When
        builder.executeRawResults();

        // Then
        verify(searchHandler).performSearch(any(SearchRequest.class), any(Collection.class));
    }

    @Test
    public void shouldReturnTokensFromSearch() throws CoreTokenException {
        // Given
        final Collection<Entry> entries = new LinkedList<Entry>();
        entries.add(new LinkedHashMapEntry());
        entries.add(new LinkedHashMapEntry());

        // Slightly more fiddly mocking to provide behaviour when the mock is called.
        given(searchHandler.performSearch(any(SearchRequest.class), any(Collection.class))).will(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Collection<Entry> list = (Collection<Entry>) invocationOnMock.getArguments()[1];
                list.addAll(entries);
                return null;
            }
        });

        // Ensure that the Token Conversion returns a Token
        given(attributeConversion.tokenFromEntry(any(Entry.class))).willReturn(
                        new Token(Long.toString(System.currentTimeMillis()),
                        TokenType.SESSION));

        // When
        Collection<Token> results = builder.execute();

        // Then
        assertThat(results.size()).isEqualTo(entries.size());
    }
}
