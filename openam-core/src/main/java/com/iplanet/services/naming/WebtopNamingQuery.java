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
package com.iplanet.services.naming;

import java.net.URL;
import java.util.Set;

/**
 * Provides a non-static wrapper around WebtopNaming so this service
 * can be injected into classes using Guice and tested appropriately.
 */
public class WebtopNamingQuery {

    /**
     * @see WebtopNaming#getSiteID(String)
     * @param server Non null Server ID.
     * @return Null if the server was not part of a Site, or not found in any Site.
     */
    public String getSiteID(String server) {
        return WebtopNaming.getSiteID(server);
    }

    /**
     * @see WebtopNaming#getServerFromID(String)
     * @param server The ID of the Server as described in the naming table.
     * @return A URL based on the Servers entry in the naming table.
     * @throws ServerEntryNotFoundException If the server was not found in the naming table.
     */
    public String getServerFromID(String server) throws ServerEntryNotFoundException {
        return WebtopNaming.getServerFromID(server);
    }

    /**
     * @see WebtopNaming#getAMServerID()
     * @return The ID of the current Server.
     * @throws ServerEntryNotFoundException If there was a problem resolving this ID.
     */
    public String getAMServerID() throws ServerEntryNotFoundException {
        return WebtopNaming.getAMServerID();
    }

    /**
     * @see WebtopNaming#isSite(String)
     * @param siteID The ID of the Site or Server to test.
     * @return True if the ID belongs to a Site ID. False if not valid or is a Server ID.
     */
    public boolean isSite(String siteID) {
        return WebtopNaming.isSite(siteID);
    }

    /**
     * @see WebtopNaming#isServer(String)
     * @param serverID The ID of the Site or Server to test.
     * @return True if the ID belongs to a Server. False if not valid or is a Site ID.
     */
    public boolean isServer(String serverID) {
        return WebtopNaming.isServer(serverID);
    }

    /**
     * @see WebtopNaming#getSiteNodes(String)
     * @param siteID Non null Site ID.
     * @return The ServerIDs that make up the Site.
     * @throws Exception If there was a problem determining the Site nodes.
     */
    public Set<String> getSiteNodes(String siteID) throws Exception {
        return WebtopNaming.getSiteNodes(siteID);
    }

    /**
     * @see com.iplanet.services.naming.WebtopNaming.SiteMonitor#isAvailable(java.net.URL)
     * @param url Non null URL to test. This URL must point to an OpenAM Server
     *            instance.
     * @return True indicates that the Server is up and responding.
     */
    public boolean isAvailable(URL url) throws Exception {
        return WebtopNaming.SiteMonitor.isAvailable(url);
    }
}
