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

import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.SortKey;
import org.forgerock.util.Reject;

import java.util.List;

/**
 * Wraps a {@link org.forgerock.json.resource.QueryResultHandler} in decorator implementations to add support for
 * sorting, paging etc.
 *
 * @since 12.0.0
 */
public final class QueryResultHandlerBuilder {
    private final QueryResultHandler handler;

    public QueryResultHandlerBuilder(QueryResultHandler handler) {
        Reject.ifNull(handler);
        this.handler = handler;
    }

    public QueryResultHandlerBuilder withSorting(List<SortKey> sortKeys) {
        // If no sort keys specified then just return the existing handler, otherwise add sorting
        return sortKeys == null || sortKeys.isEmpty() ? this
                : new QueryResultHandlerBuilder(new SortingQueryResultHandler(handler, sortKeys));
    }

    public QueryResultHandlerBuilder withPaging(int pageSize, int pageOffset) {
        return pageSize <= 0 ? this
                : new QueryResultHandlerBuilder(new PagingQueryResultHandler(handler, pageSize, pageOffset));
    }

    public QueryResultHandler build() {
        return handler;
    }

    public static QueryResultHandler withPagingAndSorting(QueryResultHandler initialHandler, QueryRequest request) {
        return new QueryResultHandlerBuilder(initialHandler)
                .withPaging(request.getPageSize(), request.getPagedResultsOffset())
                // Always add sorting after paging, otherwise paging will not be based on the sorted list
                .withSorting(request.getSortKeys())
                .build();
    }
}
