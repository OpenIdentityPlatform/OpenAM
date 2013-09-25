/**
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
 */
package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author robert.wapshott@forgerock.com
 */
public class QueryPageIteratorTest {

    private QueryBuilder mockBuilder;
    private QueryPageIterator iterator;

    @BeforeMethod
    public void setUp() throws Exception {
        mockBuilder = mock(QueryBuilder.class);
        iterator = new QueryPageIterator(mockBuilder, 10);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldAssertPageSize() {
        iterator = new QueryPageIterator(mockBuilder, -10);
    }

    @Test
    public void shouldHaveNextOnFirstCall() {
        assertThat(iterator.hasNext()).isTrue();
    }

    @Test
    public void shouldPerfomQueryWithNextCall() throws CoreTokenException {
        // Given

        // When
        iterator.next();

        // Then
        verify(mockBuilder, atLeastOnce()).executeRawResults();
    }

    @Test
    public void shouldIndicateNoFurtherQueries() throws CoreTokenException {
        // Given
        given(mockBuilder.getPagingCookie()).willReturn(QueryBuilder.getEmptyPagingCookie());
        iterator.next();

        // When
        boolean result = iterator.hasNext();

        // Then
        assertFalse(result);
    }
}
