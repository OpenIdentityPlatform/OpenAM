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

import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.Reject;

/**
 * Adds support for paging to a query resource handler.
 *
 * @since 12.0.0
 */
final class PagingQueryResponseHandler extends QueryResponseHandler {
    private final QueryResourceHandler delegate;
    private final int startOffset;
    private final int endOffset;
    private int currentOffset = 0;

    PagingQueryResponseHandler(final QueryResourceHandler delegate, final int pageSize,
            final int pageOffset) {
        super(delegate);
        Reject.ifNull(delegate);
        Reject.ifTrue(pageSize <= 0, "Page size must be positive");
        Reject.ifTrue(pageOffset < 0, "Page offset must be positive");

        this.delegate = delegate;
        this.startOffset = pageOffset;
        this.endOffset = pageOffset + pageSize;
    }

    @Override
    public boolean handleResource(ResourceResponse resource) {
        // By default we will keep going until we reach the end offset of this page...
        boolean needMore = currentOffset < endOffset - 1;
        if (currentOffset >= startOffset && currentOffset < endOffset) {
            // ...but allow the downstream handle to terminate early
            needMore = delegate.handleResource(resource) && needMore;
        }
        currentOffset++;
        return needMore;
    }
}
