/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServiceBase.java,v 1.2 2009/01/28 05:34:58 ww203982 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.common;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.AccessController;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.net.ssl.HttpsURLConnection;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchResults;


import com.iplanet.am.util.SSLSocketFactoryManager;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOToken;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.Bootstrap;
import com.sun.identity.setup.BootstrapData;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceManager;



/**
 * This is the base class for Server related functionality.
 * Any class that needs server specific methods can use this base
 * class.
 */
public abstract class ServiceBase implements ToolConstants, ServiceConstants {
    
    private static SSOToken adminToken = null;
    
    /**
     * Helper method to return Admin token when the configuration
     * properties is initialized.
     *
     * @return Admin Token.
     */
    protected static SSOToken getAdminSSOToken() {
        if (adminToken == null) {
            adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        }
        return adminToken;
    }
    
    /**
     * Helper method to load the config using the boot file
     *
     * @param bootPath filename location of the boot file
     * @return properties of the configured server.
     */
    protected Properties loadConfigFromBootfile(String bootPath)
        throws Exception  {
        if (adminToken != null) {
            try {
                doRefresh(adminToken);
                adminToken = null;
            } catch (Exception sre) {
                Debug.getInstance(DEBUG_NAME).error(
                    "ServiceBase.loadConfigFromBootfile : " +
                    "Exception in clearing cache", sre);
            }
        }
        Properties cProp = null;
        if (isServerBootable((Map)getBootServers(bootPath))) { 
            System.setProperty("bootstrap.dir", bootPath);
            cProp = Bootstrap.load(bootPath, true);
            SystemProperties.initializeProperties(cProp);
        }
        return cProp;
    }
    
    /**
     * Helper method to retrive the boot servers from the boot file
     *
     * @param path filename location of the boot file
     * @return map of the boot directory servers
     */
    protected Map<String, Map> getBootServers(String path)
        throws IOException, UnsupportedEncodingException {
        return getBootfileContent(path);
    }


    /**
     * Helper method to retrive the boot instance from the boot list
     *
     * @param bServers Map of available boot servers
     * @return <code>true</code> if server is bootable
     */
    protected boolean isServerBootable(
        Map<String, Map> bServers
    ) {
       boolean match = false;
        Map <String, String> dsInstance = new HashMap<String, String>();
        for (Iterator<String> it = bServers.keySet().iterator();
            it.hasNext() && !match; ) {
            String dsInstanceKey = it.next();
            dsInstance = (Map)bServers.get(dsInstanceKey);
            if (isDSRunning(dsInstance)) {
                if (isValidSuffix(dsInstance)) {
                    match = true;
                } 
            } 
        }
        return match;
    }

    
    private Map getBootfileContent(String path)
        throws IOException, UnsupportedEncodingException {      
        BootstrapData  bootstrapData = new BootstrapData(path);
        List list =  bootstrapData.getData();
        
        Map<String,String> mapQuery = null;
        Map<String, Map> bootList = new HashMap();
        String dsprotocol = "unknown";
        for (Iterator<String> i = list.iterator(); i.hasNext(); ) {
            String info = i.next();
            if (info.startsWith(PROTOCOL_LDAPS)) {
                info = "http://" +  info.substring(8);
                dsprotocol = "ldaps";
            } else
                if (info.startsWith(PROTOCOL_LDAP)) {
                info = "http://" +  info.substring(7);
                dsprotocol = "ldap";
                }
            URL url = new URL(info);
            mapQuery = queryStringToMap(url.getQuery());
            String instanceName = URLDecoder.decode(url.getPath(), "UTF-8");
            if (instanceName.startsWith("/")) {
                instanceName = instanceName.substring(1);
            }
            mapQuery.put(SERVER_INSTANCE, instanceName);
            String dsHost = url.getHost();
            String dsPort  = Integer.toString(url.getPort());
            mapQuery.put(DS_HOST, dsHost);
            mapQuery.put(DS_PORT, dsPort);
            mapQuery.put(DS_PROTOCOL, dsprotocol);
            
            String key = dsHost + ":" + dsPort;
            bootList.put(key, mapQuery);
        }
        return bootList;
    }
    
