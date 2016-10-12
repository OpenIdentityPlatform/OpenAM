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
* information: "Portions copyright [year] [name of copyright ownter]".
*
* Copyright 2016 ForgeRock AS.
*/
package com.iplanet.dpro.session.monitoring;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.session.service.ServicesClusterMonitorHandler;
import org.forgerock.openam.session.service.SessionAccessManager;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.sun.identity.shared.debug.Debug;

/**
 * Handler to track sessions which have been loaded on this server, but for which this is not their home server.
 * This can happen in the event that their original server went down, and this server received a request to operate on
 * them.
 */
@Singleton
public class ForeignSessionHandler {

    private final Set<SessionID> remoteSessionSet = Collections.synchronizedSet(new HashSet<SessionID>());
    private final Debug debug;
    private final SessionServerConfig serverConfig;
    private final SessionCookies sessionCookies;
    private final ServicesClusterMonitorHandler servicesClusterMonitorHandler;

    /**
     * Instantiates the session handler.
     * @param debug Session debug instance.
     * @param serverConfig Server configuration information.
     * @param sessionCookies Session cookies, used to get the load balancer cookie.
     * @param servicesClusterMonitorHandler Used to get information about the cluster.
     */
    @Inject
    public ForeignSessionHandler(@Named(org.forgerock.openam.session.SessionConstants.SESSION_DEBUG) final Debug debug,
                          final SessionServerConfig serverConfig,
                          final SessionCookies sessionCookies,
                          final ServicesClusterMonitorHandler servicesClusterMonitorHandler) {
        this.debug = debug;
        this.serverConfig = serverConfig;
        this.sessionCookies = sessionCookies;
        this.servicesClusterMonitorHandler = servicesClusterMonitorHandler;
    }

    /**
     * Add a remote session to track.
     * @param sessionID The ID of the session to add.
     */
    private void add(SessionID sessionID) {
        remoteSessionSet.add(sessionID);
    }

    /**
     * Remove a remote Session.
     * @param sessionID The ID of the session to add.
     */
    public void remove(SessionID sessionID) {
        remoteSessionSet.remove(sessionID);
    }

    /**
     * Quick access to the total size of the remoteSessionSet.
     *
     * @return the size of the sessionTable.
     */
    public int getRemoteSessionCount() {
        return remoteSessionSet.size();
    }

    /**
     * Function to remove remote sessions when primary server is up.
     */
    public void cleanUpRemoteSessions() {
        synchronized (remoteSessionSet) {
            for (Iterator iter = remoteSessionSet.iterator(); iter.hasNext(); ) {
                SessionID sessionID = (SessionID) iter.next();
                // getCurrentHostServer automatically releases local
                // session replica if it does not belong locally
                String hostServer = null;
                try {
                    hostServer = getCurrentHostServer(sessionID);
                } catch (Exception ex) {
                }
                // if session does not belong locally remove it
                if (!serverConfig.isLocalServer(hostServer)) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * This is a key method for "internal request routing" mode. It determines
     * the server id which is currently hosting session identified by sid. In
     * "internal request routing" mode, this method also has a side effect of
     * releasing a session which no longer "belongs locally" (e.g., due to
     * primary server instance restart).
     *
     * @param sessionId session id
     * @return server id for the server instance determined to be the current host.
     * @throws SessionException
     */
    public String getCurrentHostServer(SessionID sessionId) throws SessionException {

        String serverId = servicesClusterMonitorHandler.getCurrentHostServer(sessionId);

        // if we have a local session replica, discard it as hosting server instance is not supposed to be local
        if (!serverConfig.isLocalServer(serverId)) {
            // actively clean up duplicates
            handleReleaseSession(sessionId);
        }
        return serverId;
    }

    /**
     * Removes InternalSession from the session table so that another server
     * instance can be an owner. This is the server side of distributed
     * invocation initiated by calling releaseSession()
     *
     * @param sessionID session id of the session migrated
     */
    private void handleReleaseSession(SessionID sessionID) {
        // switch to non-local mode for cached client side session image
        InternalSession internalSession = InjectorHolder.getInstance(SessionAccessManager.class).releaseSession(sessionID); //TODO : untangle dependency loop
        if (internalSession == null) {
            debug.message("releaseSession: session not found {}", sessionID);
        }
    }

    /**
     * Utility used to updated various cross-reference mapping data structures
     * associated with sessions up-to-date when sessions are being recovered
     * after server instance failure.
     *
     * @param session session object
     */
    public void updateSessionMaps(InternalSession session) {
        if (null == session) {
            return;
        }
        SessionID sessionID = session.getID();
        String primaryID = sessionID.getExtension().getPrimaryID();
        if (!serverConfig.isLocalServer(primaryID)) {
            add(sessionID);
        }
        session.putProperty(sessionCookies.getLBCookieName(), serverConfig.getLBCookieValue());
    }

}
