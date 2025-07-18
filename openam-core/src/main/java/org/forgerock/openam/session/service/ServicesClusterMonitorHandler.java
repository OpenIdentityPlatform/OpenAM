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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.session.service;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.openam.session.SessionConstants;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.service.cluster.ClusterMonitor;
import com.iplanet.dpro.session.service.cluster.MultiServerClusterMonitor;
import com.iplanet.dpro.session.service.cluster.SingleServerClusterMonitor;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.sun.identity.shared.debug.Debug;

/**
 * Singleton for managing access to the cluster monitor object.
 *
 * This is planned to be removed when cluster monitor is removed
 *
 */
@Singleton
public class ServicesClusterMonitorHandler {

    /**
     * Reference to the ClusterMonitor instance. When server configuration changes which requires
     * a different instance (e.g. SFO changing state) then this AtomicReference will ensure
     * thread safety around the access to the ClusterMonitor instance.
     */
    private final AtomicReference<ClusterMonitor> clusterMonitor = new AtomicReference<>();

    private final Debug sessionDebug;
    private final SessionServiceConfig serviceConfig;
    private final SessionServerConfig serverConfig;

    @Inject
    private ServicesClusterMonitorHandler(final @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                                          final SessionServiceConfig serviceConfig,
                                          final SessionServerConfig serverConfig) {
        this.sessionDebug = sessionDebug;
        this.serviceConfig = serviceConfig;
        this.serverConfig = serverConfig;
    }

    /**
     * This is a key method for "internal request routing" mode It determines
     * the server id which is currently hosting session identified by sid. In
     * "internal request routing" mode, this method also has a side effect of
     * releasing a session which no longer "belongs locally" (e.g., due to
     * primary server instance restart)
     *
     * @param sid session id
     * @return server id for the server instance determined to be the current
     *         host
     * @throws SessionException
     */
    public String getCurrentHostServer(SessionID sid) throws SessionException {
       return getClusterMonitor().getCurrentHostServer(sid);
     }

    /**
     * Actively check if server identified by serverID is up
     *
     * @param serverID server id
     * @return true if server is up, false otherwise
     */
    public boolean checkServerUp(String serverID) {
        try {
            return getClusterMonitor().checkServerUp(serverID);
        } catch (SessionException e) {
            sessionDebug.error("Failed to check Server Up for {0}", serverID, e);
            return false;
        }
    }

    /**
     * Indicates that the Site is up.
     *
     * @param siteId A possibly null Site Id.
     * @return True if the Site is up, False if it failed to respond to a query.
     */
    public boolean isSiteUp(String siteId) {
        try {
            return getClusterMonitor().isSiteUp(siteId);
        } catch (SessionException e) {
            sessionDebug.error("Failed to check isSiteUp for {0}", siteId, e);
            return false;
        }
    }

    /**
     * The ClusterMonitor state depends on whether the system is configured for
     * SFO or not. As such, this method is aware of the change in SFO state
     * and triggers a re-initialisation of the ClusterMonitor as required.
     *
     * Note, this method also acts as the lazy initialiser for the ClusterMonitor.
     *
     * Thread Safety: Uses atomic reference to ensure only one thread can modify
     * the reference at any one time.
     *
     * @return A non null instance of the current ClusterMonitor.
     * @throws SessionException If there was an error initialising the ClusterMonitor.
     */
    private ClusterMonitor getClusterMonitor() throws SessionException {
        if (!isClusterMonitorValid()) {
            try {
                ClusterMonitor previous = clusterMonitor.getAndSet(resolveClusterMonitor());
                if (previous != null) {
                    sessionDebug.message("Previous ClusterMonitor shutdown: {}", previous.getClass().getSimpleName());
                    previous.shutdown();
                }
                sessionDebug.message("ClusterMonitor initialised: {}", clusterMonitor.get().getClass().getSimpleName());
            } catch (Exception e) {
                sessionDebug.error("Failed to initialise ClusterMonitor", e);
            }
        }


        ClusterMonitor monitor = clusterMonitor.get();
        if (monitor == null) {
            throw new SessionException("Failed to initialise ClusterMonitor");
        }
        return monitor;
    }

    /**
     * @return True if the ClusterMonitor is valid for the current SessionServiceConfiguration.
     */
    private boolean isClusterMonitorValid() {
        // Handles lazy init case
        if (clusterMonitor.get() == null) {
            return false;
        }

        Class<? extends ClusterMonitor> monitorClass = clusterMonitor.get().getClass();
        if (isPartOfCluster()) {
            return monitorClass.isAssignableFrom(MultiServerClusterMonitor.class);
        } else {
            return monitorClass.isAssignableFrom(SingleServerClusterMonitor.class);
        }
    }

    /**
     * Resolves the appropriate instance of ClusterMonitor to initialise.
     *
     * @return A non null ClusterMonitor based on service configuration.
     * @throws Exception If there was an unexpected error in initialising the MultiClusterMonitor.
     */
    private ClusterMonitor resolveClusterMonitor() throws Exception {
        if (isPartOfCluster()) {
            return new MultiServerClusterMonitor(sessionDebug, serviceConfig, serverConfig);
        } else {
            return new SingleServerClusterMonitor();
        }
    }

    private boolean isPartOfCluster() {
        WebtopNamingQuery query = new WebtopNamingQuery();
        try {
            String serverId =  query.getAMServerID();
            String siteId = query.getSiteID(serverId);
            return siteId != null;
        } catch (ServerEntryNotFoundException e) {
            return false;
        }

    }
}
