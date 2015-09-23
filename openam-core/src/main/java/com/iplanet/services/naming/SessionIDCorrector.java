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
 * Copyright 2015 ForgeRock AS.
 */
package com.iplanet.services.naming;

import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.StringUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * In the case of reconfiguring servers in a cluster, or in the case of
 * changing the servers in a site, a Sessions the Server ID (S1) and
 * Site ID (SI) of the Session will potentially be out of sync.
 *
 * For more details about these two fields see {@link SessionID#parseSessionString()}.
 *
 * SessionIDCorrector will resolve these discrepancies by adding in or removing the
 * Site ID reference accordingly. This logic depends heavily on the assumptions about how
 * these two fields are used.
 *
 * This class balances two competing requirements. The need to be accurate with
 * the Server>Site mapping, and the need to be performant as this code will
 * be triggered on every invocation of {@link SessionID}.
 */
public class SessionIDCorrector {
    public static final String HEADER = "SessionIDCorrector:";
    private final static Debug debug = Debug.getInstance("amNaming");

    private final Map<String, String> serverToSite;

    public SessionIDCorrector(Map<String, String> serverToSite) {
        this.serverToSite = serverToSite;
    }

    /**
     * Factory method to generate an instance of SessionIDCorrector based on the current
     * WebtopNaming Settings.
     *
     * Understands the Server/SiteID interactions which WebtopNaming uses for modelling
     * Serers and Sites. Some of this is counter-intuitive.
     *
     * Note: This code cannot directly listen to configuration changes because of the
     * order in which updates to configuration are propagated throughout the system.
     * Instead it is intended to be called by WebtopNaming (which in turn is called
     * by NamingService).
     *
     * @return Non null instance of SessionIDCorrector
     */
    public static SessionIDCorrector create() {
        Map<String, String> serverToSite = new HashMap<>();
        try {
            for (Object s : WebtopNaming.getAllServerIDs()) {
                String serverId = s.toString();
                // Quirk: WebtopNaming returns the ID of the Server if it is not part of a Site.
                // SiteID can also be null.
                String siteId = WebtopNaming.getSiteID(serverId);
                if (serverId.equals(siteId)) {
                    siteId = null;
                }
                debug.message("{} Mapping Server {} to Site {}", HEADER, serverId, siteId);
                serverToSite.put(serverId, siteId);
            }
            debug.message("{} Mapping complete", HEADER);
        } catch (Exception e) {
            debug.error("Failed to build Autocorrect Mapping", e);
            return null;
        }
        return new SessionIDCorrector(serverToSite);
    }

    /**
     * Performs a possible translation of the Primary ID (S1) field based on the state
     * of OpenAM Server/Site configuration.
     *
     * @param primaryID Non null, possibly empty field.
     * @param siteID Non null, possibly empty field.
     *
     * @return Either the provided Primary ID if no mapping took place, or another value
     * depending on the mapping.
     */
    public String translatePrimaryID(String primaryID, String siteID) {
        return update(new ResolvedServer(primaryID, siteID)).getPrimaryID();
    }

    /**
     * Performs a possible translation of the Site ID (SI) field based on the state
     * of OpenAM Server/Site configuration.
     *
     * @param primaryID Non null, possibly empty field.
     * @param siteID Non null, possibly empty field.
     *
     * @return Either the provided Site ID if no mapping took place, or another value
     * depending on the mapping.
     */
    public String translateSiteID(String primaryID, String siteID) {
        return update(new ResolvedServer(primaryID, siteID)).getSiteID();
    }

    /**
     * Perform the logic of checking whether we should be modifying the SiteID, and if
     * so what to change it to.
     *
     * @param server Non null ResolvedServer.
     * @return Non null ResolvedServer.
     */
    private ResolvedServer update(ResolvedServer server) {
        final String resolvedServer = server.getResolvedServer();

        if (serverToSite.containsKey(resolvedServer)) {
            if (!server.inSite() && serverToSite.get(resolvedServer) != null) {
                server.setSite(serverToSite.get(resolvedServer));
            } else if (server.inSite() && serverToSite.get(resolvedServer) == null) {
                server.setSite(null);
            }
        }
        return server;
    }

    @Override
    public String toString() {
        String r = "";
        for (Map.Entry<String, String> entry : serverToSite.entrySet()) {
            r += MessageFormat.format("{0} -> {1}, ", entry.getKey(), entry.getValue());
        }
        return r;
    }

    /**
     * Resolve the primary server using the legacy behaviour for
     * SessionID S1/SI gives us the following logic:
     *
     * S1 not set, SI set = Primary Server ID
     * S1 set, SI set = Primary Server ID & Site ID
     * S1 set, SI not set = Primary Server ID (never used)
     *
     * When working with S1/SI IDs we need to ensure we are working with
     * the correct values. This class will manage the translation from
     * S1/SI to Server and Site IDs. This will reduce the risk of error
     * when working on this code.
     */
    private class ResolvedServer {
        private final String server;
        private String site;

        public ResolvedServer(String primaryID, String siteID) {
            if (StringUtils.isEmpty(primaryID)) {
                server = siteID;
                site = null;
            } else {
                server = primaryID;
                site = siteID;
            }
        }

        public String getResolvedServer() {
            return server;
        }

        public String getResolvedSite() {
            return site;
        }

        public boolean inSite() {
            return !StringUtils.isEmpty(site);
        }

        public void setSite(String site) {
            this.site = site;
        }

        public String getSiteID() {
            if (site == null) {
                return server;
            }
            return site;
        }

        public String getPrimaryID() {
            if (site == null) {
                return null;
            }
            return server;
        }
    }
}
