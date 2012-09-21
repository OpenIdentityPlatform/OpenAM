/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServerSiteModel.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.common.configuration.ServerConfigXML;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface ServerSiteModel
    extends AMModel
{
    /**
     * Returns a set of site names.
     *
     * @return a set of site names.
     * @throws AMConsoleException if error occurs when getting the site names.
     */
    Set getSiteNames()
        throws AMConsoleException;

    /**
     * Returns primary URL of site.
     *
     * @param name Name of Site.
     * @return a primary URL of site.
     * @throws AMConsoleException if error occurs when getting the site primary
     *         URL.
     */
    String getSitePrimaryURL(String name) 
        throws AMConsoleException;

    /**
     * Returns a set of failover URLs of site.
     *
     * @param name Name of Site.
     * @return a set of failover URLs of site.
     * @throws AMConsoleException if error occurs when getting the site primary
     *         URL.
     */
    Set getSiteFailoverURLs(String name)
        throws AMConsoleException;
    
    /**
     * Returns a set of servers that belong to site.
     *
     * @param name Name of Site.
     * @return a set of servers that belong to site.
     * @throws AMConsoleException if error occurs when getting the servers set.
     */
    Set getSiteServers(String name)
        throws AMConsoleException;
    
    /**
     * Creates a site.
     *
     * @param name Name of Site.
     * @param url Primary URL of Site.
     * @throws AMConsoleException if error occurs when creating site.
     */
    void createSite(String name, String url) 
        throws AMConsoleException;

    /**
     * Creates a server.
     *
     * @param name Name of Server.
     * @throws AMConsoleException if error occurs when creating server.
     */
    void createServer(String name) 
        throws AMConsoleException;

    /**
     * Deletes a set of sites.
     *
     * @param sites Set of of sites to be deleted.
     * @throws AMConsoleException if error occurs when getting the site name.
     */
    void deleteSites(Set sites)
        throws AMConsoleException;

    /**
     * Returns a set of server names.
     *
     * @return a set of server names.
     * @throws AMConsoleException if error occurs when getting the server names.
     */
    Set getServerNames()
        throws AMConsoleException;

    /**
     * Returns a site name of which server belongs to.
     *
     * @param name Name of Server.
     * @return a site name.
     * @throws AMConsoleException if error occurs when getting the site name.
     */
    String getServerSite(String name) 
        throws AMConsoleException;

    /**
     * Deletes a set of servers.
     *
     * @param servers Set of of servers to be deleted.
     * @throws AMConsoleException if error occurs when getting the site name.
     */
    void deleteServers(Set servers)
        throws AMConsoleException;
    
    /**
     * Returns edit site title page.
     *
     * @param siteName Name of site.
     * @return edit site title page.
     */
    String getEditSitePageTitle(String siteName);
    
    /**
     * Modifies site profile.
     *
     * @param siteName name of site.
     * @param primaryURL Primary URL.
     * @param failoverURLs Failover URLs.
     * @throws AMConsoleException if site profile cannot be modified.
     */
    void modifySite(String siteName, String primaryURL, Set failoverURLs)
        throws AMConsoleException;

    /**
     * Clones a server.
     *
     * @param origServer Name of the server to be cloned.
     * @param cloneServer Name of clone server.
     * @throws AMConsoleException if server cannot be cloned.
     */
    void cloneServer(String origServer, String cloneServer)
        throws AMConsoleException;
    
    /**
     * Returns edit server title page.
     *
     * @param serverName Name of server.
     * @return edit server title page.
     */
    String getEditServerPageTitle(String serverName);
    
    /**
     * Returns server configuration.
     *
     * @param serverName name of server.
     * @return server configuration.
     * @throws AMConsoleException if server profile cannot be retrieved.
     */
    Map getServerConfiguration(String serverName)
        throws AMConsoleException;
    
    /**
     * Returns default server configuration.
     *
     * @return server configuration.
     */
    Map getServerDefaults();
    
    /**
     * Modifies server profile.
     *
     * @param serverName name of server.
     * @param parentSite Parent site.
     * @param values Property Values.
     * @throws AMConsoleException if server profile cannot be modified.
     * @throws UnknownPropertyNameException if property names are unknown.
     */
    void modifyServer(String serverName, String parentSite, Map values)
        throws AMConsoleException, UnknownPropertyNameException;

    /**
     * Updates server property inheritance settings.
     *
     * @param serverName Name of server.
     * @param toInherit Properties to inherit.
     * @param notToInherit Properties not to inherit.
     * @throws AMConsoleException if server profile cannot be updated.
     */
    void updateServerConfigInheritance(
        String serverName,
        Set toInherit, 
        Set notToInherit
    ) throws AMConsoleException;
    
    /**
     * Returns the server configuration XML object.
     *
     * @param serverName Name of server.
     * @return the server configuration XML object.
     * @throws AMConsoleException if server configuration XML object cannot be
     *         return.
     */
    ServerConfigXML getServerConfigObject(String serverName) 
        throws AMConsoleException;
    
    /**
     * Sets the server configuration XML.
     *
     * @param serverName Name of server.
     * @param xml the server configuration XML.
     * @throws AMConsoleException if server configuration XML cannot be set.
     */
    void setServerConfigXML(String serverName, String xml)
        throws AMConsoleException;
}


