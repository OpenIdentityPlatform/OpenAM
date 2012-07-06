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
 * $Id: DSConfigMgrBase.java,v 1.3 2009/11/04 19:25:35 veiming Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.XMLException;
import com.iplanet.services.util.XMLParser;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * This object is the manager of all connection information. The server
 * configuration file (serverconfig.xml) is loaded and cached in this object.
 * This class exists as a singleton instance.
 */
public class DSConfigMgrBase implements IDSConfigMgr {
    Hashtable groupHash = new Hashtable();

    public DSConfigMgrBase() {
    }
    
    /**
     * Gets the ServerGroup object reference for this serverGroupID.
     * 
     * @param serverGroupID
     *            The serverGroup ID for the ServerGroup to be retrieved.
     * @return ServerGroup The ServerGroup reference; null if no such Server
     *         group exists
     */
    public ServerGroup getServerGroup(String serverGroupID) {
        return (ServerGroup) groupHash.get(serverGroupID);
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
        if (serverGroupID == null) {
            return null;
        }
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
        return getServerInstance(DSConfigMgr.DEFAULT, authType);
    }

    public void parseServiceConfigXML() 
        throws SMSException, SSOException, XMLException {
        groupHash.clear();
        SSOToken adminToken = (SSOToken)
            AccessController.doPrivileged(AdminTokenAction.getInstance());
        String xml = ServerConfiguration.getServerConfigXML(
            adminToken, SystemProperties.getServerInstanceName());

        /*
         * xml can be null or empty when centralized server configuration
         * is not used i.e. AMConfig.properties and serverconfig.xml
         * reside in the file system. In the case, we do not need
         * to update the server config XML in the system.
         */
        if ((xml != null) && (xml.trim().length() > 0)) {
            XMLParser parser = new XMLParser(true, groupHash);

            // Get the data from the xml classes
            parser.register(DSConfigMgr.SERVERGROUP, 
                "com.iplanet.services.ldap.ServerGroup");
            parser.register(DSConfigMgr.SERVER,
                "com.iplanet.services.ldap.Server");
            parser.register(DSConfigMgr.USER,
                "com.iplanet.services.ldap.LDAPUser");
            parser.parse(xml);
        }
    }

    public String toString() {
        return groupHash.toString();
    }
}
