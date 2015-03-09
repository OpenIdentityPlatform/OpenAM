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
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import static com.sun.identity.shared.Constants.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.URLNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.debug.Debug;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionServiceURLService;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Responsible for collating WebtopNaming configuration state relating to the Session Service.
 *
 * @since 13.0.0
 */
@Singleton
public class SessionServerConfig {

    /*
     * Local server details are those of the server on which the code is executing.
     */
    private final String localServerID;
    private final String localServerProtocol;
    private final String localServerHost;
    private final int localServerPort;
    private final String localServerDeploymentPath;
    private final URL localServerURL;
    private final URL localServerSessionServiceURL;

    /*
     * siteEnabled must be True to permit Session Failover HA to be available.
     *
     * If siteEnabled==true and no site is found, issues will arise trying to resolve the serverID
     * and will hang install and subsequent login attempts.
     */
    private final boolean siteEnabled;

    /*
     * If siteEnabled, we will retrieve details of this site's primary server (usu. load balancer) and the primary
     * servers of any other sites included in this deployment.
     */
    private final String siteID;
    private final URL siteURL;
    private final Set<String> secondarySiteIDs;

    /**
     * Constructor called by Guice to initialize the Singleton instance of SessionServerConfig.
     *
     * Initialization success is dependent on {@link WebtopNaming } being ready.
     */
    @Inject
    SessionServerConfig(@Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                        SessionServiceURLService sessionServiceURLService) {

        try {

            localServerProtocol = requiredSystemProperty(AM_SERVER_PROTOCOL);
            localServerHost = requiredSystemProperty(AM_SERVER_HOST);
            final String localServerPortAsString = requiredSystemProperty(AM_SERVER_PORT);
            localServerPort = Integer.parseInt(localServerPortAsString);
            localServerDeploymentPath = requiredSystemProperty(AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
            // TODO: Establish whether or not the previous fields can be dropped in favour of WebtopNaming.getLocalServer()

            localServerURL = new URL(localServerProtocol, localServerHost, localServerPort, localServerDeploymentPath);

            localServerSessionServiceURL = sessionServiceURLService.getSessionServiceURL(
                    localServerProtocol, localServerHost, localServerPortAsString, localServerDeploymentPath);

            localServerID = WebtopNaming.getServerID(
                    localServerProtocol, localServerHost, localServerPortAsString, localServerDeploymentPath);

            siteEnabled = WebtopNaming.isSiteEnabled(
                    localServerProtocol, localServerHost, localServerPortAsString, localServerDeploymentPath);

            if (siteEnabled) {

                siteID = WebtopNaming.getSiteID(
                        localServerProtocol, localServerHost, localServerPortAsString, localServerDeploymentPath);
                siteURL = new URL(WebtopNaming.getServerFromID(siteID));
                secondarySiteIDs = findSecondarySiteIDs(
                        localServerProtocol, localServerHost, localServerPortAsString, localServerDeploymentPath);

            } else {

                siteID = null;
                siteURL = null;
                secondarySiteIDs = Collections.emptySet();

            }

        } catch (Exception ex) {
            sessionDebug.error("Failed to load Session Server configuration", ex);
            // Rethrow exception rather than hobbling on with invalid configuration state
            throw new IllegalStateException("Failed to load Session Server configuration", ex);
        }
    }

    /**
     * Gets the ID of the primary server.
     *
     * The server identified varies depending on whether or not this server is running as part of a site:
     *
     * - If a site is running, the primary server details refer to the site's primary server (usu. load balancer).
     * - If a site hasn't been setup, then the primary server details will match the local server details.
     */
    public String getPrimaryServerID() {
        return siteEnabled ? siteID : localServerID;
    }

    /**
     * Gets the URL of the primary server.
     *
     * The server identified varies depending on whether or not this server is running as part of a site:
     *
     * - If a site is running, the primary server details refer to the site's primary server (usu. load balancer).
     * - If a site hasn't been setup, then the primary server details will match the local server details.
     */
    public URL getPrimaryServerURL() {
        return siteEnabled ? siteURL : localServerURL;
    }

    /**
     * Gets ID for this OpenAM server.
     */
    public String getLocalServerID() {
        return localServerID;
    }

    /**
     * Gets the full URL for this OpenAM server.
     *
     * e.g. https://openam.example.com:8080/openam
     */
    public URL getLocalServerURL() {
        return localServerURL;
    }

    /**
     * Gets the full URL for this OpenAM server's session service servlet.
     *
     * e.g. https://openam.example.com:8080/openam/sessionservice
     */
    public URL getLocalServerSessionServiceURL() {
        return localServerSessionServiceURL;
    }

    /**
     * Checks if this OpenAM server is deployed as part of a site (or multiple sites).
     */
    public boolean isSiteEnabled() {
        return siteEnabled;
    }

    @VisibleForTesting
    Set<String> getSecondarySiteIDs() {
        return secondarySiteIDs;
    }

    /**
     * Checks if server instance identified by serverID is this OpenAM server.
     *
     * @param serverID server id, possibly null.
     * @return true if serverID is the same as local instance, false otherwise.
     */
    public boolean isLocalServer(String serverID) {
        return localServerID.equals(serverID);
    }

    /**
     * Checks if server instance identified by serverID is the primary server for this OpenAM server's site.
     *
     * How this check is performed varies depending on whether or not this server is running as part of a site:
     *
     * - If a site is running, the primary server details refer to the site's primary server (usu. load balancer).
     * - If a site hasn't been setup, then the primary server details will match the local server details.
     */
    public boolean isPrimaryServer(String serverID) {
        return siteEnabled ? siteID.equals(serverID) : localServerID.equals(serverID);
    }

    /**
     * This method is called by Session.getLocalServerSessionServiceURL, when routing a request to an individual session host. In
     * this case, the SessionID.PRIMARY_ID extension is obtained from the SessionID instance (which corresponds to the
     * AM-instance host of the session). WebtopNaming will then be called to turn this serverId (01,02, etc) into a
     * URL which will point a PLL client GetSession request. Calling this method is part of insuring that the PLL GetSession
     * request does not get routed to a site (load-balancer).
     * @param siteID the server id (PRIMARY_ID) pulled from a presented cookie.
     * @return true if the specified serverId is actually a site identifier for the current deployment
     */
    public boolean isLocalSite(String siteID) {
        // TODO: Investigate this method further and rename / better document its behaviour
        // How does this method compare to WebtopNaming.isSite? Is this method redundant?
        return this.siteID.equals(siteID) || secondarySiteIDs.contains(siteID);
    }

    /**
     * Checks whether the provided ID belongs to a site or not.
     *
     * @param serverOrSiteId The ID that needs to be checked.
     * @return <code>true</code> if the ID corresponds to a site.
     */
    public boolean isSite(String serverOrSiteId) {
        return WebtopNaming.isSite(serverOrSiteId);
    }

    /**
     * Checks if server instance identified by serverID is the same as local
     * instance
     *
     * @param sid server id
     * @return true if serverID is the same as local instance, false otherwise
     */
    public boolean isLocalSite(SessionID sid) {
        return isLocalSite(sid.getSessionServerID());
    }

    /**
     * Creates a new URL by adding the provided path to the end of this OpenAM server's URL.
     *
     * e.g. Given the path "GetHttpSession?op=create", if this OpenAM server is deployed to
     * https://openam.example.com:8080/openam then the resulting URL will be
     * https://openam.example.com:8080/openam/GetHttpSession?op=create
     */
    public URL createLocalServerURL(String path) throws MalformedURLException {
        return new URL(localServerProtocol, localServerHost, localServerPort, localServerDeploymentPath + "/" + path);
    }

    /**
     * Creates a new URL by adding the provided path to the end of the provided server's URL.
     *
     * This assumes that the provided server URL does not include the deployment path of OpenAM
     * since it can be assumed to be the same as this server's deployment path.
     *
     * e.g. Given the server URL "https://remote.example.com:8080" and the path "GetHttpSession?op=create",
     * if this OpenAM server is deployed to "/openam" then the resulting URL will be
     * https://remote.example.com:8080/openam/GetHttpSession?op=create
     */
    public URL createServerURL(URL server, String path) throws MalformedURLException {
        return new URL(server.getProtocol(), server.getHost(), server.getPort(), localServerDeploymentPath + "/" + path);
    }

    /**
     * Returns IDs for all servers in the current site. If this OpenAM server is not deployed as part
     * of one or more sites, the returned set will only include this server's ID.
     */
    public Set<String> getServerIDsInLocalSite() throws Exception {
        Set<String> serverIDs = WebtopNaming.getSiteNodes(siteID);
        if ((serverIDs == null) || (serverIDs.isEmpty())) {
            serverIDs = new HashSet<String>();
            serverIDs.add(localServerID);
        }
        return serverIDs;
    }

    /**
     * Returns all server IDs.
     */
    public List<String> getAllServerIDs() throws Exception {
        return WebtopNaming.getAllServerIDs();
    }

    /**
     * Returns the server URL based on the server ID.
     */
    public String getServerFromID(String serverID) throws ServerEntryNotFoundException {
        return WebtopNaming.getServerFromID(serverID);
    }

    /**
     * Returns true if the URL is the URL of the local session service.
     *
     * @param url the url to check
     * @return true if the url represents the local session service.
     */
    public boolean isLocalSessionService(URL url) {
        return isUrlPrefix(localServerSessionServiceURL, url);
    }

    /**
     * Returns true if the url is the URL of the local notification service.
     *
     * @param url the url to check
     * @return true if the url represents the local notification service.
     */
    public boolean isLocalNotificationService(URL url) {
        try {
            URL localURL = WebtopNaming.getNotificationURL();
            return isUrlPrefix(localURL, url);
        } catch (URLNotFoundException ex) {
            return false;
        }
    }

    /**
     * Returns the lbCookieValue corresponding to the server ID.
     */
    public String getLBCookieValue() {
        return WebtopNaming.getLBCookieValue(getLocalServerID());
    }

    /**
     * Checks if the first URL is a prefix of the second URL.
     *
     * Protocol and Host are compared case-insensitively but the path is compared case-sensitively.
     */
    private static boolean isUrlPrefix(URL firstURL, URL secondURL) {
        return firstURL != null
                && firstURL.getProtocol().equalsIgnoreCase(secondURL.getProtocol())
                && firstURL.getHost().equalsIgnoreCase(secondURL.getHost())
                && firstURL.getPort() == secondURL.getPort()
                && secondURL.getPath().startsWith(firstURL.getPath());
    }

    private String requiredSystemProperty(String key) throws SessionException {
        String value = SystemProperties.get(key);

        if (key == null) {
            throw new SessionException(SessionBundle.rbName, "propertyMustBeSet", null);
        }

        return value;
    }

    private Set<String> findSecondarySiteIDs(String protocol, String host, String port, String path)
            throws ServerEntryNotFoundException {

        // TODO: Investigate this method further - under what circumstances does it actually return results?

        Set<String> results = new HashSet<String>();

        String secondarySites = WebtopNaming.getSecondarySites(protocol, host, port, path);
        if (secondarySites != null) {
            if (secondarySites.contains("|")) {
                StringTokenizer st = new StringTokenizer(secondarySites, "|");
                while (st.hasMoreTokens()) {
                    results.add(st.nextToken());
                }
            } else {
                results.add(secondarySites);
            }
        }

        return Collections.unmodifiableSet(results);
    }

}
