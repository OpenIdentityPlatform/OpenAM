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
package org.forgerock.openam.entitlement.indextree;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceManagementDAO;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.FutureResult;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.controls.PersistentSearchRequestControl;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This monitor implementation acquires a connection against the data source and uses a persistent search to listen for
 * index changes. The persistent search uses a search result handler to feed back any changes or errors.
 */
public class IndexChangeMonitorImpl implements IndexChangeMonitor {

    private static final String PATH_INDEX_FILTER = "(&(sunserviceID=indexes)(sunxmlKeyValue=pathindex=*))";
    private static final Debug DEBUG = Debug.getInstance("amEntitlements");

    private final SearchResultHandler handler;
    private final ConnectionFactory factory;

    private final SearchRequest request;

    private Connection connection;
    private FutureResult<Result> searchStatus;

    @Inject
    public IndexChangeMonitorImpl(SearchResultHandler handler,
                                  @Named(DataLayerConstants.DATA_LAYER_BINDING) ConnectionFactory factory,
                                  ServiceManagementDAO smDAO) {
        this.handler = handler;
        this.factory = factory;

        // Construct the persistent search request.
        request = Requests.newSearchRequest(
                "ou=services," + smDAO.getRootSuffix(),
                SearchScope.WHOLE_SUBTREE,
                PATH_INDEX_FILTER)
                .addControl(PersistentSearchRequestControl.newControl(
                        true, true, true,
                        PersistentSearchChangeType.ADD,
                        PersistentSearchChangeType.DELETE,
                        PersistentSearchChangeType.MODIFY));
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void start() throws ChangeMonitorException {
        try {
            connection = factory.getConnection();
            // Start the persistence search.
            searchStatus = connection.searchAsync(request, null, handler);

        } catch (Exception e) {

            // Ensure any initiated search gets cleaned up before throwing on the exception.
            shutdown();

            throw new ChangeMonitorException("Failed creating persistent search.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void shutdown() {
        closeSearch();
        closeConnection();
    }

    /**
     * Ensure the search is safely closed.
     */
    private void closeSearch() {
        try {
            // Kill of any remaining search listener.
            if (searchStatus != null) {
                searchStatus.cancel(true);
                searchStatus = null;
            }
        } catch (Exception e) {
            DEBUG.warning("Connection failed to close.", e);
        }
    }

    /**
     * Ensure the connection is safely closed.
     */
    private void closeConnection() {
        try {
            // Ensure any connection is cleaned up before shutdown.
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (Exception e) {
            DEBUG.warning("Connection failed to close.", e);
        }
    }

}