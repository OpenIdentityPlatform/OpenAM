/*
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
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package com.iplanet.services.ldap;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.I18n;
import com.iplanet.services.util.XMLParser;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.security.ServerInstanceAction;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.util.Options;

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
    private ConnectionFactory getNewProxyConnectionFactory(String serverGroupID)
            throws LDAPServiceException {
        return getNewConnectionFactory(serverGroupID, LDAPUser.Type.AUTH_PROXY);
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
    public ConnectionFactory getNewProxyConnectionFactory() throws LDAPServiceException {
        return getNewProxyConnectionFactory(DEFAULT);
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
    private ConnectionFactory getNewBasicConnectionFactory(String serverGroupID)
            throws LDAPServiceException {
        return getNewConnectionFactory(serverGroupID, LDAPUser.Type.AUTH_BASIC);
    }

    public ConnectionFactory getNewAdminConnectionFactory() throws LDAPServiceException {

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

        String authID = sCfg.getAuthID();
        String passwd = (String) AccessController.doPrivileged(new ServerInstanceAction(sCfg));

        // The 389 port number passed is overridden by the hostName:port
        // constructed by the getHostName method.  So, this is not
        // a hardcoded port number.
        return LDAPUtils.newFailoverConnectionFactory(
                getLdapUrls(serverGroupID, Server.Type.CONN_SSL.equals(sCfg.getConnectionType())),
                authID, passwd.toCharArray(), 0, null, null);
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
    public ConnectionFactory getNewBasicConnectionFactory() throws LDAPServiceException {
        return getNewBasicConnectionFactory(DEFAULT);
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
    public ConnectionFactory getNewConnectionFactory(String serverGroupID,
            LDAPUser.Type authType) throws LDAPServiceException {
        return getNewFailoverConnectionFactory(serverGroupID, authType);
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
    private ConnectionFactory getNewFailoverConnectionFactory(String serverGroupID,
            LDAPUser.Type type) throws LDAPServiceException {
        debugger.message("in DSConfigMgr.getNewFailoverConnection()");
        String hostName = getHostName(serverGroupID);
        if (hostName.length() == 0) {
            throw new LDAPServiceException(getString(IUMSConstants.DSCFG_SERVER_NOT_FOUND));
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
        return LDAPUtils.newFailoverConnectionFactory(
                getLdapUrls(serverGroupID, Server.Type.CONN_SSL.equals(sCfg.getConnectionType())),
                authID, passwd != null ? passwd.toCharArray() : null, 0, null, Options.defaultOptions());
    }

    private Set<LDAPURL> getLdapUrls(String serverGroupID, boolean isSSL) {
        Set<LDAPURL> ldapUrls = new LinkedHashSet<>();
        ServerGroup serverGrp = getServerGroup(serverGroupID);
        for (Server server : serverGrp.getServersList()) {
            ldapUrls.add(LDAPURL.valueOf(server.getServerName(), server.getPort(), isSSL));
        }
        return ldapUrls;
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

    private void loadServerConfiguration(InputStream is) throws LDAPServiceException {
        // Instantiate the XML classes and pass the file names.
        XMLParser parser = new XMLParser(true, groupHash);

        // Get the data from the xml classes
        parser.register(SERVERGROUP, "com.iplanet.services.ldap.ServerGroup");
        parser.register(SERVER, "com.iplanet.services.ldap.Server");
        parser.register(USER, "com.iplanet.services.ldap.LDAPUser");

        try {
            parser.parse(is);
        } catch (Exception e) {
            debugger.error("DSConfigMgr.loadServerConfiguration: Exception during XML parsing", e);
            throw new LDAPServiceException(LDAPServiceException.FILE_NOT_FOUND, e);
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
