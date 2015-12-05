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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QueryResponsePresentationTest {

    private QueryResourceHandler mockHandler;
    private ArgumentCaptor<ResourceResponse> captor;

    @BeforeMethod
    public void setup() {
        mockHandler = mock(QueryResponseHandler.class);
        captor = ArgumentCaptor.forClass(ResourceResponse.class);

        given(mockHandler.handleResource(captor.capture())).willReturn(true);
    }

    // Handler tests

    @Test
    public void shouldHandleEachQueryItem() throws Exception {
        QueryResponsePresentation.perform(mockHandler, makeEmptyQueryRequest(), makeResourceResponses("abc,def,ghj"));
        verify(mockHandler, times(3)).handleResource(any(ResourceResponse.class));
    }

    @Test
    public void shouldHonourQueryResourceHandler() {
        given(mockHandler.handleResource(captor.capture())).willReturn(false);
        QueryResponsePresentation.perform(mockHandler, makeEmptyQueryRequest(), makeResourceResponses("abc,def,ghj"));
        verify(mockHandler, times(1)).handleResource(any(ResourceResponse.class)); // handler called first time only.
    }

    // Paging Tests

    @Test
    public void shouldHandlePageOfResults() throws Exception {
        QueryRequest request = makePagedQueryRequest(1, 0); // page size 1
        QueryResponsePresentation.perform(mockHandler, request, makeResourceResponses("abc,def,ghj"));
        assertThat(extractId(captor.getAllValues(), 0)).isEqualTo("abc");
    }

    @Test
    public void shouldHandlePagingOffset()  {
        QueryRequest request = makePagedQueryRequest(1, 1);
        QueryResponsePresentation.perform(mockHandler, request, makeResourceResponses("abc,def,ghj"));
        assertThat(extractId(captor.getAllValues(), 0)).isEqualTo("def");
    }

    @Test
    public void shouldHandlePagingOffset2()  {
        QueryResponsePresentation.perform(mockHandler, makePagedQueryRequest(1, 2), makeResourceResponses("abc,def,ghj"));
        assertThat(captor.getAllValues().size()).isEqualTo(1);
        assertThat(captor.getValue().getId()).isEqualTo("ghj");
    }

    @Test
    public void shouldOnlyUseOffsetIfPageSizeProvided()  {
        QueryRequest request = makePagedQueryRequest(0, 1);
        QueryResponsePresentation.perform(mockHandler, request, makeResourceResponses("abc,def,ghj"));
        assertThat(extractId(captor.getAllValues(), 0)).isNotEqualTo("def");
    }

    @Test
    public void shouldNotApplyPagingIfNoPagingDetailsProvided() {
        QueryRequest request = makePagedQueryRequest(0, 0);
        QueryResponsePresentation.perform(mockHandler, request, makeResourceResponses("abc,def,ghj"));
        assertThat(captor.getAllValues().size()).isEqualTo(3);
    }

    @Test
    public void shouldIgnoreNegativeOffsetDuringPagingRequest() {
        QueryRequest request = makePagedQueryRequest(1, -1);
        QueryResponsePresentation.perform(mockHandler, request, makeResourceResponses("abc,def,ghj"));
        assertThat(extractId(captor.getAllValues(), 0)).isEqualTo("abc");
    }

    // Query Response

    @Test
    public void shouldMarkPagingPolicyAsEXACTWhenPagingDetailsProvided() throws ResourceException {
        QueryRequest pagedRequest = makePagedQueryRequest(1, 0);
        Promise<QueryResponse, ResourceException> result =
                QueryResponsePresentation.perform(mockHandler, pagedRequest, makeResourceResponses("abc,def,ghj"));
        assertThat(result.getOrThrowUninterruptibly().getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.EXACT);
    }

    @Test
    public void shouldCountTotalPagedResultsFromQuery() throws ResourceException {
        QueryRequest pagedRequest = makePagedQueryRequest(1, 0);
        Promise<QueryResponse, ResourceException> result =
                QueryResponsePresentation.perform(mockHandler, pagedRequest, makeResourceResponses("abc,def,ghj"));
        assertThat(result.getOrThrowUninterruptibly().getTotalPagedResults()).isEqualTo(3);
    }

    @Test
    public void shouldCountRemainingPagedResultsFromQueryUsingDeprecatedFlag() throws ResourceException {
        QueryRequest deprecatedRequest = makeDeprecatedQueryRequest(1, 0);
        Promise<QueryResponse, ResourceException> result =
                QueryResponsePresentation.perform(mockHandler, deprecatedRequest, makeResourceResponses("abc,def,ghj"));
        assertThat(result.getOrThrowUninterruptibly().getRemainingPagedResults()).isEqualTo(2);
    }

    @Test
    public void shouldCountRemainingPagedResultsWhenPageOffsetSpecifiedAndDeprecatedEnabled() throws ResourceException {
        QueryRequest deprecatedRequest = makeDeprecatedQueryRequest(1, 1);
        Promise<QueryResponse, ResourceException> result =
                QueryResponsePresentation.perform(mockHandler, deprecatedRequest, makeResourceResponses("abc,def,ghj"));
        assertThat(result.getOrThrowUninterruptibly().getRemainingPagedResults()).isEqualTo(1);
    }

    // Sorting Tests

    @Test
    public void shouldSortResultsBeforeHandling()  {
        QueryRequest sortedRequest = makeSortedQueryRequest("^name");
        QueryResponsePresentation.perform(mockHandler, sortedRequest, makeResourceResponses("ghj,def,abc"));
        assertThat(extractId(captor.getAllValues(), 0)).isEqualTo("abc");
    }

    @Test
    public void shouldNotSortResultsOnUncomparableObjects()  {
        QueryRequest sortedRequest = makeSortedQueryRequest("^name");

        List<ResourceResponse> responseList = new ArrayList<>();
        responseList.add(Responses.newResourceResponse("a", null, JsonValueBuilder.jsonValue()
                .put("name", Collections.singletonMap("key2", "value2"))
                .put("place", "a").build()
        ));
        responseList.add(Responses.newResourceResponse("z", null, JsonValueBuilder.jsonValue()
                .put("name", Collections.singletonMap("key1", "value1"))
                .put("place", "z").build()
        ));

        QueryResponsePresentation.perform(mockHandler, sortedRequest, responseList);
        assertThat(extractId(captor.getAllValues(), 0)).isEqualTo("a");
    }

    @Test
    public void shouldSortResultsUsingMultiLevelSort() {
        QueryRequest multiLevelRequest = makeSortedQueryRequest("^name", "^place");
        QueryResponsePresentation.perform(mockHandler, multiLevelRequest, makeResourceResponses("weasel->forest,badger->town,badger->village"));
        assertThat(captor.getAllValues().get(0).getContent().get("name").asString()).isEqualTo("badger");
        assertThat(captor.getAllValues().get(0).getContent().get("place").asString()).isEqualTo("town");
    }

    @Test
    public void shouldSortResultsUsingMultiLevelWithDescending() {
        QueryRequest descendingRequest = makeSortedQueryRequest("^name", "place");
        QueryResponsePresentation.perform(mockHandler, descendingRequest, makeResourceResponses("weasel->forest,badger->town,badger->village"));
        assertThat(captor.getAllValues().get(0).getContent().get("name").asString()).isEqualTo("badger");
        assertThat(captor.getAllValues().get(0).getContent().get("place").asString()).isEqualTo("village");
    }

    @Test
    public void shouldLeaveResultsUnsortedIfErrorDuringSorting() {
        QueryRequest incorrectRequest = makeSortedQueryRequest("^wibble");
        QueryResponsePresentation.perform(mockHandler, incorrectRequest, makeResourceResponses("weasel->forest,badger->town,badger->village"));
        assertThat(captor.getAllValues().get(0).getContent().get("name").asString()).isEqualTo("weasel");
    }

    @Test
    public void shouldSortUsingNullValues()  {
        QueryRequest sortedRequest = makeSortedQueryRequest("^name");
        QueryResponsePresentation.perform(mockHandler, sortedRequest, makeResourceResponses("NULL,ferret,badger"));
        assertThat(extractId(captor.getAllValues(), 0)).isNull();
    }

    @Test
    public void shouldSortMultiLevelUsingNullValues()  {
        QueryRequest sortedRequest = makeSortedQueryRequest("name", "^place");
        QueryResponsePresentation.perform(mockHandler, sortedRequest, makeResourceResponses("weasel->NULL,weasel->woods,badger->forrest"));
        assertThat(extractId(captor.getAllValues(), 0)).isEqualTo("weasel");
        assertThat(captor.getAllValues().get(0).getContent().get("place").asString()).isNull();
    }

    @Test
    public void shouldSortMultiLevelUsingNullValues2()  {
        QueryRequest sortedRequest = makeSortedQueryRequest("name", "place");
        QueryResponsePresentation.perform(mockHandler, sortedRequest, makeResourceResponses("weasel->NULL,weasel->woods,badger->forrest"));
        assertThat(extractId(captor.getAllValues(), 0)).isEqualTo("weasel");
        assertThat(captor.getAllValues().get(0).getContent().get("place").asString()).isEqualTo("woods");
    }

    private static QueryRequest makeEmptyQueryRequest() {
        return mock(QueryRequest.class);
    }

    private static QueryRequest makePagedQueryRequest(int size, int offset) {
        QueryRequest request = makeEmptyQueryRequest();
        given(request.getPageSize()).willReturn(size);
        given(request.getPagedResultsOffset()).willReturn(offset);
        return request;
    }

    private QueryRequest makeDeprecatedQueryRequest(int pageSize, int offset) {
        QueryRequest mockRequest = makePagedQueryRequest(pageSize, offset);
        given(mockRequest.getAdditionalParameter("Remaining")).willReturn("true");
        return mockRequest;
    }

    /**
     * Make a QueryRequest based on a simple string encoding:
     * [^][key]
     *
     * caret - Indicates ascending sort, when missing indicates descending.
     * key - The name of the field to sort on.
     */
    private static QueryRequest makeSortedQueryRequest(String... keys) {
        QueryRequest request = makeEmptyQueryRequest();
        List<SortKey> sortKeys = new ArrayList<>();
        for (String key : keys) {
            if (key.startsWith("^")) {
                sortKeys.add(SortKey.ascendingOrder(key.substring(1)));
            } else {
                sortKeys.add(SortKey.descendingOrder(key));
            }
        }
        given(request.getSortKeys()).willReturn(sortKeys);
        return request;
    }

    /**
     * A simple mapping function that takes encoded strings and converts them
     * to ResourceResponses.
     *
     * Format:
     *
     * [name]->[place],
     *
     * or simpler:
     *
     * [name],
     *
     * The resultant object will contain a JsonValue of the schema:
     *
     * <code>
     * {
     *   name: "name"
     *   place: "place"
     * }
     * </code>
     *
     * The symbol NULL will be replaced with null. The id of the resource is set to name.
     */
    private static List<ResourceResponse> makeResourceResponses(String raw) {
        List<ResourceResponse> responses = new ArrayList<>();
        for (String item : raw.split(",")) {
            String name;
            String place;
            if (item.contains("->")) {
                String[] part = item.split("->");
                name = part[0];
                place = part[1];
            } else {
                name = item;
                place = "";
            }

            if ("NULL".equalsIgnoreCase(name)) {
                name = null;
            }
            if ("NULL".equalsIgnoreCase(place)) {
                place = null;
            }

            JsonValue value  = JsonValueBuilder.jsonValue().put("name", name).put("place", place).build();
            responses.add(Responses.newResourceResponse(name, null, value));
        }

        return responses;
    }

    private static String extractId(List<ResourceResponse> results, int position) {
        return results.get(position).getId();
    }
}