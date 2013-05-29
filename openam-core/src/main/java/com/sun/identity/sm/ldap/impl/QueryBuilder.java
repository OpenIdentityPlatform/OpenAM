/**
 * Copyright 2013 ForgeRock, Inc.
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
package com.sun.identity.sm.ldap.impl;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.fields.CoreTokenField;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.utils.TokenAttributeConversion;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Fluent class responsible for constructing queries for the LDAP data store.
 *
 * This class will handle the details around preparing a query and executing the query, including processing
 * the return results.
 *
 * Uses Token as its main means of expressing the data returned from LDAP and so is intended for use
 * with the Core Token Service.
 *
 * @author robert.wapshott@forgerock.com
 */
public class QueryBuilder {
    // Injected
    private final DataLayerConnectionFactory connectionFactory;
    private final Debug DEBUG;
    private final TokenAttributeConversion attributeConversion;
    private final CoreTokenConstants constants;
    private final LDAPSearchHandler handler;

    private String[] requestedAttributes = new String[]{};
    private int sizeLimit;
    private Filter filter;

    /**
     * Default constructor ensures the Object Class is defined.
     *
     * @param connectionFactory The connectionFactory to query.
     * @param attributeConversion Required for Token based conversions.
     * @param constants Required for system wide constants.
     */
    public QueryBuilder(DataLayerConnectionFactory connectionFactory, TokenAttributeConversion attributeConversion,
                        CoreTokenConstants constants) {
        this(connectionFactory,
             attributeConversion,
             constants,
             new LDAPSearchHandler(connectionFactory, constants),
             SessionService.sessionDebug);
    }

    /**
     * Constructor with all dependencies exposed.
     *
     * @param connectionFactory The connectionFactory to query.
     * @param attributeConversion Required for Token based conversions.
     * @param constants Required for system wide constants.
     * @param debug The debugging instance to debug to.
     */
    public QueryBuilder(DataLayerConnectionFactory connectionFactory,
                        TokenAttributeConversion attributeConversion, CoreTokenConstants constants,
                        LDAPSearchHandler handler, Debug debug) {
        this.connectionFactory = connectionFactory;
        this.attributeConversion = attributeConversion;
        this.constants = constants;
        this.handler = handler;
        sizeLimit = 0;
        this.DEBUG = debug;
    }

    /**
     * Limit the number of results returned from this query to the given amount.
     *
     * @param maxSize Positive number, zero indicates no limit.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder limitResultsTo(int maxSize) {
        sizeLimit = maxSize;
        return this;
    }

    /**
     * Limit the search to return only the named attributes.
     *
     * @param fields Collection of CoreTokenField that are required in the search results.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder returnTheseAttributes(CoreTokenField... fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Must supply at least one field");
        }

        List<String> attributes = new ArrayList<String>();
        for (CoreTokenField field : fields) {
            attributes.add(field.toString());
        }
        requestedAttributes = attributes.toArray(new String[attributes.size()]);
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
    public QueryBuilder withFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    private Filter getLDAPFilter() {
        Filter objectClass = Filter.equality(CoreTokenConstants.OBJECT_CLASS, CoreTokenConstants.FR_CORE_TOKEN);
        if (filter == null) {
            return objectClass;
        } else {
            return Filter.and(objectClass, filter);
        }
    }

    /**
     * Perform the query and return the results as unprocessed Entry instances.
     *
     * @return A non null but possibly empty collection.
     *
     * @throws com.sun.identity.sm.ldap.exceptions.QueryFailedException If there was an error during the query.
     */
    public Collection<Entry> executeRawResults() throws CoreTokenException {
        // Prepare the search
        Filter ldapFilter = getLDAPFilter();
        SearchRequest searchRequest = Requests.newSearchRequest(
                constants.getTokenDN(),
                SearchScope.WHOLE_SUBTREE,
                ldapFilter,
                requestedAttributes);
        searchRequest.setSizeLimit(sizeLimit);

        // Perform the search
        Collection<Entry> entries = handler.performSearch(searchRequest);

        if (DEBUG.messageEnabled()) {
            DEBUG.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Query: matched {0} results\n" +
                    "Search Request: {1}\n" +
                    "Filter: {2}",
                    entries.size(),
                    searchRequest,
                    ldapFilter.toString()));
        }

        return entries;
    }

    /**
     * Perform the query and return the results as processed Token instances.
     *
     * @return A non null but possibly empty collection.
     *
     * @throws com.sun.identity.sm.ldap.exceptions.QueryFailedException If there was an error during the query.
     */
    public Collection<Token> execute() throws CoreTokenException {
        if (!ArrayUtils.isEmpty(requestedAttributes)) {
            throw new IllegalStateException(
                    "Cannot convert results to Token if the query uses" +
                    "a reduced number of attributes in the return result");
        }

        List<Token> tokens = new LinkedList<Token>();
        for (Entry entry : executeRawResults()) {
            Token token = attributeConversion.tokenFromEntry(entry);
            tokens.add(token);
        }

        if (DEBUG.messageEnabled()) {

            String msg = "";
            String separator = "\n";

            for (Token t : tokens) {
                msg += t + separator;
                // Ensure we don't fill up the logs
                if (msg.length() > 500) break;
            }

            if (msg.endsWith(separator)) {
                msg = msg.substring(0, msg.length() - separator.length());
            }

            DEBUG.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Query: Matched {0}, some Tokens are shown below:\n" +
                    "{1}",
                    tokens.size(),
                    msg));
        }
        return tokens;
    }

    /**
     * Presents the QueryBuilder in a human readable format.
     *
     * Note: This function is not performant and should only be used for debugging purposes.
     *
     * @return Non null string representing this QueryBuilder.
     */
    @Override
    public String toString() {
        return MessageFormat.format(
                "Query:\n" +
                "      Filter: {0}\n" +
                "  Attributes: {1}",
                getLDAPFilter(),
                StringUtils.join(requestedAttributes, ", "));
    }
}
