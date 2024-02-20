/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2023 3A Systems LLC.
 */

package com.iplanet.dpro.session.service.cluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.annotations.VisibleForTesting;
import com.iplanet.am.util.SystemProperties;
import org.forgerock.openam.utils.CollectionUtils;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.PermutationGenerator;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

/**
 * API for querying status of servers in cluster.
 *
 * Extracted from SessionService class as part of first-pass refactoring to improve SessionService adherence to SRP.
 *
 * @since 13.0.0
 */
/*
 * Further refactoring is warranted.
 */
public class MultiServerClusterMonitor implements ClusterMonitor {

    private static final boolean MONITOR_CLUSTER_SITES = SystemProperties.
            getAsBoolean("org.openidentityplatform.cluster.monitorSites", true);

    private final Debug sessionDebug;
    private final SessionServiceConfig serviceConfig;
    private final SessionServerConfig serverConfig;
    private final ConcurrentMap<String, String> clusterMemberMap = new ConcurrentHashMap<>();
    private final ClusterStateServiceFactory clusterStateServiceFactory;
    private volatile ClusterStateService clusterStateService = null;

    /**
     *
     * @param sessionDebug The session debug instance.
     * @param serviceConfig The configuration for the session service.
     * @param serverConfig The configuration for the session server.
     * @throws Exception
     */
    public MultiServerClusterMonitor(
            Debug sessionDebug,
            SessionServiceConfig serviceConfig,
            SessionServerConfig serverConfig) throws Exception {

        this(sessionDebug, serviceConfig, serverConfig, new ClusterStateServiceFactory());
    }

