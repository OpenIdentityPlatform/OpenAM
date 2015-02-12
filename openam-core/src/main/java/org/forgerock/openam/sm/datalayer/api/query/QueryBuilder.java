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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.api.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Reject;

import com.sun.identity.shared.debug.Debug;

/**
 * Fluent class responsible for constructing queries for the data store.
 *
 * This class will handle the details around preparing a query and executing the query,
 * including processing the return results.
 *
 * Uses Token as its main means of expressing the data returned from the data store and so is
 * intended for use with the Core Token Service.
 * @param <C> The type of connection that will be used.
 * @param <F> The type of filter (if any) that will be supported.
 */
public abstract class QueryBuilder<C, F> {
    // Injected
    protected final Debug debug;

    protected String[] requestedAttributes = new String[]{};
    protected int sizeLimit;
    protected F filter;
    protected int pageSize;

    /**
     * Default constructor.
     */
    public QueryBuilder(Debug debug) {
        this.debug = debug;

        sizeLimit = 0;
        pageSize = 0;
    }

    /**
     * Limit the number of results returned from this query to the given amount.
     *
     * @param maxSize Positive number, zero indicates no limit.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder<C, F> limitResultsTo(int maxSize) {
        sizeLimit = maxSize;
        return this;
    }

    /**
     * The search results can be paged, in which case successive calls to the iterator's next method will return each
     * page.
     *
     * @param pageSize The size of each page of results.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder<C, F> pageResultsBy(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Indicates that the QueryBuilder is paging the search results, and subsequent calls are required
     * to collect the rest of the results.
     *
     * @return True if paging has been initialised, false if not.
     */
    public boolean isPagingResults() {
        return pageSize != 0;
    }

    /**
     * Limit the search to return only the named attributes.
     *
     * @param returnFields Array of CoreTokenField which are required in the results.
     * @return The QueryBuilder instance.
     * @throws IllegalArgumentException If array was null or empty.
     */
    public QueryBuilder<C, F> returnTheseAttributes(CoreTokenField... returnFields) {
        Reject.ifTrue(returnFields == null || returnFields.length == 0);

        Set<String> attributes = new HashSet<String>();
        for (CoreTokenField field : returnFields) {
            attributes.add(field.toString());
        }
        return setReturnAttributes(attributes);
    }

    /**
     * Limit the search to return only the named attributes.
     *
     * @param returnFields Collection of CoreTokenField that are required in the search results.
     * @return The QueryBuilder instance.
     * @throws IllegalArgumentException If the requested fields were null or empty.
     */
    public QueryBuilder<C, F> returnTheseAttributes(Collection<CoreTokenField> returnFields) {
        Reject.ifTrue(returnFields == null || returnFields.isEmpty());

        Set<String> fields = new HashSet<String>();
        for (CoreTokenField field : returnFields) {
            fields.add(field.toString());
        }
        return setReturnAttributes(fields);
    }

    private QueryBuilder<C, F> setReturnAttributes(Set<String> fields) {
        requestedAttributes = fields.toArray(new String[fields.size()]);
        return this;
    }

    /**
     * Assign a filter to the query. This can be a complex filter and is handled
     * by the QueryFilter class.
     *
     * @see QueryFilter For more details on generating a filter.
     *
     * @param filter An OpenDJ SDK Filter to assign to the query.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder<C, F> withFilter(F filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Perform an Attribute based query against the persistent store.
     *
     * An attribute query is one where we are interested in specific attributes from the Tokens
     * rather than all attributes assigned to the matched Tokens. This can be more performant
     * than requesting back all attributes of a Token first.
     *
     * @param connection The connection used to perform the request.
     *
     * @return An iterator of non null but possibly empty collections. If paging is not requested, the iterator will
     * have one entry in it. Note that the next method may throw a DataLayerRuntimeException in the event of a failure.
     */
    public Iterator<Collection<PartialToken>> executeAttributeQuery(C connection) {
        return executeRawResults(connection, PartialToken.class);
    }

    /**
     * Creates an iterator that contains pages of results (if paging not requested, one page is returned). It is
     * recommended that the query itself is performed in the iterator rather than in the implementation of this method.
     * @param connection The connection to query.
     * @param returnType The type of object wanted.
     * @param <T> The type being returned.
     * @return An iterator of collections of objects.
     */
    public abstract <T> Iterator<Collection<T>> executeRawResults(C connection, Class<T> returnType);

    /**
     * Perform the query and return the results as processed Token instances.
     *
     * @param connection The connection used to perform the request.
     *
     * @return An iterator of non null but possibly empty collections. If paging is not requested, the iterator will
     * have one entry in it. Note that the next method may throw a DataLayerRuntimeException in the event of a failure.
     */
    public Iterator<Collection<Token>> execute(C connection) {
        if (!ArrayUtils.isEmpty(requestedAttributes)) {
            throw new IllegalStateException(
                    "Cannot convert results to Token if the query uses" +
                    "a reduced number of attributes in the return result");
        }
        return executeRawResults(connection, Token.class);
    }
}
