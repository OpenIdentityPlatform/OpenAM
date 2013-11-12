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

import com.google.inject.Inject;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import org.forgerock.openam.sm.exceptions.ConnectionCredentialsNotFound;
import org.forgerock.openam.sm.exceptions.ServerConfigurationNotFound;

/**
 * Responsible for parsing the DSConfigMgr configuration.
 *
 * The configuration data this class is based on is from the
 * iPlanetAMPlatformService. This stores the Server and Server Group
 * configuration in XML format which are used for LDAP connections.
 *
 * This class provides a simple way to process the configuration and get
 * connection information about the defined LDAP servers.
 *
 * The data is organised by Server Group. Each Server Group is composed of a
 * collection of host names and each host will share the same connection
 * details.
 *
 * @author robert.wapshott@forgerock.com
 */
public class ServerConfigurationFactory {
    private final DSConfigMgr config;

    /**
     * Initialise the configuration parser.
     * @param config Non null server configuraiton.
     */
    @Inject
    public ServerConfigurationFactory(DSConfigMgr config) {
        this.config = config;
    }

    /**
     * Select the Server Group from the configuration.
     *
     * If the server group is valid then this ServerConfigurationFactory will select
     * the Server Group and Instance for subsequent calls.
     *
     * @param groupName The name of the server group. For example: "default" or "sms".
     * @param authType The type of connection credentials that should be selected.
     *
     * @throws IllegalStateException If the Server Configuration did not exist for the
     * named Server Group or the Server Group did not have credentials for the requested
     * connection type.
     */
    public ServerGroupConfiguration getServerConfiguration(String groupName, LDAPUser.Type authType)
            throws ServerConfigurationNotFound, ConnectionCredentialsNotFound {
        ServerGroup serverGroup = config.getServerGroup(groupName);
        ServerInstance instance = config.getServerInstance(groupName, authType);

        if (serverGroup == null) {
            throw new ServerConfigurationNotFound(groupName);
        }

        if (instance == null) {
            throw new ConnectionCredentialsNotFound(authType);
        }

        return new ServerGroupConfiguration(serverGroup, instance);
    }
}
