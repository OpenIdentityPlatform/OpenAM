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

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SortKey;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Provides buffered in-memory sorting of results for cases where the backend of a CREST service does not natively
 * support sorting. All results are buffered in memory and sorted according to the given sort criteria. All sorted
 * results are then delivered to the underlying query handler on query completion.
 *
 * @since 12.0.0
 */
final class SortingQueryResultHandler implements QueryResultHandler {
    private static final int INITIAL_QUEUE_SIZE = 11;
    private final QueryResultHandler delegate;
    private final PriorityQueue<Resource> sortedResources;

    SortingQueryResultHandler(final QueryResultHandler delegate, final List<SortKey> sortKeys) {
        Reject.ifNull(delegate, sortKeys);
        Reject.ifTrue(sortKeys.isEmpty(), "No sort keys specified");
        this.delegate = delegate;
        this.sortedResources = new PriorityQueue<Resource>(INITIAL_QUEUE_SIZE, new SortKeyComparator(sortKeys));
    }

    @Override
    public void handleError(final ResourceException error) {
        delegate.handleError(error);
    }

    @Override
    public boolean handleResource(Resource resource) {
        sortedResources.add(resource);
        return true;
    }

    @Override
    public void handleResult(QueryResult result) {
        // Drain all resources from the queue into the delegate result handler (in sorted order)
        boolean keepGoing = true;
        while (keepGoing && !sortedResources.isEmpty()) {
            keepGoing = delegate.handleResource(sortedResources.poll());
        }
        delegate.handleResult(new QueryResult(result.getPagedResultsCookie(), sortedResources.size()));
    }

    /**
     * Comparator for sorting resources based on an ordered list of sort keys. Elements are compared according to the
     * first sort key in the list. If two elements compare as equal then subsequent sort keys are used for the
     * comparison. All comparisons are currently done using case-sensitive string comparison.
     */
    private static final class SortKeyComparator implements Comparator<Resource> {
        private final List<SortKey> sortKeys;

        SortKeyComparator(final List<SortKey> sortKeys) {
            this.sortKeys = sortKeys != null ? Collections.unmodifiableList(sortKeys)
                                             : Collections.<SortKey>emptyList();
        }

        @Override
        public int compare(final Resource a, final Resource b) {
            int cmp = 0;

            for (SortKey key : sortKeys) {
                String aField = field(a, key.getField());
                String bField = field(b, key.getField());

                cmp = aField == null ? bField == null ? 0 : -1
                                     : aField.compareTo(bField);

                if (!key.isAscendingOrder()) {
                    cmp = -cmp; // Reverse comparison for descending order
                }

                // If we have found a difference then do not bother with further comparisons
                if (cmp != 0) {
                    break;
                }
            }

            return cmp;
        }

        /**
         * Extracts the given field from the given resource as a string. Non-string fields will be converted to strings
         * via the toString method. If any value on the given path is null then a null is returned.
         *
         * @param resource the resource to extract the field from.
         * @param field the path of the field to extract from the JSON content.
         * @return the value of the given field as a string, or null if any component of the path is null.
         */
        private String field(Resource resource, JsonPointer field) {
            String result = null;
            JsonValue json = resource.getContent();
            if (json != null && !json.isNull()) {
                JsonValue fieldValue = json.get(field);
                if (fieldValue != null && !fieldValue.isNull()) {
                    result = fieldValue.getObject().toString();
                }
            }
            return result;
        }
    }
}
