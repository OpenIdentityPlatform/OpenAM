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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Performs a search against an LDAP Connection.
 */
public class LDAPSearchHandler {
    // Injected
    private final LDAPConfig ldapConfig;

    @Inject
    public LDAPSearchHandler(LDAPConfig ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    /**
     * Perform a search using the provided connection.
     *
     * @see Result
     *
     * @param connection Required for perform the search.
     * @param request Non null request to perform.
     * @param entries Non null, modifiable collection to populate with search results.
     *
     * @return The Result of the search.
     *
     * @throws QueryFailedException If there was an error performing the query.
     */
    public Result performSearch(Connection connection, SearchRequest request, Collection<Entry> entries) throws QueryFailedException {
        try {
            return connection.search(request, entries);
        } catch (ErrorResultException e) {
            throw new QueryFailedException(connection, ldapConfig.getTokenStoreRootSuffix(), request.getFilter(), e);
        }
    }
}
