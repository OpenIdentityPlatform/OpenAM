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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package com.iplanet.dpro.session.operations;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionManager;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.monitoring.MonitoredOperations;
import com.iplanet.dpro.session.monitoring.SessionMonitorType;
import com.iplanet.dpro.session.monitoring.SessionMonitoringStore;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.operations.strategies.StatelessOperations;
import com.sun.identity.shared.debug.Debug;

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

    private final SessionOperations local;
    private final SessionOperations stateless;
    private final Debug debug;
    private final SessionMonitoringStore store;
    private final StatelessSessionManager statelessSessionManager;

    /**
     * Guice initialised constructor.
     *
     * @param local Required strategy.
     * @param store The store for session monitoring information.
     * @param statelessSessionManager Required for JWT checks.
     * @param debug Required for logging.
     */
    @Inject

    public ServerSessionOperationStrategy(
            final SessionMonitoringStore store,
            final LocalOperations local,
            final StatelessOperations stateless,
            final StatelessSessionManager statelessSessionManager,
            final @Named(SessionConstants.SESSION_DEBUG) Debug debug) {

        this.store = store;
        this.local = local;
        this.stateless = stateless;
        this.statelessSessionManager = statelessSessionManager;
        this.debug = debug;
    }


    /**
     * Based on the Session, determine the appropriate SessionOperations strategy to select.
     *
     * Local - For local Sessions which are hosted on the current Server.
     * Remote - The Session is from a remote Site, and the Site is up.
     * CTS - When cross talk is disabled, or if the Session is from a remote Site, which is down.
     *
     * @param sessionID Non null SessionID to use.
     * @return A non null SessionOperations implementation to use.
     */
    @Override
    public SessionOperations getOperation(SessionID sessionID) {
        if (isStateless(sessionID)) {
            return logAndWrap(sessionID, stateless, SessionMonitorType.STATELESS);
        }

        return logAndWrap(sessionID, local, SessionMonitorType.LOCAL);
    }

    private boolean isStateless(SessionID sessionID) {
        return statelessSessionManager.containsJwt(sessionID);
    }

    /**
     * Inline logging function.
     * @param sessionID Non null.
     * @param op Non null operation selected.
     * @param type
     * @return {code op}, wrapped in a MonitoredOperations.
     */
    private SessionOperations logAndWrap(SessionID sessionID, SessionOperations op, SessionMonitorType type) {
        if (debug.messageEnabled()) {
            debug.message(sessionID + ": " + op.getClass().getSimpleName() + " selected.");
        }
        return new MonitoredOperations(op, type, store);
    }
}
