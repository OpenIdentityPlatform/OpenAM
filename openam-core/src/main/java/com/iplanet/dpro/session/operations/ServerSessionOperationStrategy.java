/**
 * Copyright 2014 ForgeRock AS.
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
package com.iplanet.dpro.session.operations;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.operations.strategies.CTSOperations;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.operations.strategies.RemoteOperations;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.sun.identity.shared.debug.Debug;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server based SessionOperationStrategy implementation.
 *
 * SessionOperations represent the available operations that can be performed on a Session,
 * which are applicable under a number of situations. These situations correspond to the
 * number of situations Sessions can find themselves in.
 *
 * This strategy covers the following:
 *
 * <b>local</b> - The Session is based on the current server and that server is responsible for
 * processing the request.
 *
 * <b>remote</b> - The Session is based on a remote server which will service the request and
 * respond with an appropriate response. This is performed by using a remote request and the
 * PLL signalling system.
 *
 * <b>CTS</b> - The Session is a remote session, however the Site appears to be down. The request
 * will be performed locally using the CTS instead.
 *
 * Between these strategies, the users Session should be available during fail-over of a Site.
 */
public class ServerSessionOperationStrategy implements SessionOperationStrategy {
    private final SessionService service;

    private final LocalOperations local;
    private final RemoteOperations remote;
    private final CTSOperations cts;
    private final WebtopNamingQuery queryUtils;
    private final Debug debug;

    /**
     * Guice initialised constructor.
     *
     * @param service Required for local server decisions.
     * @param local Required strategy.
     * @param remote Required strategy.
     * @param cts Required strategy.
     * @param queryUtils Required for Server availability decisions.
     * @param debug Required for logging.
     */
    @Inject
    public ServerSessionOperationStrategy(SessionService service,
                                          LocalOperations local,
                                          RemoteOperations remote,
                                          CTSOperations cts,
                                          WebtopNamingQuery queryUtils,
                                          @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.service = service;
        this.local = local;
        this.remote = remote;
        this.cts = cts;
        this.queryUtils = queryUtils;
        this.debug = debug;
    }

    /**
     * Based on the Session, determine the appropriate SessionOperations strategy to select.
     *
     * Local - For local Sessions which are hosted on the current Server.
     * Remote - For Sessions which are from a remote Site, and the Site is up.
     * CTS - For Sessions which are from a remote Site, which is down.
     *
     * @param session Non null Session to use.
     * @return A non null SessionOperations implementation to use.
     */
    public SessionOperations getOperation(Session session) {
        if (isLocalServer(session)) {
            return log(session, local);
        }

        if (service.isSessionFailoverEnabled() && !isLocalSite(session) && !isSiteUp(getSiteId(session))) {
            return log(session, cts);
        }

        return log(session, remote);
    }

    /**
     * Fetches the Site for a Session Server ID, based on the results of WebtopNaming.
     * @param session A non null Session which may or may not be part of a Site.
     * @return Null if no Site ID was found, otherwise a non null Site ID.
     */
    private String getSiteId(Session session) {
        String serverID = session.getID().getSessionServerID();
        if (queryUtils.isSite(serverID)) {
            return serverID;
        }
        return queryUtils.getSiteID(serverID);
    }

    /**
     * Indicates that the Site associated with the Session is up.
     *
     * @param siteId Site ID to test if it is up. May be null, in which case
     *               false will be returned.
     * @return False if the Site ID is null, or if the Site was down.
     * True if the Site was up.
     */
    private boolean isSiteUp(String siteId) {
        if (siteId == null) {
            return false;
        }

        return service.checkSiteUp(siteId);
    }

    /**
     * Tests if the Session should be considered local.
     *
     * @param session Non null Session.
     * @return True if it is based on the current server.
     */
    private boolean isLocalServer(Session session) {
        try {
            return service.checkSessionLocal(session.getID());
        } catch (SessionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Indicates that the Session belongs to a the local Site. That is, it is
     * hosted on a Server within the current cluster.
     *
     * @param session Non null Session.
     * @return True if the Session is considered local.
     */
    private boolean isLocalSite(Session session) {
        return service.isLocalSite(session.getID());
    }

    /**
     * Inline logging function.
     * @param session Non null.
     * @param op Non null operation selected.
     * @return op.
     */
    private SessionOperations log(Session session, SessionOperations op) {
        if (debug.messageEnabled()) {
            debug.message(session + ": " + op.getClass().getSimpleName() + " selected.");
        }
        return op;
    }
}
