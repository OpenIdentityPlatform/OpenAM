/**
 * Copyright 2013 ForgeRock, AS.
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

import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.exceptions.QueryFailedException;
import com.sun.identity.sm.ldap.utils.TokenAttributeConversion;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ErrorResultIOException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Responsible for perform a SearchRequest against an LDAP connectionFactory.
 *
 * This class smooths over the issues around trying to create unit testable code for
 * the search methods of the Connection.
 *
 * @author robert.wapshott@forgerock.com
 */
public class LDAPSearchHandler {
    private DataLayerConnectionFactory connectionFactory;
    private CoreTokenConstants constants;

    public LDAPSearchHandler(DataLayerConnectionFactory connectionFactory, CoreTokenConstants constants) {
        this.connectionFactory = connectionFactory;
        this.constants = constants;
    }

    /**
     * Perform a search against the assigned connectionFactory with the given SearchRequest.
     *
     * This call will iterate through all results in the search and collect all Entries
     * for return to the caller.
     *
     * Note: This is a blocking call.
     *
     * @param request Non null SearchRequest.
     * @return A non null but possibly empty collection of Entrys from the search.
     * @throws QueryFailedException If there was any problem performing the query.
     */
    public Collection<Entry> performSearch(SearchRequest request) throws QueryFailedException {
        int limit = request.getSizeLimit();
        Filter filter = request.getFilter();
        List<Entry> entries = new LinkedList<Entry>();

        Connection connection = null;

        try {
            connection = connectionFactory.getConnection();
            ConnectionEntryReader search = connection.search(request);
            /**
             * Importantly, when a size limit is set on a query we must not exceed this with any operation
             * otherwise a Size Limit Exceeded exception will be thrown.
             */
            while (isWithinSearchLimit(limit, entries) && search.hasNext()) {
                /**
                 * It is possible that the search results contains a reference, which
                 * we will skip as there may still be additional entries in results.
                 */
                if (!search.isEntry()) {
                    continue;
                }

                Entry entry = search.readEntry();
                /**
                 * Filter out the ObjectClass attribute. It serves no purpose in subsequent
                 * processing and is not a field mapped by the CoreTokenField.
                 * (this introduces edge cases which are undesirable).
                 */
                entry = TokenAttributeConversion.stripObjectClass(entry);

                entries.add(entry);
            }
            search.close();
        } catch (ErrorResultIOException e) {
            throw new QueryFailedException(connection, constants.getTokenDN(), filter, e);
        } catch (SearchResultReferenceIOException e) {
            throw new QueryFailedException(connection, constants.getTokenDN(), filter, e);
        } catch (ErrorResultException e) {
            throw new QueryFailedException(connection, constants.getTokenDN(), filter, e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        return entries;
    }

    /**
     * Validation logic to ensure we do not exceed the search limit. This causes an exception
     * from the SDK.
     *
     * @param limit Zero or greater.
     * @param entries Current number of Entrys collected.
     * @return True if the search is still within the search limit.
     */
    private boolean isWithinSearchLimit(int limit, List<Entry> entries) {
        if (limit == 0) {
            return true;
        }
        return entries.size() < limit;
    }
}
