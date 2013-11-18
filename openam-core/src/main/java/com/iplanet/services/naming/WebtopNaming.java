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
 * $Id: WebtopNaming.java,v 1.31 2009/06/20 06:17:02 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock AS
 */
package com.iplanet.services.naming;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.monitoring.Agent;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.client.SendRequestException;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.naming.service.NamingService;
import com.iplanet.services.naming.share.NamingBundle;
import com.iplanet.services.naming.share.NamingRequest;
import com.iplanet.services.naming.share.NamingResponse;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

/*
 *  these are here temporarily, just to see how long it takes to
 *  build the monitoring stuff
 */
import java.util.Date;
import java.text.SimpleDateFormat;
import com.sun.identity.monitoring.SSOServerInfo;

/*
 *  and this stuff is here for retrieving session config
 */


/**
 * The <code>WebtopNaming</code> class is used to get URLs for various
 * services such as session, profile, logging etc. The lookup is based on the
 * service name and the host name. The Naming Service shall contain URLs for all
 * services on all servers. For instance, two machines might host session
 * services. The Naming Service profile may look like the following:
 * 
 * <pre>
 *      host1.session.URL=&quot;http://host1:8080/SessionServlet&quot;
 *      host2.session.URL=&quot;https://host2:9090/SessionServlet&quot;
 * </pre>
 */

public class WebtopNaming {
     
    /**
     * The unique identifier for the Naming Service. 
     */
    public static final String NAMING_SERVICE = "com.iplanet.am.naming";

    /**
     * The delimiter used to separate server IDs in the service attribute.
     */
    public static final String NODE_SEPARATOR = "|";

    private static final String AM_NAMING_PREFIX = "iplanet-am-naming-";

    private static final String FAM_NAMING_PREFIX = "sun-naming-";

    private static Hashtable namingTable = null;

    private static Hashtable serverIdTable = null;

    private static Hashtable<String, String> siteIdTable = null;

    private static Set<String> serverIDs = null;
    private static Set<String> siteIDs = null;
    private static Set<String> secondarySiteIDs = null;
    private static Map<String, String> siteNameToIdTable = null;

    //This is created for storing server id and lbcookievalue mapping
    //key:serverid | value:lbcookievalue      
    private static Hashtable lbCookieValuesTable = null;

    private static Vector platformServers = new Vector();

    //This is created for ignore case comparison
    private static Vector lcPlatformServers = new Vector();

    private static String namingServiceURL[] = null;

    private static Vector platformServerIDs = new Vector();

    /**
     * The debug instance.
     */    
    protected static Debug debug;

    private static boolean serverMode;

    private static boolean sitemonitorDisabled;

    private static String amServerProtocol = null;

    private static String amServer = null;

    private static String amServerPort = null;
    
    private static String amServerURI;

    private static SiteMonitor monitorThread = null;

    private static String MAP_SITE_TO_SERVER =
        "com.iplanet.am.naming.map.site.to.server";
    
    private static Map mapSiteToServer = new HashMap();

    static {
        initialize();
    }

    public static void initialize() {
        serverMode = Boolean.valueOf(SystemProperties.get(
                        Constants.SERVER_MODE, "false")).booleanValue();
        sitemonitorDisabled = Boolean.valueOf(SystemProperties.get(
                Constants.SITEMONITOR_DISABLED,"false")).booleanValue();
        
        if (!serverMode) {
            String v = SystemProperties.get(MAP_SITE_TO_SERVER);
            if ((v != null) && (v.length() > 0)) {
                StringTokenizer st = new StringTokenizer(v, ",");
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    int idx = s.indexOf('=');
                    if (idx != -1) {
                        addToMapSiteToServer(s.substring(0, idx),
                            s.substring(idx+1));
                    }
                }
            }
        } 
        
