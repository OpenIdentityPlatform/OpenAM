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
 * $Id: DSConfigMgr.java,v 1.18 2009/01/28 05:34:49 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import com.iplanet.am.util.SSLSocketFactoryManager;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.I18n;
import com.iplanet.services.util.XMLException;
import com.iplanet.services.util.XMLParser;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.security.ServerInstanceAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.LDAPv3;

/**
 * This object is the manager of all connection information. The server
 * configuration file (serverconfig.xml) is loaded and cached in this object.
 * This class exists as a singleton instance.
 */
public class DSConfigMgr implements IDSConfigMgr {

    private static final String LDAP_CONNECTION_NUM_RETRIES =
        "com.iplanet.am.ldap.connection.num.retries";

    private static final String LDAP_CONNECTION_RETRY_INTERVAL = 
        "com.iplanet.am.ldap.connection.delay.between.retries";

    private static final String LDAP_CONNECTION_ERROR_CODES = 
        "com.iplanet.am.ldap.connection.ldap.error.codes.retries";

    // Run time property key to obtain serverconfig.xml path
    private static final String RUN_TIME_CONFIG_PATH = 
        "com.iplanet.coreservices.configpath";

    private int connNumRetry = 3;

    private int connRetryInterval = 1000;

    private HashSet retryErrorCodes = new HashSet();

    static Debug debugger = null;

