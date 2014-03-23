/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SystemConfigurationUtil.java,v 1.7 2008/08/06 17:26:14 exu Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2014 ForgeRock AS
 */

package com.sun.identity.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import java.util.Collection;

/**
 * The <code>SystemConfigurationUtil</code> class provides methods to get
 * attributes defined in "systemConfig" configuration. It also has a method
 * getProperty to get properties in "FederationConfig.properties".
 */
public final class SystemConfigurationUtil implements ConfigurationListener {
    public static final String PROP_SERVER_MODE =
        "com.sun.identity.common.serverMode";
    private static final String SVC_PLATFORM = "PLATFORM";
    private static final String SVC_NAMING = "NAMING";

    private static ConfigurationInstance platformConfig;
    private static ConfigurationInstance namingConfig;
    private static String authenticationURL;
    private static List cookieDomains;
    private static List<String> serverList;
    private static List<String> siteList;
    private static Hashtable<String,String> serverToIdTable = null;
    private static Hashtable<String,String> idToServerTable = null;
    private static HashMap<String,String> idToSiteTable = null;
    private static HashMap<String,String> siteToIdTable = null;
    private static boolean platformNamingInitialized = false;

    private SystemConfigurationUtil() {
    }

    private static Debug getDebug() {
        return Debug.getInstance("libPlugins");        
    }

    /**
     * Returns authentication URL.
     * @return authentication URL, return null if the authentication URL
     *     could not be found.
     */
    public static String getAuthenticationURL()
        throws SystemConfigurationException {

        if (!platformNamingInitialized) {
            initPlatformNaming();
        }
        return authenticationURL;
    }
    
    /**
     * Returns list of cookie domains.
     * @return list of cookie domains, return empty list if no cookie domain
     *     is defined.
     */
    public static List getCookieDomains() throws SystemConfigurationException {
        if (!platformNamingInitialized) {
            initPlatformNaming();
        }
        return cookieDomains;
    }

    /** 
     * Returns all the instances in the server cluster this instance belongs to.
     * @return list of server names. 
     * @throws SystemConfigurationException if unable to get the server list.
     */ 
    public static List getServerList() throws SystemConfigurationException {
        if (!platformNamingInitialized) {
            initPlatformNaming();
        }
        return serverList;
    }

    /**
     * Returns all service urls.
     * @return list of server names.
     * @throws SystemConfigurationException if unable to get the server list.
     */
    public static Collection getServiceAllURLs(String serviceName)
    throws SystemConfigurationException {
        // TODO: Is this implementation still used?
        if (!platformNamingInitialized) {
            initPlatformNaming();
        }

        if (serviceName == null) {
            throw new SystemConfigurationException("missingServiceName");
        }

        Collection allurls = null;
        String name = "iplanet-am-naming-" + serviceName.toLowerCase() + "-url";

        Set<String> values = null;
        
        try {
            values = (Set<String>) namingConfig.getConfiguration(null, null).get(name);
        } catch (ConfigurationException cex) {
            getDebug().error("SystemConfigurationUtil.getServiceURL:", cex);
        }
        
        if ((values) == null || values.isEmpty()) {
            Object[] data = { serviceName };
            throw new SystemConfigurationException("noServiceURL", data);
        }

        for (String url : values) {
            if (url != null) {
                try {
                    allurls.add(new URL(url));
                } catch (MalformedURLException muex) {
                    Object[] data = { serviceName };
                    throw new SystemConfigurationException("noServiceURL", data);
                }
            } else {
                Object[] data = { serviceName };
                throw new SystemConfigurationException("noServiceURL", data);
            }
        }

        return allurls;
    }

    /**
     * Returns all the sites instance.
     * @return list of site names.
     * @throws SystemConfigurationException if unable to get the server list.
     */
    public static List getSiteList() throws SystemConfigurationException {
        if (!platformNamingInitialized) {
            initPlatformNaming();
        }
        return siteList;
    }

    /**
     * Returns <code>true</code> if this is running on server mode.
     *
     * @return <code>true</code> if this is running on server mode.
     */
    public static boolean isServerMode() {
        return Boolean.valueOf(System.getProperty(PROP_SERVER_MODE,
            getProperty(PROP_SERVER_MODE, "false"))).booleanValue();
    }

