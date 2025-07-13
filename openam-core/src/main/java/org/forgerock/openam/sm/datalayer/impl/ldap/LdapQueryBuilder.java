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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package org.forgerock.openam.sm.datalayer.impl.ldap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.DataLayerRuntimeException;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.SimplePagedResultsControl;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;

import com.iplanet.services.ldap.event.EventService;
import com.sun.identity.shared.debug.Debug;

/**
 * Constructs LDAP queries for execution of a specific {@link Filter} over a specific {@link Connection}.
 */
public class LdapQueryBuilder extends QueryBuilder<Connection, Filter> {

    // Represents the start and end state of the paged query.
    public static final ByteString EMPTY = getEmptyPagingCookie();

    private final LdapDataLayerConfiguration dataLayerConfiguration;
    private final LdapSearchHandler handler;
    private final Map<Class, EntryConverter> converterMap;
    private final ConnectionFactory<Connection> connectionFactory;
    protected ByteString pagingCookie;

    /**
     * Default constructor ensures the Object Class is defined.
     *
     * @param dataLayerConfiguration Required for data store dataLayerConfiguration.
     * @param handler The Search handler to use on the LDAP store.
     * @param debug To debug writer for this class.
     * @param converterMap The map ldap entry types to Java objects (partials, strings, tokens).
     * @param connectionFactoryProvider A producer of factories used to communicate down to the data layer with.
     */
    @Inject
    public LdapQueryBuilder(LdapDataLayerConfiguration dataLayerConfiguration, LdapSearchHandler handler,
                            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug,
                            Map<Class, EntryConverter> converterMap,
                            LdapConnectionFactoryProvider connectionFactoryProvider) {
        super(debug);
        this.dataLayerConfiguration = dataLayerConfiguration;
        this.handler = handler;
        this.converterMap = converterMap;
        this.connectionFactory = connectionFactoryProvider.createFactory();
    }

    /**
     * Perform the query and return the results as Entry instances.
     *
     * @param connection The connection used to perform the request.
     * @return A non null but possibly empty collection.
     */
    public <T> Iterator<Collection<T>> executeRawResults(Connection connection, Class<T> returnType) {
        if (String.class.equals(returnType) && requestedAttributes.length != 1) {
            throw new IllegalArgumentException("String return type wanted but more than 1 attribute requested");
        }
        EntryConverter<T> entryConverter = (EntryConverter<T>) converterMap.get(returnType);
        if (entryConverter == null) {
            throw new IllegalArgumentException("Cannot convert LDAP Entry objects to " + returnType.getName());
        }
        return new EntryIterator<>(connection, entryConverter);
    }

    @Override
    public ContinuousQuery executeContinuousQuery(ContinuousQueryListener listener) throws DataLayerException {

        CTSDJLDAPv3PersistentSearchBuilder builder = new CTSDJLDAPv3PersistentSearchBuilder(connectionFactory);

        ContinuousQuery pSearch = builder
                .withSearchFilter(getLDAPFilter())
                .returnAttributes(requestedAttributes)
                .withRetry(EventService.RETRY_INTERVAL)
                .withSearchBaseDN(dataLayerConfiguration.getTokenStoreRootSuffix())
                .withSearchScope(SearchScope.WHOLE_SUBTREE).build();

        pSearch.addContinuousQueryListener(listener);
        pSearch.startQuery();

        return pSearch;
    }

    private Collection<Entry> getEntries(Connection connection) throws CoreTokenException {
        // Prepare the search
        Filter ldapFilter = getLDAPFilter();
        SearchRequest searchRequest = LDAPRequests.newSearchRequest(
                dataLayerConfiguration.getTokenStoreRootSuffix(),
                SearchScope.WHOLE_SUBTREE,
                ldapFilter,
                getRequestedAttributes());
        searchRequest.setSizeLimit(sizeLimit);
        searchRequest.setTimeLimit((int) timeLimit.to(TimeUnit.SECONDS));

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
                if (control == null) {
                    if (debug.warningEnabled()) {
                        debug.warning("There was no paged result control in the search response, it is recommended to "
                                + "set the CTS user's size-limit at least to " + (pageSize + 1));
                    }
                    pagingCookie = getEmptyPagingCookie();
                } else {
                    pagingCookie = control.getCookie();
                }
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
     * We modify the requested attributes here to ensure the etag is
     * returned in addition to what is already being requested for.
     *
     * @return The attributes to be queried for.
     */
    private String[] getRequestedAttributes() {
        String[] attributes;
        if (requestedAttributes.length == 0) {
            attributes = new String[] {"*", CoreTokenField.ETAG.toString()};
        } else if (isETagRequested()) {
            attributes = requestedAttributes;
        } else {
            attributes = Arrays.copyOf(requestedAttributes, requestedAttributes.length + 1);
            attributes[attributes.length - 1] = CoreTokenField.ETAG.toString();
        }
        return attributes;
    }

    private boolean isETagRequested() {
        for (String attr : requestedAttributes) {
            if (CoreTokenField.ETAG.toString().equals(attr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return Creates a list based on the state of the builder.
     */
    private <T> Collection<T> createResultsList() {
        Collection<T> entries;
        if (isPagingResults()) {
            entries = new ArrayList<>(pageSize);
        } else if (sizeLimit != 0) {
            entries = new ArrayList<>(sizeLimit);
        } else {
            entries = new ArrayList<>();
        }
        return entries;
    }

    private Filter getLDAPFilter() {
        Filter objectClassFilter = Filter.equality(CoreTokenConstants.OBJECT_CLASS, CoreTokenConstants.FR_CORE_TOKEN);
        if (filter == null) {
            return objectClassFilter;
        } else {
            return Filter.and(filter, objectClassFilter);
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
     * @see CTSWorkerQuery
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

            final Collection<Entry> entries;
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
