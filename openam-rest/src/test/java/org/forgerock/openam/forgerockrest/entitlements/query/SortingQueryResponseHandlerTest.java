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

package org.forgerock.openam.forgerockrest.entitlements.query;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SortKey;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SortingQueryResponseHandlerTest {
    private static final List<SortKey> SORT_BY_NAME = Collections.singletonList(SortKey.ascendingOrder("name"));
    private static final List<SortKey> SORT_BY_NAME_DESC = Collections.singletonList(SortKey.descendingOrder("name"));
    private static final List<SortKey> SORT_BY_NAME_AND_AGE = Arrays.asList(SortKey.ascendingOrder("name"),
            SortKey.descendingOrder("age"));

    @Mock
    private QueryResponseHandler mockHandler;

    private SortingQueryResponseHandler sortingHandler;

    @BeforeMethod
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullDelegateHandler() {
        new SortingQueryResponseHandler(null, SORT_BY_NAME);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullSortKeys() {
        new SortingQueryResponseHandler(mockHandler, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptySortKeys() {
        new SortingQueryResponseHandler(mockHandler, Collections.<SortKey>emptyList());
    }

    @Test
    public void shouldSortResults() {
        // Given
        sortingHandler = new SortingQueryResponseHandler(mockHandler, SORT_BY_NAME);
        List<ResourceResponse> resources = namedResources("b", "a", "c");
        given(mockHandler.handleResource(any(ResourceResponse.class))).willReturn(true);

        // When
        for (ResourceResponse resource : resources) {
            sortingHandler.handleResource(resource);
        }
        sortingHandler.getResult(newQueryResponse());

        // Then
        ArgumentCaptor<ResourceResponse> resourceArgumentCaptor = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(mockHandler, times(resources.size())).handleResource(resourceArgumentCaptor.capture());
        assertThat(resourceArgumentCaptor.getAllValues()).isEqualTo(namedResources("a", "b", "c"));
    }

    @Test
    public void shouldSortResultsDescendingIfSpecified() {
        // Given
        sortingHandler = new SortingQueryResponseHandler(mockHandler, SORT_BY_NAME_DESC);
        List<ResourceResponse> resources = namedResources("b", "a", "c");
        given(mockHandler.handleResource(any(ResourceResponse.class))).willReturn(true);

        // When
        for (ResourceResponse resource : resources) {
            sortingHandler.handleResource(resource);
        }
        sortingHandler.getResult(newQueryResponse());

        // Then
        ArgumentCaptor<ResourceResponse> resourceArgumentCaptor = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(mockHandler, times(resources.size())).handleResource(resourceArgumentCaptor.capture());
        assertThat(resourceArgumentCaptor.getAllValues()).isEqualTo(namedResources("c", "b", "a"));
    }

    @Test
    public void shouldSupportMultiLevelSorting() {
        // Given
        sortingHandler = new SortingQueryResponseHandler(mockHandler, SORT_BY_NAME_AND_AGE);
        List<ResourceResponse> resources = nameAgeResources(field("b", 1), field("b", 2), field("a", 1), field("c", 1));
        given(mockHandler.handleResource(any(ResourceResponse.class))).willReturn(true);

        // When
        for (ResourceResponse resource : resources) {
            sortingHandler.handleResource(resource);
        }
        sortingHandler.getResult(newQueryResponse());

        // Then
        ArgumentCaptor<ResourceResponse> resourceArgumentCaptor = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(mockHandler, times(resources.size())).handleResource(resourceArgumentCaptor.capture());
        assertThat(resourceArgumentCaptor.getAllValues())
                // Sort by name first and then equal names should be sorted by descending age
                .isEqualTo(nameAgeResources(field("a", 1), field("b", 2), field("b", 1), field("c", 1)));

    }

    @Test
    public void shouldHandleNullContent() {
        // Given
        sortingHandler = new SortingQueryResponseHandler(mockHandler, SORT_BY_NAME);
        ResourceResponse resource = newResourceResponse("a", null, null);

        // When
        sortingHandler.handleResource(resource);
        sortingHandler.getResult(newQueryResponse());

        // Then
        verify(mockHandler).handleResource(resource);
    }

    @Test
    public void shouldUpdateQueryResultWithAccurateRemainingResultsCount() {
        // Given
        sortingHandler = new SortingQueryResponseHandler(mockHandler, SORT_BY_NAME);
        String cookie = "xxx";
        QueryResponse queryResult = newQueryResponse(cookie, CountPolicy.EXACT, 12);
        List<ResourceResponse> resources = namedResources("a", "b", "c");
        // Only accept a single resource
        given(mockHandler.handleResource(any(ResourceResponse.class))).willReturn(false);

        // When
        for (ResourceResponse resource : resources) {
            sortingHandler.handleResource(resource);
        }
        QueryResponse result = sortingHandler.getResult(queryResult);

        // Then
        assertThat(result.getPagedResultsCookie()).isEqualTo(cookie);
        // FIXME: the following method needs to be reinstated:
        //assertThat(result.getRemainingPagedResults()).isEqualTo(resources.size() - 1);
    }

    private List<ResourceResponse> namedResources(String...names) {
        List<ResourceResponse> resources = new ArrayList<>(names.length);

        for (String name : names) {
            resources.add(newResourceResponse(name, null, json(object(field("name", name)))));
        }

        return resources;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceResponse> nameAgeResources(Map.Entry... entries) {
        List<ResourceResponse> resources = new ArrayList<>(entries.length);

        for (Map.Entry<String, Object> entry : entries) {
            resources.add(newResourceResponse(entry.getKey(), null, json(object(
                    field("name", entry.getKey()),
                    field("age", entry.getValue())
            ))));
        }

        return resources;
    }
}