    static {        
        debugger = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    DSConfigMgr() {
        i18n = I18n.getInstance(IUMSConstants.UMS_PKG);
        groupHash = new Hashtable();
        
        String numRetryStr = SystemProperties.get(LDAP_CONNECTION_NUM_RETRIES);
        if (numRetryStr != null) {
            try {
                connNumRetry = Integer.parseInt(numRetryStr);
            } catch (NumberFormatException e) {
                if (debugger.warningEnabled()) {
                    debugger.warning("Invalid value for "
                            + LDAP_CONNECTION_NUM_RETRIES);
                }
            }
        }

        String retryIntervalStr = SystemProperties.get(
            LDAP_CONNECTION_RETRY_INTERVAL);
        if (retryIntervalStr != null) {
            try {
                connRetryInterval = Integer.parseInt(retryIntervalStr);
            } catch (NumberFormatException e) {
                if (debugger.warningEnabled()) {
                    debugger.warning("Invalid value for "
                            + LDAP_CONNECTION_RETRY_INTERVAL);
                }
            }
        }

        String retryErrs = SystemProperties.get(LDAP_CONNECTION_ERROR_CODES);
        if (retryErrs != null) {
            StringTokenizer stz = new StringTokenizer(retryErrs, ",");
            while (stz.hasMoreTokens()) {
                retryErrorCodes.add(stz.nextToken().trim());
            }
        }
    }

    /**
     * Get the reference to the DSConfigMgr.com.iplanet.am.util. The first one
     * calling it creates the object, which gets stored in a local static
     * variable.
     * 
     * @return DSConfigMgr The singleton instance.
     * @throws LDAPServiceException
     */
    public static synchronized DSConfigMgr getDSConfigMgr()
            throws LDAPServiceException {
        if (thisInstance == null) {
            InputStream is = null;
            try {
                String path = SystemProperties.get(
                    SystemProperties.CONFIG_PATH);
                if (path == null) {
                    // For Backward compatibility obtain from runtime flag
                    path = System.getProperty(RUN_TIME_CONFIG_PATH);
                }
                if (path == null) {
                    throw new LDAPServiceException(
                        LDAPServiceException.FILE_NOT_FOUND,
                        "server configuration XML file is not found. " +
                        "This instance is likely to be running in client mode");
                }
                String configFile = path
                        + System.getProperty("file.separator")
                        + SystemProperties.CONFIG_FILE_NAME;
                is = new FileInputStream(configFile);
            } catch (IOException ex) {                
                if (debugger.warningEnabled()) {
                    debugger.warning("DSConfigMgr.getDSConfigMgr: " 
                            + "serverconfig.xml probably missing. May be " 
                            + "running in client mode  ", ex);
                }
                throw new LDAPServiceException(
                        LDAPServiceException.FILE_NOT_FOUND, ex.getMessage());
            }
            thisInstance = new DSConfigMgr();
            thisInstance.loadServerConfiguration(is);
        }
        return thisInstance;
    }

    public static synchronized void initInstance(InputStream is) 
        throws LDAPServiceException {
        thisInstance = new DSConfigMgr();
        thisInstance.loadServerConfiguration(is);
    }

    /**
     * Get the ServerGroup object reference for this serverGroupID.
     * 
     * @param serverGroupID
     *            The serverGroup ID for the ServerGroup to be retrieved.
     * @return ServerGroup The ServerGroup reference; null if no such Server
     *         group exists
     */
    public ServerGroup getServerGroup(String serverGroupID) {
        return (ServerGroup) groupHash.get(serverGroupID);
    }

    /**
     * Get a new proxy connection from this servergroup.
     * 
     * @param serverGroupID
     *            The server group ID for which the connection is to be created.
     * @return LDAPConnection a new ldap connection.
     * @throws LDAPServiceException
     *             If there is no user in the server group with this
     *             authentication type.
     */
    public LDAPConnection getNewProxyConnection(String serverGroupID)
            throws LDAPServiceException {
        return getNewConnection(serverGroupID, LDAPUser.Type.AUTH_PROXY);
    }

    /**
     * Get a new proxy connection from this servergroup. This method attempts to
     * look for the "default" server configuration in the serverconfig.xml file.
     * 
     * @return LDAPConnection a new ldap connection.
     * @throws LDAPServiceException
     *             If there is no user in the server group with this
     *             authentication type.
     */
    public LDAPConnection getNewProxyConnection() throws LDAPServiceException {
        return getNewProxyConnection(DEFAULT);
    }

    /**
     * Get a new connection from this servergroup. The rootdn might not have
     * proxy rights.
     * 
     * @param serverGroupID
     *            The server group ID for which the connection is to be created.
     * @return LDAPConnection a new ldap connection.
     * @throws LDAPServiceException
     *             If there is no user in the server group with this
     *             authentication type.
     */
    public LDAPConnection getNewBasicConnection(String serverGroupID)
            throws LDAPServiceException {
        return getNewConnection(serverGroupID, LDAPUser.Type.AUTH_BASIC);
    }

    /**
     * 
     */
    public LDAPConnection getNewAdminConnection() throws LDAPServiceException {

        // This api getNewAdminConnection() is used by SMDataLayer.java and
        // EventService.java.
        debugger.message("in DSConfigMgr.getNewAdminConnection()");
        String serverGroupID = DEFAULT;
        LDAPUser.Type type = LDAPUser.Type.AUTH_ADMIN;

        String hostName = getHostName(serverGroupID);
        if(hostName.length() == 0) {
            throw new LDAPServiceException(getString(
                IUMSConstants.DSCFG_SERVER_NOT_FOUND));
        }

        if(debugger.messageEnabled()) {
            debugger.message("DSConfigMgr:getNewAdminConnection():Hostname ="+
                hostName);
        }

        ServerInstance sCfg = getServerInstance(serverGroupID, type);

        String authID = null;
        String passwd = null;
        authID = sCfg.getAuthID();
        passwd = (String) AccessController.doPrivileged(
          new ServerInstanceAction(sCfg));

        // The 389 port number passed is overridden by the hostName:port
        // constructed by the getHostName method.  So, this is not
        // a hardcoded port number.
        return getConnection(hostName, 389, sCfg.getConnectionType(),
            authID, passwd);
    }

    /**
     * Get a new connection from this servergroup. The rootdn might not have
     * proxy rights. This method attempts to look for the "default" server
     * configuration in the serverconfig.xml file.
     * 
     * @return LDAPConnection a new ldap connection.
     * @throws LDAPServiceException
     *             If there is no user in the server group with this
     *             authentication type.
     */
    public LDAPConnection getNewBasicConnection() throws LDAPServiceException {
        return getNewBasicConnection(DEFAULT);
    }

    /**
     * Get a new connection from this servergroup.
     * 
     * @param serverGroupID
     *            The server group ID for which the connection is to be created.
     * @param authType
     *            What kind of authentication do you want?
     * @return LDAPConnection a new ldap connection.
     * @see com.iplanet.services.ldap.LDAPUser.Type
     */
    public LDAPConnection getNewConnection(String serverGroupID,
            LDAPUser.Type authType) throws LDAPServiceException {

        /*
         * Old implementation
         * 
         * ServerInstance scfg = getServerInstance(serverGroupID, authType);
         * 
         * if(scfg == null) { throw new LDAPServiceException(
         * getString(IUMSConstants.DSCFG_SERVER_NOT_FOUND)); }
         * 
         * return getConnection(scfg.getServerName(), scfg.getPort(),
         * scfg.getConnectionType(), scfg.getAuthID(), scfg.getPasswd());
         */

        return getNewFailoverConnection(serverGroupID, authType);
    }

    /**
     * Returns an anonymous LDAP connection using the hostname and port
     * specified in serverconfig.xml for DEFAULT server group. Used by
     * LocalLdapAuthModule.
     */
    public LDAPConnectionPool getAnonymousConnectionPool()
            throws LDAPServiceException {
        LDAPConnection anonymousConnection = getNewFailoverConnection(DEFAULT,
                LDAPUser.Type.AUTH_ANONYMOUS);
        try {
            ServerInstance si = getServerInstance(DEFAULT,
                    LDAPUser.Type.AUTH_ANONYMOUS);
            LDAPConnectionPool pool = null;
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    pool = new LDAPConnectionPool(
                        "DSConfigMgr", si.getMinConnections(),
                        si.getMaxConnections(), anonymousConnection);
                    final LDAPConnectionPool finalPool = pool;
                    shutdownMan.addShutdownListener(
                        new ShutdownListener() {
                            public void shutdown() {
                                if (finalPool != null) {
                                    finalPool.destroy();
                                }
                            }
                        }
                    );
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
            return pool;
        } catch (LDAPException le) {
            if (debugger.messageEnabled()) {
                debugger.message("Failed to create anon conn pool" + le);
            }
            throw (new LDAPServiceException(
                    getString(IUMSConstants.DSCFG_CONNECTFAIL)));
        }
    }

    /**
     * This method give a failover connection. The list of servers in a server
     * group are used to failover.
     * 
     * @param serverGroupID
     *            The serverGroup for which the connection is required.
     * @param type
     *            The type of the user authentication that is required.
     * @see com.iplanet.services.ldap.LDAPUser.Type
     */
    public LDAPConnection getNewFailoverConnection(String serverGroupID,
            LDAPUser.Type type) throws LDAPServiceException {
        debugger.message("in DSConfigMgr.getNewFailoverConnection()");
        String hostName = getHostName(serverGroupID);
        if (hostName.length() == 0) {
            throw new LDAPServiceException(
                    getString(IUMSConstants.DSCFG_SERVER_NOT_FOUND));
        }

        if (debugger.messageEnabled()) {
            debugger.message("Hostname =" + hostName);
        }

        ServerInstance sCfg = getServerInstance(serverGroupID, type);

        String authID = null;
        String passwd = null;
        // Let user name and password be null for anonymous auth type
        if (!type.equals(LDAPUser.Type.AUTH_ANONYMOUS)) {
            authID = sCfg.getAuthID();
            passwd = (String) AccessController
                    .doPrivileged(new ServerInstanceAction(sCfg));
        }

        // The 389 port number passed is overridden by the hostName:port
        // constructed by the getHostName method. So, this is not
        // a hardcoded port number.
        if (type.equals(LDAPUser.Type.AUTH_ANONYMOUS)) {
            return getConnection(hostName, 389, sCfg.getConnectionType(),
                authID, passwd);
        }
        return getPrimaryConnection(hostName, 389, sCfg.getConnectionType(),
                             authID, passwd);
    }

    private LDAPConnection getPrimaryConnection(
        String hostName,
        int port,
        Server.Type type,
        String authID,
        String passwd
    ) throws LDAPServiceException {
        LDAPConnection conn = null;
        hostName = hostName.trim();
        if (hostName.length() == 0) {
            throw new LDAPServiceException(getString(
                IUMSConstants.DSCFG_SERVER_NOT_FOUND));
        }
        
        StringTokenizer st = new StringTokenizer(hostName);
        String hpName = null;
        LDAPServiceException exception = null;

        while (st.hasMoreElements() && (conn == null)) {
            hpName = (String)st.nextToken();
            if ((hpName != null) && (hpName.length() != 0)) {
                if (debugger.messageEnabled()) {
                    debugger.message("DSConfigMgr.getPrimaryConnection: " +
                        "host name & port number " + hpName);
                }
                if (hpName.trim().length() == 0) {
                    throw new LDAPServiceException(getString(
                        IUMSConstants.DSCFG_SERVER_NOT_FOUND));
                }
                
                try {
                    int idx = hpName.indexOf(':');
                    if (idx != -1) {
                        String upHost = hpName.substring(0, idx);
                        int upPort = Integer.parseInt(hpName.substring(idx +1));
                        conn = getConnection(upHost, upPort, type, authID, passwd);
                        exception = null;
                    } else {
                        throw new LDAPServiceException(getString(
                            IUMSConstants.DSCFG_SERVER_NOT_FOUND));
                    }
                } catch (LDAPServiceException e) {
                    exception = e;
                } catch (NumberFormatException e) {
                    throw new LDAPServiceException(e.getMessage());
                }
            }
        }
        
        if (exception != null) {
            String configTime = SystemProperties.get(
                Constants.SYS_PROPERTY_INSTALL_TIME, "false");
            if (!configTime.equalsIgnoreCase("true")) {
                debugger.error("Connection to LDAP server threw exception:", 
                    exception);
            }
            throw exception;
        }
        return conn;
    }

    private LDAPConnection getConnection(String hostName, int port,
            Server.Type type, String authID, String passwd)
            throws LDAPServiceException {

        debugger.message("in DSConfigMgr.getConnection()");
        LDAPConnection conn = null;

        if (type == Server.Type.CONN_SSL) {
            try {
                conn = new LDAPConnection(
                        SSLSocketFactoryManager.getSSLSocketFactory());
            } catch (Exception e) {
                debugger.error("getConnection.JSSSocketFactory", e);
                throw new LDAPServiceException(
                        getString(IUMSConstants.DSCFG_JSSSFFAIL));
            }
        } else {
            conn = new LDAPConnection();
        }

        int retry = 0;
        while (retry <= connNumRetry) {
            if (debugger.messageEnabled()) {
                debugger.message("DSConfigMgr.getConnection retry: " + retry);
            }

            try {
                if ((authID != null) && (passwd != null)) {
                    conn.connect(3, hostName, port, authID, passwd);
                } else {
                    conn.setOption(LDAPv3.PROTOCOL_VERSION, new Integer(3));
                    conn.connect(hostName, port);
                }
                conn.setOption(LDAPv2.SIZELIMIT, new Integer(0));
                break;
            } catch (LDAPException e) {
                if (!retryErrorCodes.contains("" + e.getLDAPResultCode())
                        || retry == connNumRetry) {
                    if (debugger.warningEnabled()) {
                        debugger.warning(
                            "Connection to LDAP server threw exception:", e);
                    }
                   try {
                       conn.disconnect();
                   } catch (Exception ignored) {
                   }
                    throw new LDAPServiceException(
                            getString(IUMSConstants.DSCFG_CONNECTFAIL), e);
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                }
            }
        }

        return conn;
    }

    public String getHostName(String serverGroupID) {
        ServerGroup serverGrp = getServerGroup(serverGroupID);
        Collection serverList = serverGrp.getServersList();

        StringBuilder hostName = new StringBuilder();

        Iterator serverIterator = serverList.iterator();

        while (serverIterator.hasNext()) {
            Server serverObj = (Server) serverIterator.next();
            hostName.append(serverObj.getServerName());
            hostName.append(':');
            hostName.append(serverObj.getPort());
            hostName.append(' ');
        }
        hostName.deleteCharAt(hostName.length() - 1);
        return hostName.toString();
    }

    /**
     * Given the server group ID, this method returns the active and best
     * available server instance. The "best available" criteria is based on the
     * priority. The priority is order in which the servers are listed in the
     * configuration file. The first has the best priority and the last entry
     * has the least priority. This method returns null if no qualified server
     * instance is found or no such server group exists
     * 
     * @param serverGroupID
     *            The serverGroupID for which the server instance is fetched.
     * @param authType
     *            The auth type is the privilege that the user in the
     *            configuration must have.
     * @return ServerInstance The server instance object that holds the server
     *         configuration information.
     * @see LDAPUser.Type
     */
    public ServerInstance getServerInstance(String serverGroupID,
            LDAPUser.Type authType) {
        if (serverGroupID == null)
            return null;

        ServerGroup svc = getServerGroup(serverGroupID);
        if (svc != null) {
            return svc.getServerInstance(authType);
        }
        return null;
    }

    /**
     * Get the instance from server group, which is defined as default.
     * 
     * @param authType
     *            The auth type is the privilege that the user in the
     *            configuration must have.
     * @return ServerInstance The server instance object that holds the server
     *         configuration information.
     * @see LDAPUser.Type
     */
    public ServerInstance getServerInstance(LDAPUser.Type authType) {
        return getServerInstance(DEFAULT, authType);
    }

    private void loadServerConfiguration(InputStream is)
            throws LDAPServiceException {
        // Instantiate the XML classes and pass the file names.
        XMLParser parser = new XMLParser(true, groupHash);

        // Get the data from the xml classes
        parser.register(SERVERGROUP, "com.iplanet.services.ldap.ServerGroup");
        parser.register(SERVER, "com.iplanet.services.ldap.Server");
        parser.register(USER, "com.iplanet.services.ldap.LDAPUser");

        try {
            parser.parse(is);
        } catch (XMLException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            throw new LDAPServiceException(LDAPServiceException.FILE_NOT_FOUND,
                    e.getMessage());
        }
    }

    public String toString() {
        return groupHash.toString();
    }

    /**
     * Get the string associated with the resource string.
     * 
     * @param keyword
     *            The resource keyword.
     * @return The Resource string assocated with this keyword.
     */
    static String getString(String keyword) {
        return i18n.getString(keyword);
    }

    Hashtable groupHash = null;

    /**
     * Constants used during parsing.
     */
    public static final String ROOT = "iPlanetDataAccessLayer";

    public static final String SERVERGROUP = "ServerGroup";

    public static final String SERVER = "Server";

    public static final String USER = "User";

    public static final String SERVER_ID = "serverid";

    public static final String CERTIFICATE = "Certificate";

    public static final String AUTH_USER_ID = "authUser";

    public static final String AUTH_ID = "DirDN";

    public static final String AUTH_PASSWD = "DirPassword";

    public static final String AUTH_TYPE = "type";

    public static final String BASE_DN = "BaseDN";

    public static final String MISC_CONFIG = "MiscConfig";

    public static final String NAME = "name";

    public static final String HOST = "host";

    public static final String PORT = "port";

    public static final String MAX_CONN_POOL = "maxConnPool";

    public static final String MIN_CONN_POOL = "minConnPool";

    public static final String VALUE = "value";

    public static final String VAL_INACTIVE = "inactive";

    public static final String VAL_AUTH_BASIC = "auth";

    public static final String VAL_AUTH_PROXY = "proxy";

    public static final String VAL_AUTH_REBIND = "rebind";

    public static final String VAL_AUTH_ADMIN = "admin";

    public static final String VAL_AUTH_ANONYMOUS = "anonymous";

    public static final String VAL_STYPE_SSL = "SSL";

    public static final String VAL_STYPE_SIMPLE = "SIMPLE";

    // Defaults
    public static final int DEF_INIT_CP_LEN = 1;

    public static final int DEF_MAX_CP_LEN = 1;

    // Single instance of this object.
    static DSConfigMgr thisInstance = null;

    private static I18n i18n = null;

    public static final String SCHEMA_BUG_PROPERTY = 
        "com.sun.identity.shared.ldap.schema.quoting";

    public static final String VAL_STANDARD = "standard";

    // Error Tokens
    public static final String INVALID_SERVER_ID = "InvalidServerID";

    public static final String SERVER_ID_DOES_NOT_EXIST = 
        "ServerIDDoesNotExist";

    public static final String INVALID_USER_ID = "InvalidUserID";

    public static final String DEFAULT = "default";
}
