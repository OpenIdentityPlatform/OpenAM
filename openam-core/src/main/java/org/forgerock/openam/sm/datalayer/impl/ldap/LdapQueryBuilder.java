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

package org.forgerock.openam.sm.datalayer.impl.ldap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerRuntimeException;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.SimplePagedResultsControl;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;

public class LdapQueryBuilder extends QueryBuilder<Connection, Filter> {

    // Represents the start and end state of the paged query.
    public static final ByteString EMPTY = getEmptyPagingCookie();

    private final LdapDataLayerConfiguration dataLayerConfiguration;
    private final LdapSearchHandler handler;
    private final Map<Class, EntryConverter> converterMap;
    protected ByteString pagingCookie;

    /**
     * Default constructor ensures the Object Class is defined.
     *
     * @param dataLayerConfiguration Required for data store dataLayerConfiguration.
     */
    @Inject
    public LdapQueryBuilder(LdapDataLayerConfiguration dataLayerConfiguration, LdapSearchHandler handler,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug,
            Map<Class, EntryConverter> converterMap) {
        super(debug);
        this.dataLayerConfiguration = dataLayerConfiguration;
        this.handler = handler;
        this.converterMap = converterMap;
    }

    /**
     * Perform the query and return the results as Entry instances.
     *
     * @param connection The connection used to perform the request.
     *
     * @return A non null but possibly empty collection.
     *
     * @throws org.forgerock.openam.cts.exceptions.QueryFailedException If there was an error during the query.
     */
    public <T> Iterator<Collection<T>> executeRawResults(Connection connection, Class<T> returnType) {
        if (String.class.equals(returnType) && requestedAttributes.length != 1) {
            throw new IllegalArgumentException("String return type wanted but more than 1 attribute requested");
        }
        EntryConverter<T> entryConverter = (EntryConverter<T>) converterMap.get(returnType);
        if (entryConverter == null) {
            throw new IllegalArgumentException("Cannot convert LDAP Entry objects to " + returnType.getName());
        }
        return new EntryIterator<T>(connection, entryConverter);
    }

    private Collection<Entry> getEntries(Connection connection) throws CoreTokenException {
        // Prepare the search
        Filter ldapFilter = getLDAPFilter();
        SearchRequest searchRequest = Requests.newSearchRequest(
                dataLayerConfiguration.getTokenStoreRootSuffix(),
                SearchScope.WHOLE_SUBTREE,
                ldapFilter,
                requestedAttributes);
        searchRequest.setSizeLimit(sizeLimit);

        if (isPagingResults()) {
            searchRequest = searchRequest.addControl(SimplePagedResultsControl.newControl(true, pageSize, pagingCookie));
        }

        // Perform the search
        Collection<Entry> entries = createResultsList();
        final Result result = handler.performSearch(connection, searchRequest, entries);

        if (isPagingResults()) {
            try {
                SimplePagedResultsControl control = result.getControl(
                        SimplePagedResultsControl.DECODER, new DecodeOptions());
                pagingCookie = control.getCookie();
            } catch (DecodeException e) {
                throw new CoreTokenException("Failed to decode Paging Cookie", e);
            }
        }

        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                            "Query: matched {0} results\n" +
                            "Search Request: {1}\n" +
                            "Filter: {2}\n" +
                            "Result: {3}",
                    entries.size(),
                    searchRequest,
                    ldapFilter.toString(),
                    result));
        }

        return entries;
    }

    /**
     * @return Creates a list based on the state of the builder.
     */
    private <T> Collection<T> createResultsList() {
        Collection<T> entries;
        if (isPagingResults()) {
            entries = new ArrayList<T>(pageSize);
        } else if (sizeLimit != 0) {
            entries = new ArrayList<T>(sizeLimit);
        } else {
            entries = new ArrayList<T>();
        }
        return entries;
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

    /**
     * Generates an empty Paging Cookie.
     *
     * The Paging Cookie is required for paging requests. In order to use the
     * paging function, an empty cookie is passed in initially and must be
     * provided with each subsequent call.
     *
     * The details of this are managed by the ReaperIterator.
     *
     * @see org.forgerock.openam.cts.impl.query.reaper.ReaperQuery
     *
     * @return Non null empty ByteString.
     */
    static ByteString getEmptyPagingCookie() {
        return ByteString.empty();
    }

    private class EntryIterator<T> implements Iterator<Collection<T>> {

        private final EntryConverter<T> converter;
        private Connection connection;

        EntryIterator(Connection connection, EntryConverter<T> converter) {
            this.converter = converter;
            this.connection = connection;
        }

        @Override
        public boolean hasNext() {
            return pagingCookie == null || pagingCookie.length() != EMPTY.length();
        }

        @Override
        public Collection<T> next() {
            // The search results can be paged by using the paging cookie.
            if (isPagingResults()) {
                pagingCookie = getEmptyPagingCookie();
            }

            Collection<Entry> entries = null;
            try {
                entries = getEntries(connection);
            } catch (CoreTokenException e) {
                throw new DataLayerRuntimeException("Could not get entries from connection", e);
            }
            Collection<T> results = createResultsList();

            for (Entry entry : entries) {
                results.add(converter.convert(entry, requestedAttributes));
            }

            if (debug.messageEnabled()) {

                StringBuilder msg = new StringBuilder();
                char separator = '\n';

                for (T t : results) {
                    msg.append(t).append(separator);
                    // Ensure we don't fill up the logs
                    if (msg.length() > 500) break;
                }

                if (msg.length() > 0 && msg.charAt(msg.length() - 1) == separator) {
                    msg = msg.deleteCharAt(msg.length() - 1);
                }

                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                                "Query: Matched {0}, some Tokens are shown below:\n" +
                                "{1}",
                        results.size(),
                        msg.toString()));
            }

            return results;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
