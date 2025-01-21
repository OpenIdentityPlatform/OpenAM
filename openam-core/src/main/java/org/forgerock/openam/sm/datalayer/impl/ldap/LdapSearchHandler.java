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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.sm.datalayer.impl.ldap;

import com.sun.identity.shared.debug.Debug;
import java.util.Collection;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;

/**
 * Performs a search against an LDAP Connection.
 */
public class LdapSearchHandler {
    // Injected
    private final LDAPConfig ldapConfig;
    private final Debug debug;

    @Inject
    public LdapSearchHandler(LdapDataLayerConfiguration ldapConfig,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug) {
        this.ldapConfig = ldapConfig;
        this.debug = debug;
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
        } catch (LdapException e) {
            if (!entries.isEmpty()) {
                if (!ResultCode.SIZE_LIMIT_EXCEEDED.equals(e.getResult().getResultCode()) && debug.warningEnabled()) {
                    debug.warning("Search abandoned due to error. Returning incomplete search results.", e);
                }
                return e.getResult();
            }
            throw new QueryFailedException(connection, ldapConfig.getTokenStoreRootSuffix(), request.getFilter(), e);
        }
    }
}
