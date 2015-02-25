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
package com.iplanet.dpro.session.service;

import com.iplanet.dpro.session.Session;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.sun.identity.shared.debug.Debug;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible for providing supporting functions for the {@link WebtopNaming} service.
 *
 * In particular translating concepts that are based on users Sessions into
 * those which WebtopNaming operates in.
 */
public class WebtopNamingSiteUtils {
    private final Debug debug;
    private final WebtopNamingQuery query;

    /**
     * Guice initialised default constructor.
     *
     * @param debug Required for debugging.
     * @param query Required for performing WebtopNaming operations.
     */
    @Inject
    public WebtopNamingSiteUtils(@Named(SessionConstants.SESSION_DEBUG) Debug debug, WebtopNamingQuery query) {
        this.debug = debug;
        this.query = query;
    }

    /**
     * Determines if the Session is associated with a Server that is part of a
     * Site. If this is the case then the function will determine the other
     * Servers in the cluster.
     *
     * @param session A non null Session which must be associated with a Server ID.
     *
     * @return A set of the nodes that make up the cluster or Site. If the server
     * is not part of a Site then just the Server ID will be returned in the Set.
     *
     * @throws IllegalStateException If WebtopNaming was unable to find the IDs
     * for Servers in the Site. Unlikely as at this point it has indicated that
     * the Server in question is part of a Site.
     */
    public Set<String> getSiteNodes(Session session) {
        String siteOrServerID = session.getID().getSessionServerID();
        String siteID;

        if (query.isSite(siteOrServerID)) {
            siteID = siteOrServerID;
        } else {
            // Check for the Servers Site.
            siteID = query.getSiteID(siteOrServerID);

            if (siteID == null) {
                // This server has no site.
                return new HashSet<String>(Arrays.asList(siteOrServerID));
            }
        }

        try {
            return query.getSiteNodes(siteID);
        } catch (Exception e) {
            String err = "Failed to find nodes for Site ID: " + siteOrServerID;
            debug.error(err, e);
            throw new IllegalStateException(err);
        }
    }

    /**
     * Determines the Site (if any) that the Session is based in, and then calculates
     * the Servers that make up that Site.
     *
     * @param session Non null Session to use for query.
     *
     * @return The URLs for each node in the Site that the Session belongs to. If the
     * Session comes from a Server that is not part of a Site, then just the URL for
     * that Server will be returned. Never null.
     *
     * @throws IllegalStateException Thrown if WebtopNaming returned an invalid URL.
     */
    public Set<URL> getSiteNodeURLs(Session session) {
        Set<URL> urls = new HashSet<URL>();
        for (String node : getSiteNodes(session)) {
            try {
                URL url = new URL(query.getServerFromID(node));
                urls.add(url);
            } catch (MalformedURLException e) {
                String err = "WebtopNaming returned invalid URL";
                debug.error(err, e);
                throw new IllegalStateException(err, e);
            } catch (ServerEntryNotFoundException e) {
                debug.error("Server not found.", e);
                continue;
            }
        }
        return urls;
    }
}