    /**
     * Returns the URL of the specified service on the specified host.
     * @param serviceName The name of the service.
     * @param protocol The service protocol.
     * @param hostname The service host name.
     * @param port The service listening port.
     * @param uri The service URI.
     * @return The URL of the specified service on the specified host.
     * @throws SystemConfigurationException if the URL could not be found.
     */
    public static URL getServiceURL(
        String serviceName,
        String protocol,
        String hostname,
        int port,
        String uri
    ) throws SystemConfigurationException {

        if (!platformNamingInitialized) {
            initPlatformNaming();
        }

        if ((protocol == null) || (protocol.length() == 0) ||
            (hostname == null) || (hostname.length() == 0)) {

            throw new SystemConfigurationException("missingProtHost");
        }

        String name = "iplanet-am-naming-" + serviceName.toLowerCase()+ "-url";
        Set values = null;
        try {
            values = (Set)namingConfig.getConfiguration(null, null).get(name);
        } catch (ConfigurationException cex) {
            getDebug().error("SystemConfigurationUtil.getServiceURL:", cex);
        }
        if ((values) == null || values.isEmpty()) {
            Object[] data = { serviceName };
            throw new SystemConfigurationException("noServiceURL", data);
        }

        String url = (String)values.iterator().next();
        if (url != null) {
            if ((url.indexOf("%") != -1) &&
                (!validate(protocol, hostname, port, uri))) {

                Object[] data = { serviceName };
                throw new SystemConfigurationException("noServiceURL", data);
            }

            int idx;
            if ((idx = url.indexOf("%protocol")) != -1) {
                url = url.substring(0,idx) + protocol+
                      url.substring(idx+"%protocol".length(), url.length());
            }
    
            if ((idx =url.indexOf("%host")) != -1) {
                url = url.substring(0,idx) + hostname +
                      url.substring(idx + "%host".length(), url.length());
            }

            if ((idx =url.indexOf("%port")) != -1) {
                url = url.substring(0,idx) + port +
                      url.substring(idx + "%port".length(), url.length());
            }

            if ((uri != null) && (idx =url.indexOf("%uri")) != -1) {
                url = url.substring(0,idx) + uri +
                      url.substring(idx + "%uri".length(), url.length());
            }
            try {
                return new URL(url);
            } catch (MalformedURLException muex) {
                Object[] data = { serviceName };
                throw new SystemConfigurationException("noServiceURL", data);
            }
        } else {
            Object[] data = { serviceName };
            throw new SystemConfigurationException("noServiceURL", data);
        }
    }

    /**
     * Returns the server id corresponding to a server instance.
     * There is a one-to-one mapping between the server id and the instance.
     * @param protocol The service protocol of the server instance.
     * @param hostname The service host name of the server instance.
     * @param port The service listening port of the server instance.
     * @param uri The service URI of the server instance.
     * @return the server id corresponding to the server instance
     * @throws SystemConfigurationException if the server id corresponding to
     *     the server instance could not be found.
     */
    public static String getServerID(
        String protocol,
        String hostname,
        int port,
        String uri
    ) throws SystemConfigurationException {

        if (!platformNamingInitialized) {
            initPlatformNaming();
        }

        if ((protocol == null) || (protocol.length() == 0) ||
            (hostname == null) || (hostname.length() == 0)) {

            throw new SystemConfigurationException("missingProtHost");
        }

        
        String server = (uri != null) ?
            protocol + ":" + "//" + hostname + ":" + port + uri :
            protocol + ":" + "//" + hostname + ":" + port;
        server = server.toLowerCase();
        String serverID = (String)serverToIdTable.get(server);
        if (serverID == null) {
            Object[] data = { server };
            throw new SystemConfigurationException("noServerID", data);
        }

        return serverID;
    }

    /**
     * Returns the server instance corresponding to the server id.
     * @param id server id
     * @return server instance name, e.g. "http://abc.sun.com:58080".
     * @throws SystemConfigurationException if the server name corresponding
     *     to the server id does not exist.
     */
    public static String getServerFromID(String id)
        throws SystemConfigurationException {

        if (!platformNamingInitialized) {
            initPlatformNaming();
        }

        String server = (String)idToServerTable.get(id);
        if (server == null) {
            Object[] data = { id };
            throw new SystemConfigurationException("noServerFromID", data);
        }
        return server;
    }

