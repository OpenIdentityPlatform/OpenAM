/**
 * Copyright 2013 ForgeRock, Inc.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package org.forgerock.openam.sm;

import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import org.forgerock.openam.ldap.LDAPURL;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the configuration information for a server group.
 *
 * @author robert.wapshott@forgerock.com
 */
public class ServerGroupConfiguration {
    private ServerGroup group;
    private ServerInstance instance;

    /**
     * New instance of the ServerGroupConfiguration.
     * @param group Non null.
     * @param instance Non null.
     */
    ServerGroupConfiguration(ServerGroup group, ServerInstance instance) {
        this.group = group;
        this.instance = instance;
    }

    /**
     * @return The DN to use for connections to the selected Server Group.
     */
    public String getBindDN() {
        return instance.getAuthID();
    }

    /**
     * @return The password to use for connections to the Server Group.
     */
    public String getBindPassword() {
        return instance.getPasswd();
    }

    /**
     * @return The maximum number of connections that should be used in the connection to LDAP.
     */
    public int getMaxConnections() {
        return instance.getMaxConnections();
    }

    /**
     * @return A non null, but possibly empty mapping of the server hostname to its port
     * for all servers in the selected Server Group.
     */
    public List<LDAPURL> getHostnamesAndPorts() {
        List<LDAPURL> hosts = new LinkedList<LDAPURL>();
        Collection<Server> servers = group.getServersList();
        for (Server server : servers) {
            hosts.add(new LDAPURL(server.getServerName(), server.getPort()));
        }
        return hosts;
    }
}
