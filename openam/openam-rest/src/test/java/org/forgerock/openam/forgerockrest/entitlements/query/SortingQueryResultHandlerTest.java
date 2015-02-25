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

import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.SortKey;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SortingQueryResultHandlerTest {
    private static final List<SortKey> SORT_BY_NAME = Arrays.asList(SortKey.ascendingOrder("name"));
    private static final List<SortKey> SORT_BY_NAME_DESC = Arrays.asList(SortKey.descendingOrder("name"));
    private static final List<SortKey> SORT_BY_NAME_AND_AGE = Arrays.asList(SortKey.ascendingOrder("name"),
            SortKey.descendingOrder("age"));

    @Mock
    private QueryResultHandler mockHandler;

    private SortingQueryResultHandler sortingHandler;

    @BeforeMethod
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDelegateHandler() {
        new SortingQueryResultHandler(null, SORT_BY_NAME);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullSortKeys() {
        new SortingQueryResultHandler(mockHandler, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptySortKeys() {
        new SortingQueryResultHandler(mockHandler, Collections.<SortKey>emptyList());
    }

    @Test
    public void shouldSortResults() {
        // Given
        sortingHandler = new SortingQueryResultHandler(mockHandler, SORT_BY_NAME);
        List<Resource> resources = namedResources("b", "a", "c");
        given(mockHandler.handleResource(any(Resource.class))).willReturn(true);

        // When
        for (Resource resource : resources) {
            sortingHandler.handleResource(resource);
        }
        sortingHandler.handleResult(new QueryResult());

        // Then
        ArgumentCaptor<Resource> resourceArgumentCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler, times(resources.size())).handleResource(resourceArgumentCaptor.capture());
        assertThat(resourceArgumentCaptor.getAllValues()).isEqualTo(namedResources("a", "b", "c"));
    }

    @Test
    public void shouldSortResultsDescendingIfSpecified() {
        // Given
        sortingHandler = new SortingQueryResultHandler(mockHandler, SORT_BY_NAME_DESC);
        List<Resource> resources = namedResources("b", "a", "c");
        given(mockHandler.handleResource(any(Resource.class))).willReturn(true);

        // When
        for (Resource resource : resources) {
            sortingHandler.handleResource(resource);
        }
        sortingHandler.handleResult(new QueryResult());

        // Then
        ArgumentCaptor<Resource> resourceArgumentCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler, times(resources.size())).handleResource(resourceArgumentCaptor.capture());
        assertThat(resourceArgumentCaptor.getAllValues()).isEqualTo(namedResources("c", "b", "a"));
    }

    @Test
    public void shouldSupportMultiLevelSorting() {
        // Given
        sortingHandler = new SortingQueryResultHandler(mockHandler, SORT_BY_NAME_AND_AGE);
        List<Resource> resources = nameAgeResources(field("b", 1), field("b", 2), field("a", 1), field("c", 1));
        given(mockHandler.handleResource(any(Resource.class))).willReturn(true);

        // When
        for (Resource resource : resources) {
            sortingHandler.handleResource(resource);
        }
        sortingHandler.handleResult(new QueryResult());

        // Then
        ArgumentCaptor<Resource> resourceArgumentCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler, times(resources.size())).handleResource(resourceArgumentCaptor.capture());
        assertThat(resourceArgumentCaptor.getAllValues())
                // Sort by name first and then equal names should be sorted by descending age
                .isEqualTo(nameAgeResources(field("a", 1), field("b", 2), field("b", 1), field("c", 1)));

    }

    @Test
    public void shouldHandleNullContent() {
        // Given
        sortingHandler = new SortingQueryResultHandler(mockHandler, SORT_BY_NAME);
        Resource resource = new Resource("a", null, null);

        // When
        sortingHandler.handleResource(resource);
        sortingHandler.handleResult(new QueryResult());

        // Then
        verify(mockHandler).handleResource(resource);
    }

    @Test
    public void shouldUpdateQueryResultWithAccurateRemainingResultsCount() {
        // Given
        sortingHandler = new SortingQueryResultHandler(mockHandler, SORT_BY_NAME);
        String cookie = "xxx";
        QueryResult queryResult = new QueryResult(cookie, 12);
        List<Resource> resources = namedResources("a", "b", "c");
        // Only accept a single resource
        given(mockHandler.handleResource(any(Resource.class))).willReturn(false);

        // When
        for (Resource resource : resources) {
            sortingHandler.handleResource(resource);
        }
        sortingHandler.handleResult(queryResult);

        // Then
        ArgumentCaptor<QueryResult> resultCaptor = ArgumentCaptor.forClass(QueryResult.class);
        verify(mockHandler).handleResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getPagedResultsCookie()).isEqualTo(cookie);
        assertThat(resultCaptor.getValue().getRemainingPagedResults()).isEqualTo(resources.size() - 1);
    }

    private List<Resource> namedResources(String...names) {
        List<Resource> resources = new ArrayList<Resource>(names.length);

        for (String name : names) {
            resources.add(new Resource(name, null, json(object(field("name", name)))));
        }

        return resources;
    }

    @SuppressWarnings("unchecked")
    private List<Resource> nameAgeResources(Map.Entry... entries) {
        List<Resource> resources = new ArrayList<Resource>(entries.length);

        for (Map.Entry<String, Object> entry : entries) {
            resources.add(new Resource(entry.getKey(), null, json(object(
                    field("name", entry.getKey()),
                    field("age", entry.getValue())
            ))));
        }

        return resources;
    }
}
