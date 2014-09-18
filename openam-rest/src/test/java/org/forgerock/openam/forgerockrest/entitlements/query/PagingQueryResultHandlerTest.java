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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements.query;

import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class PagingQueryResultHandlerTest {

    @Mock
    private QueryResultHandler mockHandler;

    private PagingQueryResultHandler testHandler;

    @BeforeMethod
    protected void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDelegateHandler() {
        new PagingQueryResultHandler(null, 1, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroPageSize() {
        new PagingQueryResultHandler(mockHandler, 0, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativePageSize() {
        new PagingQueryResultHandler(mockHandler, -1, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativePageOffset() {
        new PagingQueryResultHandler(mockHandler, 1, -1);
    }

    @Test
    public void shouldOnlyReadUntilPageIsRead() {
        // Given
        testHandler = new PagingQueryResultHandler(mockHandler, 2, 0);
        given(mockHandler.handleResource(Matchers.any(Resource.class))).willReturn(true);

        // When
        boolean firstHandle = testHandler.handleResource(new Resource("a", null, null));
        boolean secondHandle = testHandler.handleResource(new Resource("b", null, null));

        // Then
        assertThat(firstHandle).isTrue();
        assertThat(secondHandle).isFalse();
    }

    @Test
    public void shouldRespectOffset() {
        // Given
        testHandler = new PagingQueryResultHandler(mockHandler, 1, 1);
        Resource expectedResource = new Resource("b", null, null);

        // When
        testHandler.handleResource(new Resource("ignored", null, null));
        testHandler.handleResource(expectedResource);

        // Then
        verify(mockHandler).handleResource(expectedResource);
        verifyNoMoreInteractions(mockHandler);
    }

    @Test
    public void shouldIgnoreExtraResources() {
        // Given
        testHandler = new PagingQueryResultHandler(mockHandler, 1, 0);
        given(mockHandler.handleResource(Matchers.any(Resource.class))).willReturn(true);
        Resource expected = new Resource("expected", null, null);

        // When
        testHandler.handleResource(expected);
        // Additional resource beyond page size
        testHandler.handleResource(new Resource("b", null, null));

        // Then
        verify(mockHandler).handleResource(expected);
        verifyNoMoreInteractions(mockHandler);
    }
}
