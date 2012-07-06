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
 * $Id: Server.java,v 1.3 2008/06/25 05:41:36 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import com.iplanet.services.util.ParseOutput;
import com.iplanet.services.util.XMLException;
import com.iplanet.services.util.XMLParser;
import com.iplanet.ums.IUMSConstants;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represents a server. This class holds the server name and the port
 * information.
 */
public class Server implements ParseOutput {
    /**
     * The constructor to instantiate the server object. The creation interface
     * is only exposed to this package.
     */
    public Server() {
    }

    /**
     * Get the server's port.
     * 
     * @return The port number.
     */
    public int getPort() {
        return serverPort;
    }

    /**
     * The server name, as defined in the xml. The package does not do any fqdn
     * conversion or reverse address lookup.
     * 
     * @return the server name.
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Get the type of the connection to the server.
     * 
     * @return Server.Type The connection type to the server.
     */
    public Type getConnectionType() {
        return connType;
    }

    /**
     * The server id that's defined in the configuration file. It is good to
     * keep this server id unique within one service. If the same server
     * configuration repeats in another section, use the same server id.
     * 
     * @return the server id
     */
    public String getServerID() {
        return serverID;
    }

    /**
     * This method is an implementation of the interface and must be called only
     * by the <code>XMLParser</code> object. This is my final plea. I'm a
     * process not process which can load information from xml file for you. So
     * stop calling me.
     */
    public void process(
        XMLParser parser,
        String name,
        Vector elems,
        Hashtable atts, 
        String Pcdata
    ) throws XMLException {
        if (DSConfigMgr.debugger.messageEnabled()) {
            DSConfigMgr.debugger.message("in Server.process()");
        }
        if (name.equals(DSConfigMgr.SERVER)) {
            serverID = (String) atts.get(DSConfigMgr.NAME);
            serverName = (String) atts.get(DSConfigMgr.HOST);
            String connTypeStr = (String) atts.get(DSConfigMgr.AUTH_TYPE);
            String serverPortStr = (String) atts.get(DSConfigMgr.PORT);

            if (connTypeStr == null) {
                connTypeStr = DSConfigMgr.VAL_STYPE_SIMPLE;
            }

            if (connTypeStr.equalsIgnoreCase(DSConfigMgr.VAL_STYPE_SSL)) {
                connType = Type.CONN_SSL;
            } else {
                connType = Type.CONN_SIMPLE;
            }

            try {
                serverPort = Integer.parseInt(serverPortStr);
            } catch (NumberFormatException ex) {
                serverPort = 389;
            }
        } else {
            throw new XMLException(DSConfigMgr
                    .getString(IUMSConstants.DSCFG_DIRSERVER_NODE_EXPECTED));
        }
    }

    /**
     * Set the servers status
     * 
     * @param boolean
     *            True if the server is active, false if otherwise.
     */
    void setActiveStatus(boolean status) {
        serverStatus = status;
    }

    /**
     * Get the server's active Status
     * 
     * @return boolean True, if the server is active; false if otherwise.
     */
    public boolean getActiveStatus() {
        return serverStatus;
    }

    /**
     * Some toString functions for dump purposes.
     * 
     * @return String the string rep.
     */
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Server Name=").append(serverName);
        str.append(" Server Port=").append(serverPort);
        str.append(" Status=").append(serverStatus);
        return str.toString();
    }

    // Connection pool parameters

    int serverPort;

    String serverName;

    String serverID;

    Type connType;

    boolean serverStatus = true;

    public static class Type {
        int type = -1;

        /**
         * The user has anonyomous rights.
         */
        public static final Type CONN_SIMPLE = new Type(0);

        /**
         * The user is authenticated with a rootdn and password.
         */
        public static final Type CONN_SSL = new Type(1);

        private Type(int type) {
            this.type = type;
        }

        public boolean equals(Type type) {
            return (this.type == type.type);
        }

        public String toString() {
            if (equals(CONN_SIMPLE)) {
                return "SIMPLE";
            }
            if (equals(CONN_SSL)) {
                return "SSL";
            }
            return "SIMPLE";
        }
    }

}
