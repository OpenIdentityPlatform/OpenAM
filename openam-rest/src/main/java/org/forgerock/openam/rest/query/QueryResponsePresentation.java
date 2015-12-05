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

import static org.forgerock.json.resource.Responses.*;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Responsible for managing the presentation detail around a CREST query and its
 * response to the caller.
 *
 * Resource fields MUST be {@link Comparable} for them to be used as sorting keys.
 *
 * This class supports CREST query operations by providing paging and sorting of
 * results. Sorting is always performed before paging.
 *
 * This implementation also defers responsibility to the caller for details about
 * how to sort the item via a {@link Comparator} and how to convert the item to
 * a {@link ResourceResponse}.
 */
public class QueryResponsePresentation {

    public static final String REMAINING = "Remaining";

    // Static utility class.
    private QueryResponsePresentation() {

    }

    /**
     * Given a set of JsonValue results from a query, convert them to the required {@link ResourceResponse}
     * and apply them to the given handler.
     *
     * This function implicitly supports paging and sorting, based on the values supplied in the
     * {@link QueryRequest}.
     *
     * The QueryResponse is wrapped in the appropriate Promise for convenience. The response will indicate
     * either the total number of results, or the remaining depending on the request.
     * See {@link #enableDeprecatedRemainingQueryResponse(QueryRequest)} for details.
     *
     * @param handler Non null QueryResourceHandler which will receive the results from the query.
     * @param request Non null QueryRequest required to determine how results should be processed before returning.
     * @param resources Non null, possibly empty list of ResourceResponse results from a query.
     * @return Non null Promise containing the QueryResponse which includes a count of the results remaining.
     */
    public static Promise<QueryResponse, ResourceException> perform(QueryResourceHandler handler, QueryRequest request,
                                                             List<ResourceResponse> resources) {

        if (isSortingRequested(request)) {
            resources = sortItems(request, resources);
        }

        if (isPagingRequested(request)) {
            handler = createPagingHandler(handler, request);
        }

        int handledCount = handleResources(handler, resources);
        QueryResponse response = generateQueryResponse(request, resources.size(), handledCount);
        return Promises.newResultPromise(response);
    }

    /**
     * CREST 2.0 based resources use the convention of returning the remaining count of
     * resources to the caller using {@link QueryResponse#getRemainingPagedResults()}. Use
     * this method to indicate the QueryResponsePresentation that it should enable this mode.
     *
     * For reference, CREST 3.0 resources should use {@link QueryResponse#getTotalPagedResults()} instead.
     *
     * @param request Non null QueryRequest to update, indicating CREST 2.0 compatibility mode.
     */
    public static void enableDeprecatedRemainingQueryResponse(QueryRequest request) {
        try {
            request.setAdditionalParameter(REMAINING, "true");
        } catch (BadRequestException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Validate the QueryRequest and determine if we have enough information to provide
     * paging for the caller.
     *
     * @param handler Non null handler to wrap.
     * @param request Non null QueryRequest possibly with page size and offset defined.
     * @return Non null wrapped QueryResponseHandler.
     */
    private static QueryResponseHandler createPagingHandler(QueryResourceHandler handler, QueryRequest request) {
        if (isPagingRequested(request)) {
            return new PagingQueryResponseHandler(
                    new QueryResponseHandler(handler),
                    Math.max(0, request.getPageSize()),
                    Math.max(0, request.getPagedResultsOffset()));
        } else {
            return new QueryResponseHandler(handler);
        }
    }

    /**
     * If we detect we need to operate in CREST 2.0 compatibility mode, then return the
     * remaining number of items in the query.
     *
     * Otherwise, the response will always contain the total results available.
     *
     * @see QueryResponsePresentation#enableDeprecatedRemainingQueryResponse(QueryRequest)
     */
    private static QueryResponse generateQueryResponse(QueryRequest request, int total, int handled) {
        if ("true".equalsIgnoreCase(request.getAdditionalParameter(REMAINING))) {
            return Responses.newRemainingResultsResponse(null, total - handled);
        } else {
            return Responses.newQueryResponse(null, CountPolicy.EXACT, total);
        }
    }

    /**
     * Will attempt to handle each resource in sequence until the handler indicates they are complete.
     */
    private static int handleResources(QueryResourceHandler handler, List<ResourceResponse> resources) {
        int count = 0;
        for (ResourceResponse response : resources) {
            count++;
            boolean handled = handler.handleResource(response);
            if (!handled) {
                break;
            }
        }
        return count;
    }

    private static boolean isPagingRequested(QueryRequest request) {
        return request.getPageSize() > 0;
    }

    private static boolean isSortingRequested(QueryRequest request) {
        return !CollectionUtils.isEmpty(request.getSortKeys());
    }

    private static List<ResourceResponse> sortItems(final QueryRequest request, List<ResourceResponse> items) {
        Comparator<ResourceResponse> comparator = new Comparator<ResourceResponse>() {
            final List<SortKey> sortKeys = request.getSortKeys();
            @Override
            public int compare(ResourceResponse o1, ResourceResponse o2) {
                int compare = 0;
                for (SortKey key : sortKeys) {
                    Comparable<Object> v1 = getField(o1, key.getField());
                    Comparable<Object> v2 = getField(o2, key.getField());

                    compare = compare(v1, v2, key.isAscendingOrder());
                    if (compare != 0) {
                        break;
                    }
                }
                return compare;
            }

            private int compare(Comparable<Object> first, Comparable<Object> second, boolean ascending) {
                int result;
                if (first == null && second == null) {
                    result = 0;
                } else if (first == null) {
                    result = -1;
                } else if (second == null) {
                    result = 1;
                } else {
                    result = first.compareTo(second);
                }

                if (!ascending) {
                    result = -result;
                }
                return result;
            }

            /**
             * Gets the comparable field value from a resource.
             *
             * @param resource The resource.
             * @param field The field.
             * @return The comparable field value or {@code null} if the field is empty or the
             * field value is not comparable.
             */
            private Comparable<Object> getField(ResourceResponse resource, JsonPointer field) {
                JsonValue value = resource.getContent();
                if (value.get(field).isNull()) {
                    return null;
                }
                Object object = value.get(field).getObject();
                if (isComparable(object)) {
                    return asComparable(object);
                } else {
                    return null;
                }
            }

            private boolean isComparable(Object o) {
                return o instanceof Comparable;
            }

            @SuppressWarnings("unchecked")
            private Comparable<Object> asComparable(Object o) {
                return (Comparable<Object>) o;
            }
        };

        try {
            Collections.sort(items, comparator);
        } catch (NullPointerException e) {
            // No-op, sorting failed.
        }
        return Collections.unmodifiableList(items);
    }
}
