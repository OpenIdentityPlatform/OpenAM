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
 * $Id: ServerInstance.java,v 1.2 2008/06/25 05:41:36 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

/**
 * The server instance object represents a tuple of <Server, User, BaseDN>. This
 * Server object provides information regarding the server name, port number.
 * The user object provides the information regarding the authentication type,
 * authentication dn, and password. The baseDN is the search base, which must be
 * honored by the application and restrict its domain under this baseDN.
 */
public class ServerInstance {

    /**
     * The construction of this object is done in the Parser.
     */
    ServerInstance(ServerGroup group, Server theServer, LDAPUser theUser) {
        serverGroup = group;
        server = theServer;
        user = theUser;
    }

    /**
     * Set the active status. Set the status of the server instance. This method
     * must be called if there was some problems with this server instance. e.g
     * While using this server instance, an application gets LDAP_SERVER_DOWN
     * error code, then, the application can set the status to false, so that
     * the next call to <code>getServerInstance(String serviceID)</code> would
     * exclude this particular server instance.
     * 
     * @param serverStatus
     *            Status of the server.
     */
    public synchronized void setActiveStatus(boolean serverStatus) {
        server.setActiveStatus(serverStatus);
    }

    /**
     * Get the status of server instance.
     * 
     * @return boolean the status of the server.
     */
    public synchronized boolean getActiveStatus() {
        return server.getActiveStatus();
    }

    /**
     * Get the name of the server.
     * 
     * @return String Get the name of the server
     */
    public synchronized String getServerName() {
        return server.getServerName();
    }

    /**
     * Get the port number.
     * 
     * @return int The port number of the server instance.
     */
    public synchronized int getPort() {
        return server.getPort();
    }

    /**
     * The serverID of the server to which this server instance is associated
     * with.
     * 
     * @return String The server ID of the server.
     */
    public synchronized String getServerID() {
        return server.getServerID();
    }

    /**
     * Get the Authentication ID i.e. The LDAP Bind DN.
     * 
     * @return String The LDAP bind DN.
     */
    public synchronized String getAuthID() {
        return user.getAuthID();
    }

    /**
     * Get the connection type to the server.
     * 
     * @return Server.Type The server type to the server.
     */
    public Server.Type getConnectionType() {
        return server.getConnectionType();
    }

    /**
     * Get the password to get authenticated.
     * 
     * @return String The password corresponding to the authentication ID.
     */
    public synchronized String getPasswd() {
        return user.getPasswd();
    }

    /**
     * Get the authentication type of this server.
     * 
     * @return int the Authentication code.
     */
    public synchronized LDAPUser.Type getAuthType() {
        return user.getAuthType();
    }

    /**
     * Get the minimum connections that the connection pool is supposed to open.
     * 
     * @return int the Minimum # of connections
     */
    public synchronized int getMinConnections() {
        return serverGroup.minConnPool;
    }

    /**
     * Get the maximum connections that the connection pool is supposed to open.
     * 
     * @return int the Maximum # of connections
     */
    public synchronized int getMaxConnections() {
        return serverGroup.maxConnPool;
    }

    /**
     * The base DN that this server instance has defined. While using this
     * server instance, this base DN must be honored.
     * 
     * @return String The base dn value.
     */
    public synchronized String getBaseDN() {
        return serverGroup.baseDN;
    }

    public synchronized int getIntValue(String key, int defVal) {
        if (serverGroup.miscConfig != null) {
            String attrVal = (String) serverGroup.miscConfig.get(key);
            if (attrVal == null)
                return defVal;

            try {
                int intValue = Integer.parseInt(attrVal);
                return intValue;
            } catch (NumberFormatException ex) {
                return defVal;
            }
        }
        return defVal;
    }

    public synchronized String getStringValue(String key, String defVal) {
        if (serverGroup.miscConfig == null)
            return defVal;

        String retVal = (String) serverGroup.miscConfig.get(key);

        if (retVal == null)
            return defVal;
        else
            return retVal;
    }

    public synchronized boolean getBooleanValue(String key, boolean defVal) {
        if (serverGroup.miscConfig == null)
            return defVal;
        String attrVal = (String) serverGroup.miscConfig.get(key);

        if (attrVal == null)
            return defVal;

        if (attrVal.equalsIgnoreCase("true")) {
            return true;
        } else if (attrVal.equalsIgnoreCase("false")) {
            return false;
        } else
            return defVal;
    }

    public synchronized int getLdapHeatbeat() {
        return serverGroup.getLdapHeartbeat();
    }

    ServerGroup serverGroup = null;

    Server server = null;

    LDAPUser user = null;

    boolean status;

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Server Group Name =");
        buf.append(serverGroup.serverGroupName());
        buf.append(" Server ID:=");
        buf.append(server.getServerID());
        buf.append(" LDAPUser=");
        buf.append(user.getUserID());
        buf.append(" Base DN=");
        buf.append(serverGroup.baseDN);
        return buf.toString();
    }
}