    private Map queryStringToMap(String str)
        throws UnsupportedEncodingException {
        Map map = new HashMap();
        StringTokenizer st = new StringTokenizer(str, "&");
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            int idx = s.indexOf('=');
            map.put(s.substring(0, idx), URLDecoder.decode(
                s.substring(idx +1), "UTF-8"));
        }
        return map;
    }
    
    /**
     * Method to check if the server instance is running
     *
     * @param sName server instance name
     * @return <code>true</code> if server instance is running
     */
    protected static boolean isServerRunning(String sName) {
        boolean isSvrRunning = false;
        try {
            URLConnection uc = new URL(sName).openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                uc.getInputStream()));
            isSvrRunning = true;
        } catch (MalformedURLException mfe) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isServerRunning : " +
                "Exception in getting server URL", mfe);
        } catch (IOException ioe) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isServerRunning : " +
                "Exception in connecting to server URL", ioe);
        }
        return isSvrRunning;
    }
    
    /**
     * Method to check if the server instance is running
     *
     * @param url server instance URL
     * @return <code>true</code> if server instance is running
     */
    protected static boolean isServerRunning(URL url) {
        boolean isSvrRunning = false;
        try {
            URLConnection uc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                uc.getInputStream()));
            isSvrRunning = true;
        } catch (MalformedURLException mfe) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isServerRunning : " +
                "Exception in getting server URL", mfe);
        } catch (IOException ioe) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isServerRunning : " +
                "Exception in connecting to server URL", ioe);
        }
        return isSvrRunning;
    }
    
    /**
     * Method to connect to server instance
     *
     * @param svrName server instance name
     * @return <code>true</code> if server instance can be connected
     */
    protected boolean connectToServer(String  svrName)
        throws javax.net.ssl.SSLHandshakeException, Exception {
        boolean connect = false;
        try {
            URL u = new URL(svrName);
            URLConnection  svrConn = u.openConnection();
            if (u.getProtocol().equalsIgnoreCase("http")){
                HttpURLConnection testConnect =
                    (HttpURLConnection)svrConn;
                testConnect.connect();
            } else if (u.getProtocol().equalsIgnoreCase("https")) {
                HttpsURLConnection testConnect =
                    (HttpsURLConnection)svrConn;
                testConnect.connect();
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                svrConn.getInputStream()));
            connect = true;
        } catch (javax.net.ssl.SSLHandshakeException ssle) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.connectToServer : ", ssle);
            throw ssle;
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.connectToServer : ", ex);
            throw ex;
        }
        return connect;
    }
    
    
    /**
     * Helper method to construct the URL from properties.
     *
     * @param prop properties of the server  instance
     * @return string representing the URL of the instance
     */
    protected String getURLStrFromProperties(Properties prop) {
        String propURL = null;
        String hostname = prop.getProperty(AM_SERVER_HOST);
        String port = prop.getProperty(AM_SERVER_PORT);
        String protocol = prop.getProperty(AM_SERVER_PROTOCOL);
        String uri = prop.getProperty(AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        
        propURL = protocol + "://" + hostname + ":" + port + uri;
        return propURL;
    }
    
    /**
     * Helper method to construct the URL from properties.
     *
     * @param mProp map of server instance properties
     * @return string representing the URL of the instance
     */
    public String getURLStrFromProperties(Map mProp) {
        String propURL = null;
        String hostname = (String)mProp.get(AM_SERVER_HOST);
        String port = (String)mProp.get(AM_SERVER_PORT);
        String protocol = (String)mProp.get(AM_SERVER_PROTOCOL);
        String uri = (String)mProp.get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        
        propURL = protocol + "://" + hostname + ":" + port + uri;
        return propURL;
    }
    
    /**
     * Read the properties file and initialize the
     * Map with the properties.
     *
     * @param pName name of resource bundle to load the properties
     * @return map of properties loaded from a resource bundle
     */
    protected static Map loadPropertiesToMap(String pName) {
        Map<String, String>  propMap = new HashMap<String, String>();
        propMap.clear();
        ResourceBundle rb = ResourceBundle.getBundle(pName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            propMap.put(key, (String)rb.getString(key));
        }
        return propMap;
    }
    
    /**
     * Read the properties file and initialize the
     * Map with the properties.
     *
     * @param prop object containing properties
     * @return map of properties loaded from properties object
     */
    protected static Map loadPropertiesToMap(Properties prop) {
        Map<String, String> propMap = new HashMap<String, String>();
        for (Enumeration e = prop.propertyNames(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            propMap.put(key, (String)prop.getProperty(key));
        }
        return propMap;
    }
    
    /**
     * Method to check if the host name is valid
     *
     * @param hName name of the host
     * @return <code>true</code> if host name can be resolved
     */
    protected boolean isValidHost(String hName) {
        boolean valid = false;
        try {
            InetAddress.getByName(hName.trim());
            valid = true;
        } catch (java.net.UnknownHostException e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isValidHost: " + e);
        }
        return valid;
    }
    
    /**
     * Method to check if the port is valid
     *
     * @param port Port of the host
     * @return <code>true</code> if host name can be resolved
     */
    protected boolean isValidPort(String port) {
        boolean valid = false;
        try {
            int n = Integer.parseInt(port);
            valid = true;
        } catch (NumberFormatException nfe) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isValidPort: " + nfe);
        }
        return valid;
    }
    
    /**
     * Helper method to clear the cache
     */
    private static boolean doRefresh(SSOToken ssoToken) {
        boolean valid = true;
        try {
            AMIdentityRepository.clearCache();
            ServiceManager svcMgr = new ServiceManager(ssoToken);
            svcMgr.clearCache();
        } catch (Exception sms) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.doRefresh: " + sms);
            valid = false;
        }
        return valid;
    }
    
    /**
     * Returns a LDAP connection to the directory host.
     *
     * @param dsHostName name of the sever where DS is installed
     * @param dsPort port at which the directory server is listening
     * @param dsProtocol protocol used by directory server
     * @param dsManager admin user name for directory server
     * @param dsAdminPwd  admin password used by admin user name
     * @return LDAP connection
     */
    protected static LDAPConnection getLDAPConnection(
        String dsHostName,
        int dsPort,
        String dsProtocol,
        String dsManager,
        String dsAdminPwd
    ) {
        LDAPConnection ld = null;
        try {
            ld = (dsProtocol.equalsIgnoreCase("ldaps")) ?
                new LDAPConnection(
                SSLSocketFactoryManager.getSSLSocketFactory()) :
                new LDAPConnection();
            ld.setConnectTimeout(300);
            ld.connect(3, dsHostName, dsPort, dsManager, dsAdminPwd);
        } catch (Exception e) {
            disconnectDServer(ld);
            ld = null;
        }
        return ld;
    }
    
    /**
     * Returns a LDAP connection to the directory host.
     *
     * @param paramMap Map containing directory specific information
     * @return LDAP connection
     */
    private LDAPConnection getLDAPConnection(
        Map paramMap
    ) {
        String dsHost = (String)paramMap.get(DS_HOST);
        String dsPort = (String)paramMap.get(DS_PORT);
        String dsProtocol = (String)paramMap.get(DS_PROTOCOL);
        String dsMgr = (String)paramMap.get(DS_MGR);
        String dsPwd = (String)paramMap.get(DS_PWD);
        dsPwd = Crypt.decode(dsPwd, Crypt.getHardcodedKeyEncryptor());
        LDAPConnection ld = null;
        try {
            ld = (dsProtocol.equalsIgnoreCase("ldaps")) ?
                new LDAPConnection(
                    SSLSocketFactoryManager.getSSLSocketFactory()) :
                new LDAPConnection();
            ld.setConnectTimeout(300);
            ld.connect(3, dsHost, getPort(dsPort), dsMgr, dsPwd);
        } catch (Exception e) {
            disconnectDServer(ld);
            ld = null;
        }
        return ld;
    }
       
    /**
     * Check if Directory Server has the suffix.
     *
     * @return <code>true</code> if specified suffix exists.
     */
    protected static boolean connectDSwithDN(
        LDAPConnection ld,
        String suffix
    ) {
        String filter = "cn=" + suffix;
        String[] attrs = { "" };
        LDAPSearchResults results = null;
        boolean isValidSuffix = true;
        try {
            results = ld.search(suffix,
                LDAPConnection.SCOPE_BASE, filter, attrs, false);
        } catch (LDAPException e) {
            isValidSuffix = false;
        }
        return isValidSuffix;
    }   
    
    /**
     * Helper method to disconnect from Directory Server.
     */
    private static void disconnectDServer(LDAPConnection ld) {
        if ((ld != null) && ld.isConnected()) {
            try {
                ld.disconnect();
            } catch (LDAPException e) {
                Debug.getInstance(DEBUG_NAME).error(
                    "ServiceBase.disconnectDServer: " +
                    "LDAP Operation return code: " +
                    e.getLDAPResultCode());
            }
        }
    }
    
    /**
     * Check if Directory Server is running.
     *
     * @return <code>true</code> if directory is running.
     */
    protected static boolean isDSServerUp(
        String dsHostName,
        int dsPort,
        String dsProtocol,
        String dsManager,
        String dsAdminPwd
    ) {
        boolean canConnect = false;
        LDAPConnection ld = null;
        ld = getLDAPConnection(dsHostName, dsPort,
            dsProtocol, dsManager, dsAdminPwd);
        if ((ld != null) && ld.isConnected()) {
            canConnect = true;
            disconnectDServer(ld);
        }
        return canConnect;
    }
    
    /**
     * Check if config Directory Server is running.
     *
     * @return <code>true</code> if config directory is running.
     */
    protected boolean isDSRunning(Map instanceMap) {
        String dsHost = (String)instanceMap.get(DS_HOST);
        String dsPort = (String)instanceMap.get(DS_PORT);
        String dsMgr = (String)instanceMap.get(DS_MGR);
        String dsPwd = (String)instanceMap.get(DS_PWD);
        String dsProto = (String)instanceMap.get(DS_PWD);
        boolean  dsRunning = false;
        if (!isDSServerUp(dsHost, getPort(dsPort), dsProto, dsMgr,
            Crypt.decode(dsPwd, Crypt.getHardcodedKeyEncryptor()))) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isDSRunning : " +
                "Cannot connect to Directory Server :" +
                dsHost + ":" + dsPort);
        } else {
            dsRunning = true;
        }
        return dsRunning;
    }
    
    /**
     * Check if Directory Server has the given suffix.
     *
     * @return <code>true</code> if suffix is valid.
     */
    protected boolean isValidSuffix(Map instanceMap) {
        boolean  validSuffix = false;
        if (connectDSwithDN(getLDAPConnection(instanceMap),
            (String)instanceMap.get(DS_BASE_DN))) {
            validSuffix = true;
        } else {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.isValidateSuffix : " +
                "Cannot connect to Directory Server :" +
                instanceMap.get(DS_HOST) +
                ":" + instanceMap.get(DS_PORT) +
                " with suffix" + DS_BASE_DN);
        }
        return validSuffix;
    }
    
    /**
     * Validates the directory server port and returns as an int value.
     *
     * @return port of directory server.
     */
    private int getPort(String strPort) {
        int port = 0;
        try {
            port =  Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServiceBase.getPort: " +
                "Exception in getting port information", e);
        }
        return port;
    }
}
