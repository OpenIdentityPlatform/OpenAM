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
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Entry;

import java.util.Collection;
import java.util.Iterator;

/**
 * Responsible for providing an Iterator which manages the process of querying pages of results
 * from the Directory.
 *
 * This implementation requires a pre-configured QueryBuilder which describes the query to perform.
 * From there it will coordinate the query with the Directory making use of the 'Paging Cookie'
 * to ensure that all results are correctly returned.
 *
 * @author robert.wapshott@forgerock.com
 */
public class QueryPageIterator implements Iterator<Collection<Entry>> {
    private final QueryBuilder builder;
    private final int pageSize;

    private ByteString cookie;
    private boolean firstCall = true;

    /**
     * Generate an instance of a Query Page Iterator.
     *
     * @param builder The QueryBuilder instance to apply the paging to.
     * @param pageSize The size of the results page to return with each call.
     */
    public QueryPageIterator(QueryBuilder builder, int pageSize) {
        if (pageSize < 0) {
            throw new IllegalArgumentException("PageSize must be positive");
        }

        this.builder = builder;
        this.pageSize = pageSize;
        cookie = QueryBuilder.getEmptyPagingCookie();
    }

    /**
     * Perform the query and collect the paging cookie.
     * @return Non null, maybe empty.
     * @throws CoreTokenException Unexpected.
     */
    private Collection<Entry> queryPage() throws CoreTokenException {
        builder.pageResultsBy(pageSize, cookie);
        Collection<Entry> entries = builder.executeRawResults();
        cookie = builder.getPagingCookie();
        return entries;
    }

    /**
     * Indicates there are further results to query.
     *
     * Note: This value is initially true, because the signal to indicate that the search has
     * ended is the same signal as the initialisation state.
     *
     * @return True if there are more results to collect by the query.
     */
    public boolean hasNext() {
        if (firstCall) {
            return true;
        }
        return cookie.length() != QueryBuilder.getEmptyPagingCookie().length();
    }

    /**
     * Query the QueryBuilder and collect the results from the query.
     *
     * @return A non null collection of results from the query.
     * @throws IllegalStateException If there was an error during the query.
     */
    public Collection<Entry> next() {
        if (firstCall) {
            firstCall = false;
        }
        try {
            return queryPage();
        } catch (CoreTokenException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Unsupported.
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
    }
}