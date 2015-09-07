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

import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceResponse;

/**
 * Base Query Response handler for paging and sorting.
 *
 * @since 13.0.0
 */
public class QueryResponseHandler implements QueryResourceHandler {

    private final QueryResourceHandler delegate;

    QueryResponseHandler(QueryResourceHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean handleResource(ResourceResponse resource) {
        return delegate.handleResource(resource);
    }

    /**
     * Will get the paged and/or sorted details of the query response.
     *
     * @param result The {@code QueryResponse}.
     * @return The paged and/or sorted {@code QueryResponse}.
     */
    QueryResponse getResult(QueryResponse result) {
        return result;
    }
}