    /**
     * Check whether the given id is a siteId.
     * 
     * @param siteID
     *            The site id to check.
     * @return true, if the given siteID is an existing site id.
     */
    public static boolean isSiteId(String siteID) {
        if (!platformNamingInitialized) {
            initPlatformNaming();
        }

        return idToSiteTable.containsKey(siteID);
    }

    /**
     * Initializes the properties map.
     *
     * @param properties Map of new properties.
     */
    public static void initializeProperties(Properties properties) {
        SystemPropertiesManager.initializeProperties(properties);
    }

    /**
     * Initializes the properties map.
     *
     * @param propertyName Name of properties.
     * @param propertyValue Value of properties.
     */
    public static void initializeProperties(
        String propertyName,
        String propertyValue
    ) {
        SystemPropertiesManager.initializeProperties(
            propertyName, propertyValue);
    }

    /**
     * Returns property value corresponding to the property name. 
     * @param propertyName name of the property whose value to be returned. 
     * @return the value of the property, returns null if the property does
     *     not exist.
     */  
    public static String getProperty(String propertyName) {
        return SystemPropertiesManager.get(propertyName);
    }

    /**
     * Returns property value corresponding to the property name. If the
     * property is not found, the default value is returned. 
     * @param propertyName name of the property whose value to be returned. 
     * @param defaultValue the default value.
     * @return the value of the property, returns null if the property does
     *     not exist.
     */
    public static String getProperty(String propertyName, String defaultValue) {
        String value = SystemPropertiesManager.get(propertyName);
        return (value == null ? defaultValue : value);
    }

    /**
     * This function stores the server list by parsing platform server list
     * that are stored in <code>iPlanetAMPlatformService</code>.
     * This would expect the servers from the platform service are in the
     * following format
     *   protocol://server.domain:port|serverId
     *   e.g. http://shivalik.red.iplanet.com:58080|01
     *        http://solpuppy.red.iplanet.com:58081|02
     * The serverId can be anything and does not need to be a number
     * If the platform server is not in the correct format, that
     * entry will be ignored.
     * Note: This server id should be unique if it's participating
     * in load balancing mode. 
     */
    private static void storeServerAndSiteList(Set<String> servers, Set<String> sites) {
        int numberOfServers = servers != null ? servers.size() : 0;
        int numberOfSites = sites != null ? sites.size() : 0;
        int numberOfItems = numberOfServers + numberOfSites;

        if (numberOfItems == 0) {
            serverList = Collections.EMPTY_LIST;
            serverToIdTable = null;
            idToServerTable = null;
            return;
        }

        serverList = new ArrayList(numberOfItems);
        serverToIdTable = new Hashtable(numberOfItems);
        idToServerTable = new Hashtable(numberOfItems);

        if (servers != null) {
            for (String serverEntry : servers) {
                try {
                    ServerOrSiteEntry entry = new ServerOrSiteEntry(serverEntry);
                    serverList.add(entry.getUrl());
                    idToServerTable.put(entry.getId(), entry.getUrl());
                    serverToIdTable.put(entry.getUrl(), entry.getId());
                    if (getDebug().messageEnabled()) {
                        getDebug().message("SystemConfigUtil.storeServerAndSiteList: " +
                            "adding server " + entry.getId() + ": " + entry.getUrl());
                    }
                } catch (IllegalArgumentException ex) {
                    getDebug().error("SystemConfigurationUtil.storeServerAndSiteList: " +
                            "Platform Server List entry is invalid:" + serverEntry);
                }
            }
        }

        if (sites != null) {
            if (numberOfSites == 0) {
                siteList = Collections.EMPTY_LIST;
            } else {
                siteList = new ArrayList<String>(numberOfSites);
                idToSiteTable = new HashMap<String, String>(numberOfSites);
                siteToIdTable = new HashMap<String, String>(numberOfSites);
            }

            for (String siteEntry : sites) {
                try {
                    ServerOrSiteEntry entry = new ServerOrSiteEntry(siteEntry);
                    serverList.add(entry.getUrl());
                    siteList.add(entry.getUrl());
                    idToServerTable.put(entry.getId(), entry.getUrl());
                    serverToIdTable.put(entry.getUrl(), entry.getId());
                    idToSiteTable.put(entry.getId(), entry.getUrl());
                    siteToIdTable.put(entry.getUrl(), entry.getId());
                    if (getDebug().messageEnabled()) {
                        getDebug().message("SystemConfigUtil.storeServerAndSiteList: " +
                            "adding site " + entry.getId() + ": " + entry.getUrl());
                    }
                } catch (IllegalArgumentException ex) {
                    getDebug().error("SystemConfigurationUtil.storeServerAndSiteList: " +
                            "Platform Site List entry is invalid:" + siteEntry);
                }
            }
        }
    }

