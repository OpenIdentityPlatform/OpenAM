/**
 * Copyright 2013 ForgeRock AS.
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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.forgerock.openam.ldap.LDAPURL;

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
    public char[] getBindPassword() {
        return instance.getPasswd().toCharArray();
    }

    /**
     * @return The maximum number of connections that should be used in the connection to LDAP.
     */
    public int getMaxConnections() {
        return instance.getMaxConnections();
    }

    /**
     * @return The heartbeat of the LDAP connection. May return -1 which indicates do not use a heartbeat
     */
    public int getLdapHeartbeat() {
        return instance.getLdapHeatbeat();
    }

    /**
     * Creates a list of {@link LDAPURL} instances based on the server instances available in the servergroup.
     *
     * @return A non null, but possibly empty list of {@link LDAPURL} instances based on the configured server
     * instances in the corresponding server group.
     */
    public Set<LDAPURL> getLDAPURLs() {
        Collection<Server> servers = group.getServersList();
        Set<LDAPURL> ret = new LinkedHashSet<LDAPURL>(servers.size());
        for (Server server : servers) {
            ret.add(LDAPURL.valueOf(server.getServerName(), server.getPort(),
                    Server.Type.CONN_SSL.equals(server.getConnectionType())));
        }
        return ret;
    }
}
