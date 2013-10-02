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
 * $Id: ServerGroup.java,v 1.5 2009/01/28 05:34:49 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.iplanet.services.ldap;


import com.iplanet.services.util.GenericNode;
import com.iplanet.services.util.ParseOutput;
import com.iplanet.services.util.XMLException;
import com.iplanet.services.util.XMLParser;
import com.iplanet.ums.IUMSConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import java.util.List;

public class ServerGroup implements ParseOutput {
    /**
     * Not to be called. This is a method to be called by the parser to read the
     * xml information.
     */
    public void process(
        XMLParser parser,
        String name,
        Vector elems,
        Hashtable atts, 
        String Pcdata
    ) throws XMLException {
        if (DSConfigMgr.debugger.messageEnabled()) {
            DSConfigMgr.debugger.message("in ServerGroup.process()");
        }
        if (name.equals(DSConfigMgr.SERVERGROUP)) {
            // get the group ID.
            groupName = (String) atts.get(DSConfigMgr.NAME);

            // Get the Servers
            for (int i = 0; i < elems.size(); i++) {
                Object obj = elems.elementAt(i);

                if (DSConfigMgr.debugger.messageEnabled()) {
                    DSConfigMgr.debugger.message("Object of type:"
                            + obj.getClass().getName());
                }

                if (obj instanceof Server) {
                    if (servers == null) {
                        if (DSConfigMgr.debugger.messageEnabled()) {
                            DSConfigMgr.debugger
                                    .message("Initializing servers list.");
                        }
                        servers = new ArrayList<Server>();
                    }
                    servers.add((Server) obj);
                } else if (obj instanceof LDAPUser) {
                    if (users == null) {
                        users = new ArrayList();
                    }
                    users.add(obj);
                } else if (obj instanceof GenericNode) {
                    // if it is generic node, its probably the base dn.
                    GenericNode x = (GenericNode) obj;
                    if (x._name.equals(DSConfigMgr.BASE_DN)) {
                        if (x._pcdata != null) {
                            if (!DN.isDN(x._pcdata)) {
                                throw new XMLException(
                                        DSConfigMgr.getString(
                                           IUMSConstants.DSCFG_INVALID_BASE_DN)
                                                + x._pcdata);
                            }
                        }
                        baseDN = LDAPDN.normalize(x._pcdata);
                    } else if (x._name.equals(DSConfigMgr.MISC_CONFIG)) {
                        String attrName = (String) x._atts
                                .get(DSConfigMgr.NAME);
                        String attrValue = (String) x._atts
                                .get(DSConfigMgr.VALUE);
                        if (name != null && name.length() > 0) {
                            if (miscConfig == null) {
                                miscConfig = new HashMap();
                            }
                            miscConfig.put(attrName, attrValue);
                        }
                    }
                }
            }

            if (servers == null || baseDN == null) {
                String errorMsg = null;
                if (servers == null) {
                    errorMsg = "No server object found in the server group:"
                            + groupName;
                }
                if (baseDN == null) {
                    errorMsg = "No base DN string defined in the server group:"
                            + groupName;
                }
                throw new XMLException(errorMsg);
            }

            // Get the rest of the attributes
            String maxConnPoolStr = System.getProperty("max_conn_pool");
            if (maxConnPoolStr == null)
                maxConnPoolStr = (String) atts.get(DSConfigMgr.MAX_CONN_POOL);
            String minConnPoolStr = System.getProperty("min_conn_pool");
            if (minConnPoolStr == null)
                minConnPoolStr = (String) atts.get(DSConfigMgr.MIN_CONN_POOL);

            try {
                maxConnPool = Integer.parseInt(maxConnPoolStr);
            } catch (NumberFormatException ex) {
                maxConnPool = 10;
            }

            try {
                minConnPool = Integer.parseInt(minConnPoolStr);
            } catch (NumberFormatException ex) {
                minConnPool = 1;
            }

            String ldapHeartbeatStr = System.getProperty(Constants.LDAP_HEARTBEAT);
            if (ldapHeartbeatStr == null)
                ldapHeartbeatStr = (String) atts.get(Constants.LDAP_HEARTBEAT);

            try {
                ldapHeartbeat = Integer.parseInt(ldapHeartbeatStr);
            } catch (NumberFormatException ex) {
                ldapHeartbeat = 10;
            }

        } else {
            throw new XMLException(DSConfigMgr
                    .getString(IUMSConstants.DSCFG_SERVERGROUP_NODE_EXPECTED));
        }

        // Put it in the list of groups.
        parser.getGroupContainer().put(groupName, this);
    }

    /**
     * Get the server instance that's currently active.
     * 
     * @param authType
     *            Defined in LDAPUser.java there are four types of users.
     * @return ServerInstance The active instance is returned. If no instance is
     *         active, null is returned.
     * @see LDAPUser.Type
     */
    public ServerInstance getServerInstance(LDAPUser.Type authType) {
        Server serv = null;
        for (int i = 0; i < servers.size(); i++) {
            serv = servers.get(i);
            if (serv != null && serv.getActiveStatus() == true) {
                break;
            }
        }

        LDAPUser user = null;
        for (int i = 0; i < users.size(); i++) {
            user = (LDAPUser) users.get(i);
            if (user != null && user.getAuthType().equals(authType)) {
                break;
            }
        }

        if ((serv != null) && (user != null)) {
            return new ServerInstance(this, serv, user);
        }

        return null;
    }

    public String serverGroupName() {
        return groupName;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Server Group Name=");
        buf.append(groupName);
        for (int i = 0; i < users.size(); i++) {
            buf.append('\n');
            buf.append(' ');
            buf.append(users.get(i).toString());
        }
        for (int i = 0; i < servers.size(); i++) {
            buf.append('\n');
            buf.append(' ');
            buf.append(servers.get(i).toString());
        }
        buf.append("Min Connection Pool=");
        buf.append(minConnPool);
        buf.append(" Max Connection Pool=");
        buf.append(maxConnPool);
        return buf.toString();
    }

    /**
     * The list of servers that are defined in this server group.
     */
    public Collection getServersList() {
        return servers;
    }

    public int getLdapHeartbeat() {
        return ldapHeartbeat;
    }

    String baseDN = null;

    int maxConnPool = -1;

    int minConnPool = -1;

    List<Server> servers = null;

    List users = null;

    HashMap miscConfig = null;

    private String groupName;

    private int ldapHeartbeat;
}