    private static boolean validate(
        String protocol,
        String host,
        int port,
        String uri
    ) {
        String server = (uri != null) ? 
            protocol + "://" + host + ":" + port + uri :
            protocol + "://" + host + ":" + port;
        server = server.toLowerCase();
        return (serverList.contains(server));
    }

    /**
     * This method will be invoked when a component's 
     * configuration data has been changed. The parameters componentName,
     * realm and configName denotes the component name,
     * organization and configuration instance name that are changed 
     * respectively.
     *
     * @param event Configuration action event, like ADDED, DELETED, MODIFIED
     *     etc.
     */
    public void configChanged(ConfigurationActionEvent event) {
        if (getDebug().messageEnabled()) {
            getDebug().message("SystemConfigurationUtil.configChanged: " +
                "type = " + event.getType() + ", configuration name = " +
                event.getConfigurationName() + ", component name = " +
                event.getComponentName() + ", realm = " + event.getRealm());
        }

        try {
            update();
        } catch (ConfigurationException ex) {
            getDebug().error("SystemConfigurationUtil.configChanged: ", ex);
        }
    }

    private static synchronized void update() throws ConfigurationException {
        Map avPairs = platformConfig.getConfiguration(null, null);
        if ((avPairs == null) || avPairs.isEmpty()) {
            authenticationURL = null;
            cookieDomains = Collections.EMPTY_LIST;
            serverList = Collections.EMPTY_LIST;
            serverToIdTable = null;
            idToServerTable = null;
        } else {
            Set values = (Set)avPairs.get(Constants.ATTR_LOGIN_URL);
            if ((values == null) || values.isEmpty()) {
                authenticationURL = null;
            } else {
                authenticationURL = (String)values.iterator().next();
            }

            values =(Set)avPairs.get(Constants.ATTR_COOKIE_DOMAINS);
            if ((values == null) || values.isEmpty()) {
                cookieDomains = Collections.EMPTY_LIST;
            } else {
                cookieDomains = new ArrayList();
                cookieDomains.addAll(values);
            }

            Set<String> servers = (Set)avPairs.get(Constants.PLATFORM_LIST);
            Set<String> sites = (Set)avPairs.get(Constants.SITE_LIST);

            if (getDebug().messageEnabled()) {
                getDebug().message("SystemConfigUtil.update: " +
                    "servers=" + servers);
                getDebug().message("SystemConfigUtil.update: " +
                    "sites=" + sites);
            }

            storeServerAndSiteList(servers, sites);
        }
    }

    private static synchronized void initPlatformNaming() {

        if (platformNamingInitialized) {
            return;
        }

        try {
            platformConfig =
                ConfigurationManager.getConfigurationInstance(SVC_PLATFORM);
            platformConfig.addListener(new SystemConfigurationUtil());

        } catch (ConfigurationException cex) {
            getDebug().error("SystemConfigurationUtil.initPlatformNaming: unable to get " +
                "platform configuration.", cex);
        }

        try {
            namingConfig =
                ConfigurationManager.getConfigurationInstance(SVC_NAMING);
        } catch (ConfigurationException cex) {
            getDebug().error("SystemConfigurationUtil.initPlatformNaming: unable to get " +
                "naming configuration.", cex);
        }
        
        try {
            update();
            platformNamingInitialized = true;
        } catch (ConfigurationException ex) {
            getDebug().error("SystemConfigurationUtil.initPlatformNaming: ", ex);
        }
    }

    /**
     * Checks whether the given serverID is a really existing serverid in the
     * current configuration. It returns false if the given id is a siteID!
     * 
     * @param serverID
     *            true if the given serverID is a valid serverID.
     */
    public static boolean isValidServerId(String serverID) {
        if (isSiteId(serverID)) {
            return false;
        }
        return idToServerTable.containsKey(serverID);
    }
}
