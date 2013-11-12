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
 * $Id: NamingService.java,v 1.13 2009/04/07 22:30:07 beomsuk Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.iplanet.services.naming.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.comm.server.RequestHandler;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.services.naming.share.NamingRequest;
import com.iplanet.services.naming.share.NamingResponse;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.common.FQDNUtils;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NamingService implements RequestHandler, ServiceListener {

    public static final int SERVICE_REV_NUMBER_70 = 20;

    public static int serviceRevNumber;

    private static Debug namingDebug = null;

    public static final String NAMING_SERVICE = "com.iplanet.am.naming";

    private static volatile Hashtable namingTable = null;

    private static Properties platformProperties = null;

    private static String server_proto = null;

    private static String server_host = null;

    private static String server_port = null;

    private static SSOToken sso = null;

    private static ServiceSchemaManager ssmNaming = null;

    private static ServiceSchemaManager ssmPlatform = null;

    private static ServiceConfigManager scmNaming = null;

    private static ServiceConfigManager scmPlatform = null;

    private static ServiceConfig sessionServiceConfig = null;

    private static Set sessionConfig = null;

    private static String delimiter = "|";

    /*
     * Initialize SSO, schema managers statically, and add listener for schema
     * change events for platform service so that the naming table gets updated
     * if a new platform server is added or gets deleted
     */
    static {
        initialize();
    }

    public static void initialize() {
        namingDebug = Debug.getInstance("amNaming");
        platformProperties = SystemProperties.getAll();
        server_proto = platformProperties.getProperty(
            "com.iplanet.am.server.protocol", "");
        server_host = platformProperties.getProperty(
            "com.iplanet.am.server.host", "");
        server_port = platformProperties.getProperty(
            Constants.AM_SERVER_PORT, "");

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            String adminDN = (String) AccessController
                    .doPrivileged(new AdminDNAction());
            String adminPassword = (String) AccessController
                    .doPrivileged(new AdminPasswordAction());
            sso = mgr.createSSOToken(new AuthPrincipal(adminDN), adminPassword);
            ssmNaming = new ServiceSchemaManager("iPlanetAMNamingService", sso);
            ssmPlatform = new ServiceSchemaManager("iPlanetAMPlatformService",
                    sso);
            scmNaming = new ServiceConfigManager("iPlanetAMNamingService", sso);
            scmPlatform = new ServiceConfigManager("iPlanetAMPlatformService",
                    sso);
            serviceRevNumber = ssmPlatform.getRevisionNumber();
            if (serviceRevNumber < SERVICE_REV_NUMBER_70) {
                ServiceConfigManager scm = new ServiceConfigManager(
                        "iPlanetAMSessionService", sso);
                sessionServiceConfig = scm.getGlobalConfig(null);
                sessionConfig = sessionServiceConfig.getSubConfigNames();
            }

            // Add Listener to the platform and naming service 
            // for schema changes 
            ssmNaming.addListener(new NamingService());
            ssmPlatform.addListener(new NamingService());
            // Add Listener to the platform and naming service
            // for config changes
            scmNaming.addListener(new NamingService());
            scmPlatform.addListener(new NamingService());
        } catch (Exception ne) {
            namingDebug.error("Naming Initialization failed.", ne);
        }
    }

    public NamingService() {

    }

    /**
     * This function returns the naming table that consists of service urls,
     * platform servers and key/value mappings for platform servers Each server
     * instance needs to be updated in the platform server list to reflect that
     * server in the naming table
     */
    public static Hashtable getNamingTable(boolean forClient)
            throws SMSException {
        return updateNamingTable(forClient);
    }

    public static Hashtable getNamingTable() throws SMSException {
        try {
            if (namingTable != null) {
                return namingTable;
            }
            updateNamingTable();
        } catch (Exception ex) {
            throw new SMSException(ex.getMessage());
        }
        return namingTable;
    }

    /**
     * This method updates the naming table especially whenever a new server
     * added/deleted into platform server list.
     * Note that WebtopNaming maintains a reference to the namingTable Hashtable in this class, and thus state in this hash
     * must be updated in-place, or else the reference in WebtopNaming will point to Hashtables with stale state. Note also
     * that this update should preclude concurrent references to this Hashtable - thus the update will synchronize on the
     * Hashtable reference itself, as this will exclude concurrent get operations on the Hashtable while state in the Hashtable
     * is being updated.
     */
    private static void updateNamingTable() throws SMSException {
        Hashtable updatedNamingTable = updateNamingTable(false);
        if (namingTable != null) {
            synchronized (namingTable) {
                namingTable.clear();
                namingTable.putAll(updatedNamingTable);
            }
        } else {
            namingTable = updatedNamingTable;
        }
    }

    /**
     * This method updates the naming table especially whenever a new server
     * added/deleted into platform server list
     */
    private static Hashtable updateNamingTable(boolean forClient)
            throws SMSException {
        Hashtable nametable = null;

        try {
            ServiceSchema sc = ssmNaming.getGlobalSchema();
            Map namingAttrs = sc.getAttributeDefaults();
            sc = ssmPlatform.getGlobalSchema();
            Map platformAttrs = sc.getAttributeDefaults();
            Set sites = getSites(platformAttrs);
            Set servers = getServers(platformAttrs, sites);
            Set siteNamesAndIDs = getSiteNamesAndIDs();
            storeSiteNames(siteNamesAndIDs, namingAttrs);
            
            if ((sites != null) && !sites.isEmpty()) {
                if (!forClient) {
                    registFQDNMapping(sites);
                }
                sites.addAll(servers);
            } else {
                sites = servers;
            }

            if (forClient) {
                storeServerListForClient(sites, namingAttrs);
            } else {
                storeServerList(sites, namingAttrs);
            }

            // To reduce risk convert from a Map to a Hastable since the rest
            // of the naming code expects it in this format. Note there is
            // tradeoff based on whether or not short circuiting is being used.

            nametable = convertToHash(namingAttrs);
            if (forClient && (namingTable != null)) {
                String siteList = (String) namingTable
                    .get(Constants.SITE_ID_LIST);
                nametable.put(Constants.SITE_ID_LIST, siteList);
            }

            insertLBCookieValues(nametable);
        } catch (Exception ex) {
            namingDebug.error("Can't get naming table", ex);
            throw new SMSException(ex.getMessage());
        }

        return nametable;
    }

    /**
     * This will convert updated naming attributes map into naming hashtable
     */
    static Hashtable convertToHash(Map m) {
        Hashtable retHash = new Hashtable();
        Set s = m.keySet();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Set val = (Set) m.get(key);
            retHash.put(key, setToString(val));
        }
        return retHash;
    }

    /**
     * This function stores the server list by parsing platform server list that
     * are stored in <code>iPlanetAMPlatformService</code>. This would expect
     * the servers from the platform service are in the following format
     * protocol://server.domain:port|serverId e.g.
     * http://shivalik.red.iplanet.com:58080|01
     * http://solpuppy.red.iplanet.com:58081|02 The serverId can be anything and
     * does not need to be a number If the platform server is not in the correct
     * format, that entry will be ignored. Note: This server id should be unique
     * if it's participating in load balancing mode.
     */

    static void storeServerList(Set servers, Map namingAttrs) {
        Set serverList = new HashSet();
        Set siteList = new HashSet();
        Iterator iter = servers.iterator();
        while (iter.hasNext()) {
            String serverEntry = (String) iter.next();
            int index = serverEntry.indexOf(delimiter);
            if (index != -1) {
                String server = serverEntry.substring(0, index);
                String serverId = serverEntry.substring(index + 1, serverEntry
                        .length());

                siteList.add(serverId);
                index = serverId.indexOf(delimiter);
                if (index != -1) {
                    serverId = serverId.substring(0, 2);
                }

                HashSet serverSet = new HashSet();
                serverSet.add(server);
                serverList.add(server);
                namingAttrs.put(serverId, serverSet);
            } else {
                namingDebug.error("Platform Server List entry is invalid:"
                        + serverEntry);
            }
        }
        namingAttrs.put(Constants.PLATFORM_LIST, serverList);
        namingAttrs.put(Constants.SITE_ID_LIST, siteList);
    }

    static void storeServerListForClient(Set servers, Map namingAttrs) {
        Set serverList = new HashSet();
        Iterator iter = servers.iterator();
        while (iter.hasNext()) {
            String serverEntry = (String) iter.next();
            int index = serverEntry.indexOf(delimiter);
            if (index != -1) {
                String server = serverEntry.substring(0, index);
                String serverId = serverEntry.substring(index + 1, serverEntry
                        .length());
                index = serverId.indexOf(delimiter);
                if (index != -1) {
                    continue;
                }
                HashSet serverSet = new HashSet();
                serverSet.add(server);
                serverList.add(server);
                namingAttrs.put(serverId, serverSet);
            } else {
                namingDebug.error("Platform Server List entry is invalid:"
                        + serverEntry);
            }
        }
        namingAttrs.put(Constants.PLATFORM_LIST, serverList);
    }
    
    static void storeSiteNames(Set siteNames, Map namingAttrs) {
        namingAttrs.put(Constants.SITE_NAMES_LIST, siteNames);
    }

    static String setToString(Set s) {
        StringBuilder sb = new StringBuilder(100);
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            sb.append((String) iter.next());
            if (iter.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public ResponseSet process(List<Request> requests,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, ServletContext servletContext)
    {
        ResponseSet rset = new ResponseSet(NAMING_SERVICE);
        for (Request req : requests) {
            Response res = processRequest(req);
            rset.addResponse(res);
        }
        return rset;
    }

    private Response processRequest(Request req) {
        String content = req.getContent();
        NamingRequest nreq = NamingRequest.parseXML(content);
        NamingResponse nres = new NamingResponse(nreq.getRequestID());

        // get the version from nreq and check old
        float reqVersion = Float.valueOf(nreq.getRequestVersion()).floatValue();
        boolean limitNametable = (reqVersion > 1.0);

        // get the sesisonId from nreq
        String sessionId = nreq.getSessionId();
        try {
            if (sessionId == null) {
                nres.setNamingTable(NamingService
                        .getNamingTable(limitNametable));
            } else {
                Hashtable tempHash = new Hashtable();
                tempHash = transferTable(NamingService
                        .getNamingTable(limitNametable));
                Hashtable replacedTable = null;
                URL url = usePreferredNamingURL(nreq, reqVersion);
                if (url != null) {
                    String uri = (reqVersion < 3.0) ?
                        SystemProperties.get(
                            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)
                        : WebtopNaming.getURI(url);
                    
                    if (uri.equals(Constants.EMPTY)) {
                        uri = SystemProperties.get(
                            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

                        if (namingDebug.messageEnabled()) {
                            namingDebug.message("uri is blank; adding " + uri);
                        }
                    }

                    replacedTable = replaceTable(tempHash,
                                        url.getProtocol(),
                                        url.getHost(),
                                        Integer.toString(url.getPort()),
                                        uri); 
                } else {
                    replacedTable = replaceTable(tempHash, sessionId);
                }

                if (replacedTable == null) {
                    nres.setException("SessionID ---" + sessionId
                            + "---is Invalid");
                } else {
                    nres.setNamingTable(replacedTable);
                }
                nres.setAttribute(Constants.NAMING_AM_LB_COOKIE, 
                        Session.getLBCookie(sessionId));
            }
        } catch (Exception e) {
            nres.setException(e.getMessage());
        }
        // if request version is less than 3.0, need to replace
        // %uri with the actual value
        if (reqVersion < 3.0) {
            String uri = SystemProperties.get(
                Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
            if (!uri.startsWith("/")) {
                uri = "/" + uri;
            }
            nres.replaceURI(uri);
        }
        return new Response(nres.toXMLString());
    }

    private URL usePreferredNamingURL(NamingRequest request, float reqVersion)
        throws ServerEntryNotFoundException, MalformedURLException {
        String preferredNamingURL = null;
        URL preferredURL = null;

        if (request == null) {
            return null;
        }

        preferredNamingURL = request.getPreferredNamingURL();
        if (preferredNamingURL == null) {
            return null;
        }

        String sessionid = request.getSessionId();
        if (sessionid == null) {
            return null;
        }

        URL url = new URL(request.getPreferredNamingURL());
        String uri = (reqVersion < 3.0) ?
            SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) :
            WebtopNaming.getURI(url);
        String serverID = WebtopNaming.getServerID(url.getProtocol(),
            url.getHost(), Integer.toString(url.getPort()), uri);
        SessionID sessionID = new SessionID(sessionid);
        String primary_id = sessionID.getExtension(SessionID.PRIMARY_ID);

        if (primary_id != null) {
            String secondarysites = WebtopNaming.getSecondarySites(primary_id);

            if ((secondarysites != null) && (serverID != null)) {
                if (secondarysites.indexOf(serverID) != -1) {
                    preferredURL = url;
                }
            }
        }

        return preferredURL;
    }

    private Hashtable replaceTable(Hashtable namingTable, String sessionID) {
        SessionID sessID = new SessionID(sessionID);
        namingDebug.message("SessionId received is --" + sessionID);
         
        return replaceTable(namingTable,
                       sessID.getSessionServerProtocol(),
                       sessID.getSessionServer(),
                       sessID.getSessionServerPort(),
                       sessID.getSessionServerURI());
    }

    private Hashtable replaceTable(Hashtable namingTable,
        String protocol, String host, String port, String uri) {
        if (protocol.equalsIgnoreCase("") || host.equalsIgnoreCase("")
                || port.equalsIgnoreCase("")) {
            return null;
        }
        // Do validation from platform server list
        if (!(protocol.equals(server_proto) && host.equals(server_host) && port
                .equals(server_port))) {
            String cookieURL = protocol + "://" + host + ":" + port;
            String platformList = (String) namingTable
                    .get(Constants.PLATFORM_LIST);
            if (platformList.indexOf(cookieURL) == -1) {
                return null;
            }
        }
        Hashtable tempNamingTable = namingTable;
        // replace all percent here
        for (Enumeration e = tempNamingTable.keys(); e.hasMoreElements();) {
            Object obj = e.nextElement();
            String key = obj.toString();
            String url = (tempNamingTable.get(obj)).toString();
            url = url.replaceAll("%protocol", protocol);
            url = url.replaceAll("%host", host);
            url = url.replaceAll("%port", port);
            url = url.replaceAll("%uri", uri);
            tempNamingTable.put(key, url);
        }
        return tempNamingTable;
    }

    private Hashtable transferTable(Hashtable hashTab) {
        if (hashTab == null)
            return null;
        Hashtable newTab = new Hashtable();
        for (Enumeration e = hashTab.keys(); e.hasMoreElements();) {
            Object obj = e.nextElement();
            String key = obj.toString();
            String value = (hashTab.get(obj)).toString();
            newTab.put(key, value);
        }
        return newTab;
    }

    // The following functions are the implementations of service listener
    // for schema/config change events

    /**
     * This function updates the naming table whenever it gets a schema changed
     * event.
     */
    public void schemaChanged(String serviceName, String version) {
        // Do not update if the servieName is not "iPlanetAMPlatformService"
        if ((serviceName == null)
            || (!serviceName.equals("iPlanetAMPlatformService") && 
            !serviceName.equals("iPlanetAMNamingService"))) {
            return;
        }

        try {
            updateNamingTable();
        } catch (SMSException ex) {
            namingDebug.error("Error occured in updating naming table", ex);
        }
    }

    // We don't need to do anything for the following methods, we kept it
    // for the implementation sake. But, these methods will never be
    // invoked since the Platform service currently does not support
    // global/organization 'Configuration'.

    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        if ((serviceName == null)
                || (!serviceName.equals("iPlanetAMPlatformService") && 
                    !serviceName.equals("iPlanetAMNamingService"))) {
            return;
        }
        try {
            updateNamingTable();
            SessionService ss = SessionService.getSessionService();
            if ((ss != null) && ss.isSessionFailoverEnabled()) {
                ss.ReInitClusterMemberMap();
            }
        } catch (Exception ex) {
            namingDebug.error("Error occured in updating naming table", ex);
        }
    }

    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        // Do nothing
    }

    private static void registFQDNMapping(Set sites) {
        if ((sites == null) || sites.isEmpty()) {
            return;
        }

        MessageFormat form = new MessageFormat(
            "com.sun.identity.server.fqdnMap[{0}]");

        for (Iterator iter = sites.iterator(); iter.hasNext(); ) {
            String entry = (String) iter.next();
            StringTokenizer tok = new StringTokenizer(entry, "|");
            String strUrl = tok.nextToken();
            String strId = tok.nextToken();
            
            try {
                URL url = new URL(strUrl);
                String host = url.getHost();
                if (host != null) {
                    Object[] args = { host };
                    form.format(args);
                    SystemProperties.initializeProperties(
                        form.format(args), host);
                }
            } catch (MalformedURLException ex) {
                namingDebug.error("NamingService.registFQDNMapping", ex);
            }
        }
        
        FQDNUtils.getInstance().init();
    }

    private static Set getSites(Map platformAttrs) throws Exception {
        Set sites = null;

        if (serviceRevNumber < SERVICE_REV_NUMBER_70) {
            Set servers = (Set) platformAttrs.get(Constants.PLATFORM_LIST);
            sites = getSitesFromSessionConfig(servers);
        } else {
            sites = SiteConfiguration.getSiteInfo(sso);
        }

        if (namingDebug.messageEnabled()) {
            if (sites != null) {
                namingDebug.message("Sites : " + sites.toString());
            }
        }

        return sites;
    }
    
    private static Set<String> getSiteNamesAndIDs()
    throws Exception {
        Set<String> siteNames = SiteConfiguration.getSites(sso);
        Set<String> siteNamesAndIDs = new HashSet<String>();
        
        for (String siteName : siteNames) {
            String id = null;
            try {
                id = SiteConfiguration.getSiteID(sso, siteName);
            } catch(SMSException smsException) {
              namingDebug.warning("Unable to determine Site ID for SiteName:["+siteName+"], Ignoring.");
              continue;
            }
            StringBuilder nameAndID = new StringBuilder();
            nameAndID.append(siteName).append(delimiter).append(id);
            siteNamesAndIDs.add(nameAndID.toString());
        }
        
        if (namingDebug.messageEnabled()) {
            namingDebug.message("Site Names: " + siteNames.toString());
        }
        
        return siteNamesAndIDs;
    }

    private static Set getServers(Map platformAttrs, Set sites)
            throws Exception {
        Set servers = ServerConfiguration.getServerInfo(sso);

        if ((sites != null) && (serviceRevNumber < SERVICE_REV_NUMBER_70)) {
            servers = getServersFromSessionConfig(sites, servers);
        }

        if (namingDebug.messageEnabled()) {
            if (servers != null) {
                namingDebug.message("servers : " + servers.toString());
            }
        }

        return servers;
    }

    private static Set getSitesFromSessionConfig(Set platform) throws Exception
    {
        HashSet sites = new HashSet();
        Iterator iter = platform.iterator();

        while (iter.hasNext()) {
            String server = (String) iter.next();
            int idx = server.indexOf(delimiter);
            String serverFQDN = server.substring(0, idx);

            if (sessionConfig.contains(serverFQDN)) {
                sites.add(server);
            }
        }

        return sites.isEmpty() ? null : sites;
    }

    private static Set getServersFromSessionConfig(Set sites, Set platform)
            throws Exception {
        HashSet servers = new HashSet();

        Map clusterInfo = getClusterInfo(sites);
        Iterator serverlist = platform.iterator();

        while (serverlist.hasNext()) {
            String server = (String) serverlist.next();
            if (sites.contains(server)) {
                continue;
            }

            int idx = server.indexOf(delimiter);
            String serverid = server.substring(idx + 1, server.length());
            Iterator keys = clusterInfo.keySet().iterator();
            boolean found = false;

            while (!found && keys.hasNext()) {
                String siteid = (String) keys.next();
                String clusterlist = (String) clusterInfo.get(siteid);
                if (clusterlist.indexOf(serverid) >= 0) {
                    servers.add(server + delimiter + siteid);
                    found = true;
                }
            }

            if (found == false) {
                servers.add(server);
            }
        }

        return servers.isEmpty() ? null : servers;
    }

    private static Hashtable getClusterInfo(Set sites) throws Exception {
        Hashtable clustertbl = new Hashtable();
        Iterator iter = sites.iterator();

        while (iter.hasNext()) {
            String site = (String) iter.next();
            int idx = site.indexOf(delimiter);
            String siteid = site.substring(idx + 1, site.length());
            site = site.substring(0, idx);
            ServiceConfig subConfig = sessionServiceConfig.getSubConfig(site);
            Map sessionAttrs = subConfig.getAttributes();
            String clusterServerList = CollectionHelper.getMapAttr(
                sessionAttrs, Constants.CLUSTER_SERVER_LIST, "");
            clustertbl.put(siteid, clusterServerList);
        }

        return clustertbl;
    }

    private static void insertLBCookieValues(Hashtable nametable)
            throws Exception {
        Map lbCookieMappings = null;

        lbCookieMappings = ServerConfiguration.getLBCookieValues(sso);
       
        if (namingDebug.messageEnabled()) {
            namingDebug.message("NamingService.insertLBCookieValues()" +
            "LBCookie Mappings : " + lbCookieMappings.toString());
        }

        StringBuilder strBuffer = new StringBuilder();
        Set s = lbCookieMappings.keySet();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String val = (String) lbCookieMappings.get(key);
            strBuffer.append(key).append(delimiter).append(val).append(",");
        }

        String strCookieMappings = "";
        if(strBuffer.length() > 0){
            strCookieMappings = strBuffer.substring(0,strBuffer.length()-1);
        }
        nametable.put(
            Constants.SERVERID_LBCOOKIEVALUE_LIST, strCookieMappings);
    }
    
}
