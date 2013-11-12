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
package org.forgerock.openam.cts.impl.query;

import javax.inject.Inject;
import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;

import java.util.Collection;

/**
 * Responsible for perform a SearchRequest against an LDAP connectionFactory.
 *
 * This class smooths over the issues around trying to create unit testable code for
 * the search methods of the Connection.
 *
 * @author robert.wapshott@forgerock.com
 */
public class LDAPSearchHandler {
    // Injected
    private final ConnectionFactory connectionFactory;
    private final LDAPConfig constants;
    private Result result;

    @Inject
    public LDAPSearchHandler(ConnectionFactory connectionFactory, LDAPConfig constants) {
        this.connectionFactory = connectionFactory;
        this.constants = constants;
    }

    /**
     * Perform a search using a connection acquired from the connection factory.
     * The entries collection will be populated with the results of the search.
     *
     * @see Result
     * @param request Non null request to perform.
     * @param entries Non null modifable collection to fill with search results.
     * @return The Result of the search.
     * @throws QueryFailedException
     */
    public Result performSearch(SearchRequest request, Collection<Entry> entries) throws QueryFailedException {
        Filter filter = request.getFilter();
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            result = connection.search(request, entries);
            return result;
        } catch (ErrorResultException e) {
            throw new QueryFailedException(connection, constants.getTokenStoreRootSuffix(), filter, e);
        } finally {
            IOUtils.closeIfNotNull(connection);
        }
    }
}