        try {
            getAMServer();
            debug = Debug.getInstance("amNaming");
        } catch (Exception ex) {
            debug.error("Failed to initialize server properties", ex);
        }
    }

    private static void addToMapSiteToServer(String u1, String u2) {
        try {
            URL url1 = new URL(u1);
            URL url2 = new URL(u2);
            mapSiteToServer.put(url1, url2);
        } catch (MalformedURLException e) {
            debug.error("WebtopNaming.addToMapSiteToServer", e);
        }
    } 

    /**
     * Determines whether WebtopNaming code runs in the core server mode
     * or in the client SDK run-time mode.
     *
     * @return <code>true</code> running in the core server mode,
     *     <code>false</code> otherwise 
     */
    public static boolean isServerMode() {
        return serverMode;
    }

    /**
     * Tells whether the provided ID belongs to a server or not.
     *
     * @param serverID The ID that needs to be checked.
     * @return <code>true</code> if the ID corresponds to a server.
     */
    public static boolean isServer(String serverID) {
        return serverIDs.contains(serverID);
    }

    /**
     * Tells whether the provided ID belongs to a site or not.
     *
     * @param siteID The ID that needs to be checked.
     * @return <code>true</code> if the ID corresponds to a site.
     */
    public static boolean isSite(String siteID) {
        return siteIDs.contains(siteID);
    }

    /**
     * Tells whether the provided ID belongs to a secondary site or not.
     *
     * @param secondarySiteID The ID that needs to be checked.
     * @return <code>true</code> if the ID corresponds to a secondary site.
     */
    public static boolean isSecondarySite(String secondarySiteID) {
        return secondarySiteIDs.contains(secondarySiteID);
    }

    /**
     * Determines whether Site is enabled for the given server instance.
     *
     * @param protocol protocol of the server instance
     * @param host host of the server instance
     * @param port port of the server instance
     * @param uri uri of the server instance
     * 
     * @return <code>true</code> if Site is enabled, 
     *     <code>false</code> otherwise  
     *
     * @throws Exception if server entry is not found or there is any
     *     other run-time error
     */
    public static boolean isSiteEnabled(
        String protocol,
        String host,
        String port, 
        String uri
    ) throws Exception {
        String serverid = getServerID(protocol, host, port, uri);
        return isSiteEnabled(serverid);
    }

    /**
     * Determines whether Site is enabled for the given server ID.
     * @param serverid server ID
     *
     * @return <code>true</code> if Site is enabled, 
     *     <code>false</code> otherwise
     *
     * @throws Exception if the given server ID is null
     */
    public static boolean isSiteEnabled(String serverid) throws Exception {
        String siteid = siteIdTable.get(serverid);
        return (!serverid.equals(siteid));
    }

    /**
     * Returns the server ID.  
     *
     * @return Server ID 
     *
     * @throws ServerEntryNotFoundException if the Naming Service
     * can not find that server entry
     */
    public static String getAMServerID() throws ServerEntryNotFoundException {
        return getServerID(amServerProtocol, amServer, amServerPort, 
            amServerURI);
    }

    private static void getAMServer() {
        amServer = SystemProperties.get(Constants.AM_SERVER_HOST);
        amServerPort = SystemProperties.get(Constants.AM_SERVER_PORT);
        amServerProtocol = SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
        amServerURI = SystemProperties.get(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    }

    private static void initializeNamingService() {
        try {
            // Initilaize the list of naming URLs
            getNamingServiceURL();
            if (!serverMode && !sitemonitorDisabled) {
                startSiteMonitor(namingServiceURL);
            }
        } catch (Exception ex) {
            debug.error("Failed to initialize naming service", ex);
        }
    }

    /**
     * Returns the URL of the specified service on the specified
     * host.
     * 
     * @param service  the name of the service.
     * @param protocol the service protocol
     * @param host the service host name
     * @param port the service listening port
     * @param uri the deployment uri
     *
     * @return the URL of the specified service on the specified host.
     *
     * @throws URLNotFoundException if the Naming Service can not
     *     find a URL for a specified service
     */
    public static URL getServiceURL(String service, String protocol,
            String host, String port, String uri) throws URLNotFoundException {
        return (getServiceURL(service, protocol, host, port, uri, serverMode));
    }

    /**
     * Returns the URL of the specified service on the specified host.
     * 
     * @param service the name of the service.
     * @param url the deployment URI.
     * @param validate a boolean value indicate whether or not to 
     *    validate the protocol, host and port of the server
     *
     * @return the URL of the specified service on the specified host.
     *
     * @throws URLNotFoundException if the Naming Service can not
     *     find a URL for a specified service
     */
    public static URL getServiceURL(String service, URL url, boolean validate)
        throws URLNotFoundException {
        return getServiceURL(service, url.getProtocol(), 
            url.getHost(), Integer.toString(url.getPort()), url.getPath(),
            validate);
    }

    /**
     * Returns the URL of the specified service on the specified host.
     * 
     * @param service the name of the service.
     * @param protocol the service protocol.
     * @param host the service host name.
     * @param port the ervice listening port.
     * @param validate a boolean value indicate whether or not to 
     *    validate the protocol, host and port of the server
     *
     * @return the URL of the specified service on the specified host.
     *
     * @throws URLNotFoundException if the Naming Service can not
     *     find a URL for a specified service
     */
    public static URL getServiceURL(
        String service, 
        String protocol,
        String host,
        String port,
        boolean validate
    ) throws URLNotFoundException {
        String namingURL = SystemProperties.get(Constants.AM_NAMING_URL);
        try {
            String uri = getURI(new URL(namingURL));
            return getServiceURL(service, protocol, host, port, uri, validate);
        } catch (MalformedURLException ex) {
            throw new URLNotFoundException(ex.getMessage());
        }
        
    }
    
    public static URL mapSiteToServer(
        String protocol,
        String host,
        String port,
        String uri
    ) throws URLNotFoundException {
        if (!mapSiteToServer.isEmpty()) {
            try {
                URL url = new URL(protocol + "://" + host + ":" + port + uri);
                return (URL) mapSiteToServer.get(url);
            } catch (MalformedURLException e) {
                throw new URLNotFoundException(e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Returns the URL of the specified service on the specified host.
     * 
     * @param service name of the service.
     * @param protocol service protocol.
     * @param host service host name.
     * @param port service listening port.
     * @param uri the deployment uri.
     * @param validate a boolean value indicate whether or not to 
     *    validate the protocol, host and port of the server.
     *
     * @return the URL of the specified service on the specified host.
     *
     * @throws URLNotFoundException if the Naming Service can not
     *     find a URL for a specified service
     */
    public static URL getServiceURL(
        String service, 
        String protocol,
        String host,
        String port,
        String uri,
        boolean validate
    ) throws URLNotFoundException {
        try {
            // check before the first naming table update to avoid deadlock
            // uri can be empty string for pre-OpenSSO 8.0 releases
            if ((protocol == null) || (host == null) || (port == null) ||
                (uri == null) || (protocol.length() == 0) ||
                (host.length() == 0) || (port.length() == 0)
            ) {
                throw new Exception(NamingBundle.getString("noServiceURL")
                        + service);
            }
            
            URL mappedURL = mapSiteToServer(protocol, host, port, uri);
            if (mappedURL != null) {
                protocol = mappedURL.getProtocol();
                host = mappedURL.getHost();
                port = Integer.toString(mappedURL.getPort());
                uri = mappedURL.getPath();
            }
            if (namingTable == null) {
                getNamingProfile(false);
            }
            String url = null;

            String name = AM_NAMING_PREFIX + service.toLowerCase() + "-url";
            url = (String) namingTable.get(name);
            if (url == null) {
                name = FAM_NAMING_PREFIX + service.toLowerCase() + "-url";
                url = (String) namingTable.get(name);
            }
            
            if (url != null) {
                // If replacement is required, the protocol, host, and port
                // validation is needed against the server list
                // (iplanet-am-platform-server-list)
                if (validate && url.indexOf("%") != -1) {
                    validate(protocol, host, port, uri);
                }
                // %protocol processing
                int idx;
                if ((idx = url.indexOf("%protocol")) != -1) {
                    url = url.substring(0, idx)
                            + protocol
                            + url.substring(idx + "%protocol".length(), url
                                    .length());
                }

                // %host processing
                if ((idx = url.indexOf("%host")) != -1) {
                    url = url.substring(0, idx)
                            + host
                            + url.substring(idx + "%host".length(), url
                                    .length());
                }

                // %port processing
                if ((idx = url.indexOf("%port")) != -1) {
                    // plugin the server name
                    url = url.substring(0, idx)
                            + port
                            + url.substring(idx + "%port".length(), url
                                    .length());
                }
                
                // %uri processing
                // uri can be null for previous releases.
                if ((uri != null) && ((idx = url.indexOf("%uri")) != -1)) {
                    int test = uri.lastIndexOf('/');
                    while (test > 0) {
                        uri = uri.substring(0, test);
                        test = uri.lastIndexOf('/');
                    }

                    url = url.substring(0, idx) + uri + 
                        url.substring(idx + "%uri".length(), url.length());
                }

                return new URL(url);
            } else {
                throw new Exception(NamingBundle.getString("noServiceURL")
                        + service);
            }
        } catch (Exception e) {
            throw new URLNotFoundException(e.getMessage());
        }
    }

    /**
     * Returns all the URLs of the specified service based on the
     * servers in platform server list.
     * 
     * @param service the name of the service.
     *
     * @return the URL of the specified service on the specified host.
     *
     * @throws URLNotFoundException if the Naming Service can not
     *     find a URL for a specified service
     */
    public static Vector getServiceAllURLs(String service)
            throws URLNotFoundException {
        Vector allurls = null;

        try {
            if (namingTable == null) {
                getNamingProfile(false);
            }

            String name = AM_NAMING_PREFIX + service.toLowerCase() + "-url";
            String url = (String)namingTable.get(name);
            if (url == null) {
                name = FAM_NAMING_PREFIX + service.toLowerCase() + "-url";
                url = (String)namingTable.get(name);
            }

            if (url != null) {
                allurls = new Vector();
                if (monitorThread == null) {
                    allurls.add(getServiceURL(service, amServerProtocol,
                        amServer, amServerPort, amServerURI));
                } else {
                    if (url.indexOf("%") != -1) {
                        Vector servers =  SiteMonitor.getAvailableSites();
                        Iterator it = servers.iterator();
                        while (it.hasNext()) {
                            String server = getServerFromID((String)it.next());
                            URL serverURL = new URL(server);
                            allurls.add(getServiceURL(service,
                                serverURL.getProtocol(), serverURL.getHost(),
                                String.valueOf(serverURL.getPort()), 
                                serverURL.getPath()));
                        }
                    } else {
                        allurls.add(new URL(url));
                    }
                }
            }

            return allurls;
        } catch (Exception e) {
            throw new URLNotFoundException(e.getMessage());
        }
    }

    /**
     * Returns the platform server list. Note: Calling this method would
     * cause performance impact, as it involves xml request over the wire.
     *
     * @return platform server list
     *      
     * @throws Exception if an error occurs when updating the
     *     nameing table
     */
    public static Vector getPlatformServerList() throws Exception {
         return getPlatformServerList(true);
    }

    /**
     * Returns the platform server list.
     *
     * @param update a boolean flag indicating whether a refresh of the
     * naming profile is needed.
     *
     * @return platform server list
     *
     * @throws Exception if an error occurs when updating the
     *     nameing table
     */

    public static Vector getPlatformServerList(boolean update)
             throws Exception {
         getNamingProfile(update);
         return platformServers;
    }

    /**
     * Returns key value from a hashtable, ignoring the case of the
     * key.
     */
    private static String getValueFromTable(Hashtable table, String key) {
        if (table == null)
            { return null; }
        if ( (key == null) || (key.isEmpty()) )
            { return null; }
        if (table.contains(key)) {
            return (String) table.get(key);
        }
        for (Enumeration keys = table.keys(); keys.hasMoreElements();) {
            String tmpKey = (String) keys.nextElement();
            if (tmpKey.equalsIgnoreCase(key)) {
                return (String) table.get(tmpKey);
            }
        }
        return null;
    }

    /**
     * Returns local server name from naming table. 
     *
     * @return server name opensso is deployed. 
     */
    public static String getLocalServer() {
        String server = null;
        
        try {
            server = getServerFromID(getAMServerID());
        } catch (ServerEntryNotFoundException e) {
            debug.error("Failed to get local server entry.", e);
        }
        
        return server;
    }

    /**
     * Returns the server ID that is there in the platform server
     * list for a corresponding server. 
     *
     * @param protocol procotol of the server instance
     * @param host host of the server instance
     * @param port port of the server instance
     * @param uri uri of the server instance
     *
     * @return Server ID
     *
     * @throws ServerEntryNotFoundException if the Naming Service
     *     can not find that server entry     
     */
    public static String getServerID(
        String protocol,
        String host,
        String port,
        String uri)
        throws ServerEntryNotFoundException {
        return getServerID(protocol, host, port, uri, true);
    }

    /**
     * Returns the server ID that is there in the platform server
     * list for a corresponding server. 
     *
     * @param protocol procotol of the server instance
     * @param host host of the server instance
     * @param port port of the server instance
     * @param uri uri of the server instance
     * @param updatetbl a boolean flag indicating whether a refresh of the
     *     naming profile is needed.
     *
     * @return Server ID
     *
     * @throws ServerEntryNotFoundException if the Naming Service
     *     can not find that server entry
     */
    public static String getServerID(
        String protocol, 
        String host, 
        String port,
        String uri,
        boolean updatetbl
    ) throws ServerEntryNotFoundException {
        String installTime = SystemProperties.get(
            Constants.SYS_PROPERTY_INSTALL_TIME, "false");
        try {
            // check before the first naming table update to avoid deadlock
            if (protocol == null || host == null || port == null ||
                protocol.length() == 0 || host.length() == 0 ||
                port.length() == 0) {
                if (installTime.equals("false")) {
                    debug.error("WebtopNaming.getServerId():noServerId");
                }
                throw new Exception(NamingBundle.getString("noServerID"));
            }

            String serverWithoutURI = protocol + ":" + "//" + host + ":" + port;
            String serverWithURI = null;
            
            if ((uri != null) && (uri.length() > 0)) {
                StringTokenizer tok = new StringTokenizer(uri, "/");
                uri = "/" + tok.nextToken();
            } else {
                serverWithURI = protocol + ":" + "//" + host + ":" + port + amServerURI;
                debug.message("WebtopNaming.getServerId(): serverWithURI: " + serverWithURI);
            }
            
            String server = (uri != null) ?
                protocol + ":" + "//" + host + ":" + port + uri : 
                serverWithoutURI;

            String serverID = null;
            if (serverIdTable != null) {
                serverID = getValueFromTable(serverIdTable, server);
                
                if (serverID == null) {
                    //try without URI, this is for prior release of OpenSSO
                    //Enterprise 8.0
                    serverID = getValueFromTable(serverIdTable, 
                        serverWithoutURI);
                }

                if (serverID == null) {
                    // try with the URI, Agent 3.0 preferred naming URL
                    // is missing the amServer URI
                    serverID = getValueFromTable(serverIdTable,
                        serverWithURI);
                }
            }
            //update the naming table and as well as server id table
            //if it can not find it
            if (( serverID == null ) && (updatetbl == true)) {
                getNamingProfile(true);
                serverID = getValueFromTable(serverIdTable, server);
                if (serverID == null) {
                    //try without URI, this is for prior release of OpenSSO
                    //Enterprise 8.0
                    serverID = getValueFromTable(serverIdTable, 
                        serverWithoutURI);
                }

                if (serverID == null) {
                    // try with the URI, Agent 3.0 preferred naming URL
                    // is missing the amServer URI
                    serverID = getValueFromTable(serverIdTable,
                        serverWithURI);
                }
            }

            if (serverID == null) {
                if (installTime.equals("false")) {
                    if (!sitemonitorDisabled) {
                        debug.error("WebtopNaming.getServerId():serverId null " +
                            "for server: " + server);
                    } else {
                        debug.message("WebtopNaming.getServerId():serverId null " +
                            "for server: " + server);
                    }
                }
                if (!sitemonitorDisabled) {
                    throw new ServerEntryNotFoundException(
                            NamingBundle.getString("noServerID"));
                }
            }
            return serverID;
        } catch (Exception e) {
            if (installTime.equals("false")) {
                debug.error("WebtopNaming.getServerId()", e);
            }
            throw new ServerEntryNotFoundException(e);
        }
    }

    /**
     * Returns the server URL based on the server ID.
     *
     * @param serverID Server ID
     *
     * @return server URL 
     *
     * @throws ServerEntryNotFoundException if the Naming Service
     *     can not find that server entry
     */

    public static String getServerFromID(String serverID)
            throws ServerEntryNotFoundException {
        String server = null;
        try {
            // refresh local naming table in case the key is not found
            if (namingTable != null) {
                server = getValueFromTable(namingTable, serverID);
            }
            if (server == null) {
                getNamingProfile(true);
                server = getValueFromTable(namingTable, serverID);
            }
            if (server == null) {
                throw new ServerEntryNotFoundException(NamingBundle
                        .getString("noServer" ));
            }

        } catch (Exception e) {
            debug.error("WebtopNaming.getServerFromID() can not find "
                        + "server name for server ID : " + serverID, e);
            throw new ServerEntryNotFoundException(e);
        }
        return server;
    }

    /**
     * Returns all server IDs.
     *
     * @return all server IDs.
     * 
     * @throws Exception if an error occurs when updating the
     *     nameing table
     */
    public static Vector getAllServerIDs() throws Exception  {
        if (namingTable == null) {
            getNamingProfile(false);
        }

        return platformServerIDs;
    }

    private static void updateLBCookieValueMappings() {
        Hashtable lbcookieTbl = new Hashtable();
        String serverSet = (String) namingTable.get(
                           Constants.SERVERID_LBCOOKIEVALUE_LIST);

        if ((serverSet == null) || (serverSet.length() == 0)) {
            return;
        }

        StringTokenizer tok = new StringTokenizer(serverSet, ",");
        while (tok.hasMoreTokens()) {
            String serverid = tok.nextToken();
            String lbCookieValue = serverid;
            int idx = serverid.indexOf(NODE_SEPARATOR);
            if (idx != -1) {
                lbCookieValue = serverid.substring(idx + 1, serverid.length());
                serverid = serverid.substring(0, idx);
            }
            lbcookieTbl.put(serverid, lbCookieValue);
        }

        lbCookieValuesTable = lbcookieTbl;
        
        if (debug.messageEnabled()) {
            debug.message("WebtopNaming.updateLBCookieValueMappings():" +
                "LBCookieValues table -> " + lbCookieValuesTable.toString());
        }

        return;
    }

    /**
     * Returns the lbCookieValue corresponding to the server ID.
     * 
     * @param serverid the server id
     *
     * @return the LB cookie value corresponding to server ID
     */     
    public static String getLBCookieValue(String serverid) {
        String lbCookieValue = null;

        if (serverid == null) {
            if (debug.messageEnabled()) {
                debug.message("WebtopNaming.getLBCookieValue():" +
                    " server id is null, returning null ");
            }
            return null;
        } else if (lbCookieValuesTable == null) {
            if (debug.messageEnabled()) {
                debug.message("WebtopNaming.getLBCookieValue():" +
                    " lbCookieValues table is null, returning server id: " +
                    serverid);
            }
            return serverid;
        }

        lbCookieValue = (String) lbCookieValuesTable.get(serverid);
        if (lbCookieValue == null) {
            if (debug.messageEnabled()) {
                debug.message("WebtopNaming.getLBCookieValue():" +
                    " lbCookieValue from table is null, returning server id: " +
                    serverid);
            }
            return serverid;
        }

        if (debug.messageEnabled()) {
            debug.message("WebtopNaming.getLBCookieValue(): lbCookieValue"
            + "for " + serverid + " is "  + lbCookieValue);
        }

        return lbCookieValue;
    }

    /**
     * Returns the unique identifier of the site which the given 
     * server instance belongs to. 
     *
     * @param protocol procotol of the server instance
     * @param host host of the server instance
     * @param port port of the server instance
     * @param uri uri of the server instance
     * 
     * @return Site ID
     *
     * @throws ServerEntryNotFoundException if the Naming Service
     *     can not find that server entry
     */
    public static String getSiteID(
        String protocol,
        String host,
        String port, 
        String uri
    ) throws ServerEntryNotFoundException {
        String serverid = getServerID(protocol, host, port, uri);
        return getSiteID(serverid);
    }

    /**
     * Returns the unique identifier of the site which the given
     * server instance belongs to.
     *
     * @param serverid server ID
     *
     * @return Site ID
     */
    public static String getSiteID(String serverid) {
        String primary_site = null;
        String sitelist = null;

        if (siteIdTable == null) {
            return null;
        }

        sitelist = (String) siteIdTable.get(serverid);
        StringTokenizer tok = new StringTokenizer(sitelist, NODE_SEPARATOR);
        if (tok != null) {
            primary_site = tok.nextToken();
        }

        if (debug.messageEnabled()) {
            debug.message("WebtopNaming : SiteID for " + serverid + " is "
                    + primary_site);
        }

        return primary_site;
    }
    
    public static String getSiteIdByName(String siteName) {
        String siteId = null;
        
        if (siteNameToIdTable == null) {
            return null;
        }
        
        siteId = siteNameToIdTable.get(siteName);
        
        if (debug.messageEnabled()) {
            debug.message("WebtopNaming : Site ID for " + siteName + " is "
                    + siteId);
        }
        
        return siteId;
    }
    
    public static String getSiteNameById(String siteId) {
        String siteName = null;
        
        if (siteNameToIdTable == null) {
            return null;
        }
        
        for (Map.Entry<String, String> siteNameEntry : siteNameToIdTable.entrySet()) {
            if (siteNameEntry.getValue().equals(siteId)) {
                siteName = siteNameEntry.getKey();
                break;
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message("WebtopNaming : Site Name for " + siteName + " is "
                    + siteId);
        }
        
        return siteName;
    }

    /**
     * Returns the String representation of the separator delimited
     * secondary site list.
     *
     * @param protocol procotol of the server instance
     * @param host host of the server instance
     * @param port port of the server instance
     * @param uri uri of the server instance
     *
     * @return the secondary site list
     * @throws ServerEntryNotFoundException if the Naming Service
     *     can not find that server entry
     */
    public static String getSecondarySites(
        String protocol,
        String host,
        String port,
        String uri)
    throws ServerEntryNotFoundException {
        String serverid = getServerID(protocol, host, port, uri);
        return getSecondarySites(serverid);
    }

    /**
     * Returns the String representation of the separator delimited 
     * secondary site list.
     *
     * @param serverid server ID
     * 
     * @return the secondary site list
     */
    public static String getSecondarySites(String serverid) {
        String sitelist = null;
        String secondarysites = null;

        if (siteIdTable == null) {
            return null;
        }

        sitelist = siteIdTable.get(serverid);
        if (sitelist == null) {
            return null;
        }

        int index = sitelist.indexOf(NODE_SEPARATOR);
        if (index != -1) {
            secondarysites = sitelist.substring(index + 1, sitelist.length());
        }

        if (debug.messageEnabled()) {
            debug.message("WebtopNaming : SecondarySites for " + serverid
                    + " is " + secondarysites);
        }

        return secondarysites;
    }

    /**
     * Returns all the node ID for the site.
     * 
     * @param serverid one of server IDs within the site, it can also
     *     be the loab balancer's ID 
     *
     * @return HashSet has all the node is for the site.
     *
     * @throws Exception if an error occurs when updating the
     *     nameing table     
     */
    public static Set<String> getSiteNodes(String serverid) throws Exception {
        HashSet<String> nodeset = new HashSet();

        if (namingTable == null) {
            getNamingProfile(false);
        }

        String siteid = getSiteID(serverid);

        Enumeration e = siteIdTable.keys();
        while (e.hasMoreElements()) {
            String node = (String) e.nextElement();
            if (siteid.equalsIgnoreCase(node)) {
                continue;
            }

            if (siteid.equalsIgnoreCase(getSiteID(node))) {
                nodeset.add(node);
            }
        }

        return nodeset;
    }

    /**
     * Returns the class of the specified service.
     * 
     * @param service the name of the service.
     *
     * @return The class name of the specified service.
     *
     * @throws ClassNotFoundException if no definition for the class 
     *    with the specified name could be found.
     */
    public static String getServiceClass(String service)
            throws ClassNotFoundException {
        try {
            if (namingTable == null) {
                getNamingProfile(false);
            }
            String cls = null;
            String name = AM_NAMING_PREFIX + service.toLowerCase()
                    + "-class";
            cls = (String) namingTable.get(name);
            if (cls == null) {
                name = FAM_NAMING_PREFIX + service.toLowerCase() + "-class";
                cls = (String)namingTable.get(name);
            }
            if (cls == null) {
                throw new Exception(NamingBundle.getString("noServiceClass")
                        + service);
            }
            return cls;
        } catch (Exception e) {
            throw new ClassNotFoundException(e.getMessage());
        }
    }

    /**
     * Returns the URL of the notification service on the local
     * host.
     *
     * @return the notification URL
     *
     * @throws URLNotFoundException if the Naming Service can not
     *     find a URL for a specified service
     */
    public synchronized static URL getNotificationURL()
            throws URLNotFoundException {
        try {
            String url = System.getProperty(Constants.CLIENT_NOTIFICATION_URL,
                    SystemProperties.get(Constants.CLIENT_NOTIFICATION_URL));
            if (url == null) {
                throw new URLNotFoundException(NamingBundle
                        .getString("noNotificationURL"));
            }
            return new URL(url);
        } catch (Exception e) {
            throw new URLNotFoundException(e.getMessage());
        }
    }

    private synchronized static void getNamingProfile(boolean update)
            throws Exception {
        if (update || namingTable == null) {
            updateNamingTable();
        }
    }

    private static void updateServerProperties(URL url) {
        amServerProtocol = url.getProtocol();
        amServer = url.getHost();
        amServerPort = Integer.toString(url.getPort());
        amServerURI = url.getPath();
        amServerURI = amServerURI.replaceAll("//", "/");
        int idx = amServerURI.lastIndexOf("/");
        while (idx > 0) {
            amServerURI = amServerURI.substring(0, idx);
            idx = amServerURI.lastIndexOf("/");
        }

        SystemProperties.initializeProperties(Constants.AM_SERVER_PROTOCOL,
            amServerProtocol);
        SystemProperties.initializeProperties(Constants.AM_SERVER_HOST,
            amServer);
        SystemProperties.initializeProperties(Constants.AM_SERVER_PORT,
            amServerPort);
        SystemProperties.initializeProperties(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, amServerURI);
        if (debug.messageEnabled()) {
            debug.message("Server Properties are changed : ");
            debug.message(Constants.AM_SERVER_PROTOCOL + " : "
                + SystemProperties.get(Constants.AM_SERVER_PROTOCOL, null));
            debug.message(Constants.AM_SERVER_HOST + " : "
                + SystemProperties.get(Constants.AM_SERVER_HOST, null));
            debug.message(Constants.AM_SERVER_PORT + " : "
                + SystemProperties.get(Constants.AM_SERVER_PORT, null));
            debug.message(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR + " : "
                + SystemProperties.get(
                    Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, null));
        }
    }

    private static Hashtable getNamingTable(URL nameurl) throws Exception {
        Hashtable nametbl = null;
        NamingRequest nrequest = new NamingRequest(NamingRequest.reqVersion);
        Request request = new Request(nrequest.toXMLString());
        RequestSet set = new RequestSet(NAMING_SERVICE);
        set.addRequest(request);
        Vector responses = null;

        try {
            responses = PLLClient.send(nameurl, set);
            if (responses.size() != 1) {
                throw new Exception(NamingBundle
                        .getString("unexpectedResponse"));
            }

            Response res = (Response) responses.elementAt(0);
            NamingResponse nres = NamingResponse.parseXML(res.getContent());
            if (nres.getException() != null) {
                throw new Exception(nres.getException());
            }
            nametbl = nres.getNamingTable();
        } catch (SendRequestException sre) {
            debug.error("Naming service connection failed for " + nameurl, sre);
        } catch (Exception e) {
            debug.error("getNamingTable: ", e);
        }

        return nametbl;
    }

    private static void updateNamingTable() throws Exception {

        if (!serverMode) {
            if (namingServiceURL == null) {
                initializeNamingService();
            }

            // Try for the primary server first, if it fails and then
            // for the second server. We get connection refused error
            // if it doesn't succeed.
            Hashtable namingtbl = null;
            URL tempNamingURL = null;
            for (int i = 0; ((namingtbl == null) && 
                    (i < namingServiceURL.length)); i++) {
                tempNamingURL = new URL(namingServiceURL[i]);
                namingtbl = getNamingTable(tempNamingURL);
            }

            if (namingtbl == null) {
                debug.error("updateNamingTable : "
                        + NamingBundle.getString("noNamingServiceAvailable"));
                throw new Exception(NamingBundle
                        .getString("noNamingServiceAvailable"));
            } else {
                namingTable = namingtbl;
            }

            updateServerProperties(tempNamingURL);
        } else {
            namingTable = NamingService.getNamingTable();
        }

        String servers = (String) namingTable.get(Constants.PLATFORM_LIST);

        if (servers != null) {
            StringTokenizer st = new StringTokenizer(servers, ",");
            Vector platformServersNEW = new Vector(); 
            Vector lcPlatformServersNEW = new Vector(); 
            while (st.hasMoreTokens()) {
                String svr = st.nextToken();
                lcPlatformServersNEW.add(svr.toLowerCase());
                platformServersNEW.add(svr);
            }
            platformServers = platformServersNEW;
            lcPlatformServers = lcPlatformServersNEW;
        }
        updateServerIdMappings();
        updateSiteIdMappings();
        updateSiteNameToIDMappings();
        updatePlatformServerIDs();
        updateLBCookieValueMappings();
        updatePlatformSets();
                
        if (debug.messageEnabled()) {
            debug.message("Naming table -> " + namingTable.toString());
            debug.message("Server Id Table -> " + serverIdTable.toString());
            debug.message("Site Id Table -> " + siteIdTable.toString());
            debug.message("Site Name to Id Table -> " + siteNameToIdTable.toString());
            debug.message("Platform Servers -> " + platformServers.toString());
            debug.message("Platform Server IDs -> "
                          + platformServerIDs.toString());
        }
    }

    /*
     * This method is to update the servers and their IDs in a seprate 
     * hash. It will get updated each time when the naming table gets
     * updated. Note: this table will have all the entries in the
     * naming table but in a reverse order except the platform server
     * list. We can just keep only server ID mappings but we need to 
     * exclude each other entry which is there in.
     */
    private static void updateServerIdMappings() {
        Hashtable serverIdTbl = new Hashtable();
        Enumeration e = namingTable.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = (String) namingTable.get(key);
            if ((key == null) || (value == null)) {
                continue;
            }
            // If the key is server list skip it, since it would
            // have the same value
            if (key.equals(Constants.PLATFORM_LIST)) {
                continue;
            }
            serverIdTbl.put(value, key);
        }
        
        serverIdTable = serverIdTbl;
    }

    private static void updateSiteIdMappings() {
        Hashtable<String, String> siteIdTbl = new Hashtable<String, String>();
        String serverSet = (String) namingTable.get(Constants.SITE_ID_LIST);

        if ((serverSet == null) || (serverSet.length() == 0)) {
            return;
        }

        StringTokenizer tok = new StringTokenizer(serverSet, ",");
        while (tok.hasMoreTokens()) {
            String serverid = tok.nextToken();
            String siteid = serverid;
            int idx = serverid.indexOf(NODE_SEPARATOR);
            if (idx != -1) {
                siteid = serverid.substring(idx + 1, serverid.length());
                serverid = serverid.substring(0, idx);
            }
            siteIdTbl.put(serverid, siteid);
        }

        siteIdTable = siteIdTbl;
        if (debug.messageEnabled()) {
            debug.message("SiteID table -> " + siteIdTable.toString());
        }

        return;
    }

    private static void updatePlatformServerIDs()
        throws MalformedURLException, ServerEntryNotFoundException {
        Iterator it = platformServers.iterator();
        while (it.hasNext()) {
            String plaformURL = (String) it.next();
            URL url = new URL(plaformURL);
            String serverID = getServerID(url.getProtocol(), url.getHost(),
                Integer.toString(url.getPort()), url.getPath());
            if (!platformServerIDs.contains(serverID)) {
                platformServerIDs.add(serverID);
            }
        }
    }
    
    private static void updateSiteNameToIDMappings() {
        Map siteNameToIdTbl = new HashMap();
        String siteNameToIDs = (String) namingTable.get(Constants.SITE_NAMES_LIST);

        if ((siteNameToIDs == null) || (siteNameToIDs.length() == 0)) {
	     siteNameToIdTable = siteNameToIdTbl;
            return;
        }

        StringTokenizer tok = new StringTokenizer(siteNameToIDs, ",");
        while (tok.hasMoreTokens()) {
            String siteNameAndID = tok.nextToken();
            String siteName = siteNameAndID;
            String siteId = siteNameAndID;
            
            int idx = siteNameAndID.indexOf(NODE_SEPARATOR);
            if (idx != -1) {
                siteId = siteNameAndID.substring(idx + 1, siteNameAndID.length());
                siteName = siteNameAndID.substring(0, idx);
            }
            siteNameToIdTbl.put(siteName, siteId);
        }

        siteNameToIdTable = siteNameToIdTbl;
        if (debug.messageEnabled()) {
            debug.message("SiteNameToIDs table -> " + siteNameToIdTable.toString());
        }

        return;
    }

    private static void updatePlatformSets() {
        Set<String> siteIDSet = new HashSet<String>(siteNameToIdTable.values());
        Set<String> secondarySiteIDSet = new HashSet<String>();
        Set<String> serverIDSet = new HashSet<String>(platformServerIDs);

        for (Map.Entry<String, String> entry : siteIdTable.entrySet()) {
            String siteId = entry.getValue();
            if (siteId.indexOf(NODE_SEPARATOR) != -1) {
                StringTokenizer tokenizer = new StringTokenizer(siteId, NODE_SEPARATOR);
                //the first one is always the primary site ID, which we don't need now
                tokenizer.nextToken();
                while (tokenizer.hasMoreTokens()) {
                    secondarySiteIDSet.add(tokenizer.nextToken());
                }
            }
        }
        serverIDSet.removeAll(siteIDSet);
        serverIDSet.removeAll(secondarySiteIDSet);

        siteIDs = siteIDSet;
        secondarySiteIDs = secondarySiteIDSet;
        serverIDs = serverIDSet;
    }

    private static void validate(
        String protocol, 
        String host, 
        String port,
        String uri
    ) throws URLNotFoundException {
        String server = (uri != null) ?
            protocol + "://" + host + ":" + port + uri :
            protocol + "://" + host + ":" + port;
        server = server.toLowerCase();
        
        try {
            // first check if this is the local server, proto, and port,
            // if it is there is no need to
            // validate that it is in the trusted server platform server list
            if (protocol.equalsIgnoreCase(amServerProtocol) &&
                host.equalsIgnoreCase(amServer) && 
                port.equals(amServerPort) &&
                ((uri == null) || uri.equalsIgnoreCase(amServerURI))
            ) {
                return;
            }
            if (debug.messageEnabled()) {
                debug.message("WebtopNaming.validate: platformServers= " + 
                    platformServers);
            }

            if (!lcPlatformServers.contains(server)) {
                getNamingProfile(true);
                if (!platformServers.contains(server)) {
                    throw new URLNotFoundException(NamingBundle
                            .getString("invalidServiceHost")
                            + " " + server);
                }
            }
        } catch (Exception e) {
            debug.error("platformServers: " + platformServers, e);
            throw new URLNotFoundException(e.getMessage());
        }
    }

    /**
     * Returns a list of the naming service urls.
     * 
     * @return a String array of naming service urls.
     *
     * @throws Exception if there is no configured url or there is an
     *     error when trying to get the urls
     */
    public synchronized static String[] getNamingServiceURL() throws Exception {
        if (!serverMode && (namingServiceURL == null)) {
            // Initilaize the list of naming URLs
            ArrayList urlList = new ArrayList();

            // Get the naming service URLs from properties files
            String configURLListString =
                               SystemProperties.get(Constants.AM_NAMING_URL);
            if (configURLListString != null) {
                StringTokenizer stok = new StringTokenizer(configURLListString);
                while (stok.hasMoreTokens()) {
                    String nextURL = stok.nextToken();
                    if (urlList.contains(nextURL)) {
                        if (debug.warningEnabled()) {
                            debug.warning(
                                "Duplicate naming service URL specified "
                                + nextURL + ", will be ignored.");
                        }
                    } else {
                        urlList.add(nextURL);
                    }
                }
            }

            if (urlList.isEmpty()) {
                throw new Exception(
                    NamingBundle.getString("noNamingServiceURL"));
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Naming service URL list: " + urlList);
                }
            }

            namingServiceURL = new String[urlList.size()];
            System.arraycopy(urlList.toArray(), 0, namingServiceURL, 0,
                urlList.size());
        }

        return namingServiceURL;
    }

    private static synchronized void startSiteMonitor(String[] urlList) {
        // Site monitor is already started.
        if (monitorThread != null) {
            return;
        }

        // Start naming service monitor
        monitorThread = new SiteMonitor(urlList);
        SystemTimer.getTimer().schedule(monitorThread, new Date(
            System.currentTimeMillis() / 1000 * 1000));
    }

    /**
     * Removes a server from the available site list. 
     *
     * @param server the <code>String</code> to parse as a URL of 
     *     the server to be removed from the site list         
     */
    static public void removeFailedSite(String server) {
        if (monitorThread != null) {
            try {
                URL url = new URL(server);
                removeFailedSite(url);
            } catch (MalformedURLException e) {
                debug.error("Server URL is not valid : ", e);
            }
        }

        return;
    }


    /**
     * Removes a server from the available site list.
     *
     * @param url url of the server to be removed from the site list 
     */
    static public void removeFailedSite(URL url) {
        if (monitorThread != null) {
            try {
                String serverid = getServerID(url.getProtocol(),
                    url.getHost(), String.valueOf(url.getPort()), 
                    url.getPath());
                SiteMonitor.removeFailedSite(serverid);
            } catch (ServerEntryNotFoundException e) {
                debug.error("Can not find server ID : ", e);
            }
        }

        return;
    }

    /**
     * Returns the uri of the specified URL.
     * 
     * @param url the URL that includes uri.
     *
     * @return a uri of the specified <code>URL</code>.
     */
    public static String getURI(URL url) {
        String uri = url.getPath();
        int idx = uri.lastIndexOf('/');
        while (idx > 0) {
            uri = uri.substring(0, idx);
            idx = uri.lastIndexOf('/');
        }
        
        return uri;
    }

    /**
     * Provides the Monitoring Agent site and server related information.
     * 
     * @return 0 (zero) if all information collected and provided successfully;
     *         -1 if server protocol, hostname, port or URI is null;
     *         -2 if not serverMode, or Monitoring Agent already running
     *         -3 if unable to get the ServerID
     *
     */
    public static int configMonitoring() {
        String classMethod = "WebtopNaming.configMonitoring: ";
        /*
         * start the monitoring agent, if not already started
         */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (isServerMode() && !MonitoringUtil.isRunning() && !sitemonitorDisabled) {
            Date date1 = new Date();
            String startDate = sdf.format(date1);

            if (debug.warningEnabled()) {
                Date date = new Date();
                debug.warning(classMethod +
                    "start monitoring config" + "\n" +
                    "    Start time = " + sdf.format(date));
            }

            if ((amServerProtocol != null) &&
                (amServer != null) &&
                (amServerPort != null) &&
                (amServerURI != null))
            {
                String xxx = null;
                try {
                    xxx = getServerID(amServerProtocol, amServer,
                        amServerPort, amServerURI);
                } catch (ServerEntryNotFoundException sefx) {
                    debug.error(classMethod + "can't getServerID");
                    return -3;
                }

                String siteID = getSiteID(xxx);
                String baseDir =
                    SystemProperties.get(SystemProperties.CONFIG_PATH);
                boolean isEmbeddedDS =
                    (new File(baseDir + "/opends")).exists();

                /*
                 *  session failover status and site configuration
                 *  (SiteConfiguration.getSites()) (to get the site names)
                 *  require an SSOToken, which we can't get at this point.
                 */
                SSOServerInfo srvrInfo =
                    new SSOServerInfo.SSOServerInfoBuilder(xxx, siteID).
                        svrProtocol(amServerProtocol).
                        svrName(amServer).
                        svrURI(amServerURI).
                        svrPort(amServerPort).
                        embeddedDS(isEmbeddedDS).
                        siteIdTable(siteIdTable).
                        svrIdTable(serverIdTable).
                        startDate(startDate).
                        namingTable(namingTable).build();
    
                Agent.siteAndServerInfo(srvrInfo);

                if (debug.messageEnabled()) {
                    Date date = new Date();
                    debug.message(classMethod +
                        "monitoring agent config returned\n" +
                        "    End time = " + sdf.format(date));
                }

                return 0;
            } else {
                debug.error(classMethod + "null proto/server/port/uri");
                return -1;
            }
        }
        return -2;
    }
    
    /**
     * The <code>SiteMonitor</code> class is used to monitor the
     * health status of all the sites.
     */
static public class SiteMonitor extends GeneralTaskRunnable {
    static long sleepInterval;
    static Vector availableSiteList = new Vector();
    static String currentSiteID = null;
    static SiteStatusCheck siteChecker = null;
    static String[] siteUrlList = null;

    /**
     * A boolean flag indicating whether site monitoring is enabled.
     */    
    static public boolean keepMonitoring = false;

    static {
        try {
            String checkClass =
                SystemProperties.get(Constants.SITE_STATUS_CHECK_CLASS,
                    "com.iplanet.services.naming.SiteStatusCheckThreadImpl");
            if (debug.messageEnabled()) {
                debug.message("SiteMonitor : SiteStatusCheck class = "
                        + checkClass);
            }
            siteChecker =
                (SiteStatusCheck) Class.forName(checkClass).newInstance();
            sleepInterval = Long.valueOf(SystemProperties.
                    get(Constants.MONITORING_INTERVAL, "60000")).longValue();
            getNamingProfile(false);
            currentSiteID = getServerID(amServerProtocol, amServer, 
                amServerPort, amServerURI);
        } catch (Exception e) {
            debug.message("SiteMonitor initialization failed : ", e);
        }
    }

    /**
     * Constructs a WebtopNaming$SiteMonitor object with the provided
     * site urls.
     *
     * @param urlList  a String containing the urls of the sites
     */
    public SiteMonitor(String[] urlList) {
        siteUrlList = urlList;
    }

    public boolean addElement(Object obj) {
        return false;
    }
    
    public boolean removeElement(Object obj) {
        return false;
    }
    
    public boolean isEmpty() {
        return true;
    }
    
    public long getRunPeriod() {
        return sleepInterval;
    }
    
    public void run() {
        keepMonitoring = true;
        try {
            runCheckValidSite();
        } catch (Exception e) {
            debug.error("SiteMonitor run failed : ", e);
        }
    }

    static void runCheckValidSite() {
        Vector siteList = checkAvailableSiteList();
        updateSiteList(siteList);
        updateCurrentSite(siteList);
    }
    
    /**
     * Checks if the site is up.
     *
     * @param siteurl a site url
     *
     * @return <code>true</code> if the site is up
     */
    public static boolean checkSiteStatus(URL siteurl) {
        return siteChecker.doCheckSiteStatus(siteurl);
    }

    private static Vector checkAvailableSiteList() {
        Vector siteList = new Vector();
        for (int i = 0; i < siteUrlList.length; i++) {
            try {
                URL siteurl = new URL(siteUrlList[i]);
                if (siteChecker.doCheckSiteStatus(siteurl) == false) {
                    continue;
                }

                String serverid = getServerID(
                    siteurl.getProtocol(), siteurl.getHost(),
                    String.valueOf(siteurl.getPort()), siteurl.getPath());
                siteList.add(serverid);
            } catch (MalformedURLException ex) {
                if (debug.messageEnabled()) {
                    debug.message("SiteMonitor: Site URL "
                         + siteUrlList[i] + " is not valid.", ex);
                }
            } catch (ServerEntryNotFoundException ex) {
                if (debug.messageEnabled()) {
                    debug.message("SiteMonitor: Site URL "
                         + siteUrlList[i] + " is not available.", ex);
                }
            }
        }

        return siteList;
    }

    /**
     * Checks if the site of the url is up.
     *
     * @param url a site url
     *
     * @return <code>true</code> if the site is up
     *
     * @throws Exception if failing to get the naming service url.
     */
    public static boolean isAvailable(URL url) throws Exception {
        if ((namingTable == null) || (keepMonitoring == false)) {
            return true;
        }

        String serverID = null;
        try {
            serverID = getServerID(url.getProtocol(), url.getHost(),
                Integer.toString(url.getPort()), url.getPath(), false);
        } catch (ServerEntryNotFoundException e) {
            if (debug.messageEnabled()) {
                debug.message("URL is not part of AM setup.");
            }
            return true;
        }

        Vector sites = getAvailableSites();
        boolean available = false;
        Iterator it = sites.iterator();
        while (it.hasNext()) {
            String server = (String)it.next();
            if (serverID.equalsIgnoreCase(server)) {
                available = true;
                break;
            }
        }

        if (debug.messageEnabled()) {
            debug.message("In SiteMonitor.isAvailable()");
            if (available) {
                debug.message("SiteID " + url.toString() + " is UP.");
            } else {
                debug.message("SiteID " + url.toString() + " is DOWN.");
            }
        }

        return available;
    }
    
    /**
     * Checks if the url is one of configured sites.
     *
     * @param url a site url
     *
     * @return <code>true</code> if the url is one of configured sites. 
     *
     * @throws Exception if failing to get the naming service url.    
     */
    public static boolean isCurrentSite(URL url) throws Exception {
        if ((namingTable == null) || !keepMonitoring) {
            return true;
        }

        String serverID = null;
        try {
            serverID = getServerID(url.getProtocol(), url.getHost(),
                Integer.toString(url.getPort()), url.getPath(), false);
        } catch (ServerEntryNotFoundException e) {
            if (debug.messageEnabled()) {
                debug.message("URL is not part of AM setup.");
            }
            return true;
        }

        Vector sites = getAvailableSites();
        boolean isCurrent = false;
        if (!sites.isEmpty()) {
            String serverid = (String)sites.firstElement();
            if (serverid != null) {
                isCurrent = serverid.equalsIgnoreCase(serverID);
            }
        }

        return isCurrent;
    }

    static Vector getAvailableSites() throws Exception {
        Vector sites = null;
        if (availableSiteList.isEmpty()) {
            String[] namingURLs = getNamingServiceURL();
            for (int i = 0; i < namingURLs.length; i++) {
                URL url = new URL(namingURLs[i]);
                availableSiteList.add(getServerID(
                    url.getProtocol(), url.getHost(), 
                    String.valueOf(url.getPort()), url.getPath()));
            }

            updateCurrentSite(availableSiteList);
        }

        sites = new Vector(availableSiteList);
        if (debug.messageEnabled()) {
            debug.message("In SiteMonitor.getAvailableSites()");
            debug.message("availableSiteList : " + sites.toString());
        }

        return sites;
    }

    static void removeFailedSite(String site) {
        if ((keepMonitoring == true) && (availableSiteList.contains(site))) {
            availableSiteList.remove(site);
        }

        return;
    }

    private static void updateSiteList(Vector list) {
        availableSiteList = list;

        if (debug.messageEnabled()) {
            debug.message("In SiteMonitor.updateSiteList()");
            debug.message("availableSiteList : "
                    + availableSiteList.toString());
        }
        return;
    }

    private static void updateCurrentSite(Vector list) {
        if (serverMode) {
            return;
        }

        if ((list == null) || (list.isEmpty())) {
            return;
        }

        String sid = (String)list.firstElement();
        if (!currentSiteID.equalsIgnoreCase(sid)) {
            if (debug.messageEnabled()) {
                debug.message("Invoke updateServerProperties() : " +
                        "Server properties are changed for service failover");
            }

            try {
                currentSiteID = sid;
                String serverurl = getServerFromID(currentSiteID);
                updateServerProperties(new URL(serverurl));
            } catch (Exception e) {
                debug.error("SiteMonitor: ", e);
            }
        }

        return;
    }
}

/**
 * The interface <code>SiteStatusCheck</code> provides
 * method that will be used by SiteMonitor to check each site is alive.
 * Each implementation class has to implement doCheckSiteStatus method.
 */
public interface SiteStatusCheck {
    /**
     * Check if the site is alive.
     * @param siteurl the url which needs to be checked alive
     *
     * @return <code>true</code> if the site is up.
     */
    public boolean doCheckSiteStatus(URL siteurl);
}
}