    /**
     * Chained constructor to allow ClusterStateServiceFactory to be mocked from tests.
     */
    @VisibleForTesting
    MultiServerClusterMonitor(
            Debug sessionDebug,
            SessionServiceConfig serviceConfig,
            SessionServerConfig serverConfig,
            ClusterStateServiceFactory clusterStateServiceFactory) throws Exception {

        this.sessionDebug = sessionDebug;
        this.serviceConfig = serviceConfig;
        this.serverConfig = serverConfig;
        this.clusterStateServiceFactory = clusterStateServiceFactory;

        // Next, must be in this Order!
        // Initialize our Cluster Member Map, first!
        initClusterMemberMap();
        // Initialize the Cluster State Service, second!
        // (As Cluster Service uses Cluster Member Map).
        initializeClusterService();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSiteUp(String siteId) {
        return clusterStateService.isSiteUp(siteId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkServerUp(String serverID) {
        return ((serverID == null) || (serverID.isEmpty())) ? false : clusterStateService.checkServerUp(serverID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentHostServer(SessionID sid) throws SessionException {
        String serverID = locateCurrentHostServer(sid);
        if (serverID == null) {
            // Is this block necessary? Can locateCurrentHostServer ever return null?
            return sid.getSessionServerID();
        }
        return serverID;
    }

    /**
     * Initialise the ClusterStateService to monitor all Servers within the current
     * Site, and also all Sites except the current Site. This will allow the
     * ClusterStateService to provide an answer to whether a Site is up or down.
     *
     * @throws Exception If there was an unexpected error during initialisation.
     */
    private synchronized void initializeClusterService() throws Exception {
        // Initialize Our Cluster State Service
        // Ensure we place our Server in Member Map.
        clusterMemberMap.put(
                serverConfig.getLocalServerSessionServiceURL().toExternalForm(),
                serverConfig.getLocalServerID());

        final Map<String, String> siteMemberMap;
        if(MONITOR_CLUSTER_SITES) {
            // Collect all Sites to monitor
            siteMemberMap = getSiteMemberMap();
        } else {
            siteMemberMap = Collections.emptyMap();
        }

        // Instantiate the State Service.
        clusterStateService = clusterStateServiceFactory.createClusterStateService(serverConfig,
                serviceConfig, CollectionUtils.invertMap(clusterMemberMap), siteMemberMap);

        // Show our State Server Info Map
        if (sessionDebug.messageEnabled()) {
            sessionDebug.message(
                    "SessionService's ClusterStateService Initialized Successfully, " + clusterStateService.toString());
        }
    }

    /**
     * Obtain a mapping of site ID to primary URL for all other sites
     *
     * @return
     */
    private Map<String, String> getSiteMemberMap() throws Exception {
        Map<String, String> siteMemberMap = new HashMap<>();
        for (Object nodeId : serverConfig.getAllServerIDs()) {
            String serverOrSiteId = (String) nodeId;
            if (serverConfig.isSite(serverOrSiteId)) {
                // Excluding the current local site, as it is not needed for intra-cluster failover.
                if (serverConfig.isLocalSite(serverOrSiteId)) {
                    continue;
                }
                siteMemberMap.put(serverOrSiteId, serverConfig.getServerFromID(serverOrSiteId));
            }
        }
        return siteMemberMap;
    }

    /**
     * Initialize the cluster server map given the server IDs in Set (AM70).
     */
    private void initClusterMemberMap() throws Exception {
        Set<String> serverIDs = serverConfig.getServerIDsInLocalSite();
        for (String serverID : serverIDs) {
            String serverURL = serverConfig.getServerFromID(serverID);
            if ((serverID == null) || (serverURL == null)) {
                continue;
            }
            // ************************************************************
            // This if Clause is very important, please do not think it is
            // not.  If we pollute the cluster map with duplicate URLs
            // There is a very good chance Login processing will
            // automatically fail, since it can not determine
            // which serverId is which.
            // Only Associate one Server URL to a Single ServerID.
            // @since 10.1
            //
            // Further investigate, as we should not get duplicates here...
            //
            clusterMemberMap.putIfAbsent(serverURL, serverID);
        }
    }

    /**
     * A convenience method to get the cluster server list in delimiter
     * separated String format. This is currently used in message debug log.
     */
    private String getClusterServerList() {
        StringBuilder clusterServerList = new StringBuilder();
        for (String serverID : clusterMemberMap.values()) {
            clusterServerList.append(serverID).append(" ");
        }
        return clusterServerList.toString();
    }

    /**
     * Determines current hosting server instance for internal request routing
     * mode.
     *
     * @param sid session id
     * @return server id for the server instance determined to be the current
     *         host
     * @throws com.iplanet.dpro.session.SessionException
     */
    String locateCurrentHostServer(SessionID sid) throws SessionException {
        String primaryID = sid.getExtension().getPrimaryID();
        String serverID = sid.getSessionServerID();
        // if this is our local Server
        if (serverConfig.isLocalServer(serverID)) {
            return serverID;
        }
        // if session is from remote site
        if (!serverConfig.isPrimaryServer(serverID)) {
            return serverID;
        }

        // Ensure we have a Cluster State Service Available.
        synchronized (this) {
            if (clusterStateService == null) {
                try {
                    initializeClusterService();
                } catch (Exception e) {
                    sessionDebug.error("Unable to Initialize the Cluster Service, please review Configuration settings.", e);
                    throw new SessionException(e);
                }
            }
        }

        // Check for Service Available.
        if (clusterStateService.isUp(primaryID)) {
            return primaryID;
        } else {
            int selectionListSize = clusterStateService.getServerSelectionListSize();
            String sKey = sid.getExtension().getStorageKey();
            if(sKey == null){
                throw new SessionException("SessionService.locateCurrentHostServer: StorageKey is null");
            }
            PermutationGenerator perm = new PermutationGenerator(sKey.hashCode(), selectionListSize);
            String selectedServerId = null;
            for (int i = 0; i < selectionListSize; ++i) {
                selectedServerId = clusterStateService.getServerSelection(perm.itemAt(i));
                if (selectedServerId == null) {
                    continue;
                }
                if (clusterStateService.isUp(selectedServerId)) {
                    break;
                }
            }

            // since current server is also part of the selection list
            // selection process is guaranteed to succeed
            return selectedServerId;
        }
    }

    /**
     * Factory to allow ClusterStateService to be mocked from tests
     */
    static class ClusterStateServiceFactory {

        ClusterStateService createClusterStateService(
                SessionServerConfig sessionServerConfig,
                SessionServiceConfig sessionServiceConfig,
                Map<String, String> serverMembers,
                Map<String, String> siteMembers) throws Exception {

            return new ClusterStateService(
                    sessionServerConfig.getLocalServerID(),
                    sessionServiceConfig.getSessionFailoverClusterStateCheckTimeout(),
                    sessionServiceConfig.getSessionFailoverClusterStateCheckPeriod(),
                    serverMembers,
                    siteMembers);
        }

    }

    /**
     * Signals that this ClusterMonitor should be shutdown.
     * Will signal the underlying {@link ClusterStateService} to cancel
     * its runnable thread.
     *
     * Thread Safety: Synchronized to prevent possible multiple calls which
     * would break state in the underlying GeneralRunnableTask framework.
     */
    @Override
    public synchronized void shutdown() {
        clusterStateService.cancel();
    }
}
