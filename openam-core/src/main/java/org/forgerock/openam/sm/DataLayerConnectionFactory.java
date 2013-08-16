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

import com.google.inject.Inject;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.ldap.LDAPUser;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.sm.exceptions.ConnectionCredentialsNotFound;
import org.forgerock.openam.sm.exceptions.ServerConfigurationNotFound;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.LDAPOptions;
import static org.forgerock.openam.core.guice.CoreGuiceModule.ShutdownManagerWrapper;

/**
 * A factory for providing connections to LDAP.
 *
 * Note: This class uses the same connection details as the DataLayer, however this
 * class will use the OpenDJ LDAP SDK rather than the Netscape SDK.
 *
 * @author robert.wapshott@forgerock.com
 */
public class DataLayerConnectionFactory implements ShutdownListener {
    // Injected
    private final Debug debug;
    private final ServerConfigurationFactory parser;

    // State for intialising and shutting down the factory.
    private final ConnectionFactory factory;

    /**
     * Initialise the connection factory.
     *
     * @param parser Non null required configuration parser.
     * @param wrapper
     */
    @Inject
    public DataLayerConnectionFactory(ServerConfigurationFactory parser, ShutdownManagerWrapper wrapper) {
        this(parser, wrapper, SessionService.sessionDebug);
    }

    /**
     * Testing constructor allowing injection of all dependencies.
     *
     * @param parser Non null required configuration parser.
     * @param debug Non null.
     */
    public DataLayerConnectionFactory(ServerConfigurationFactory parser, ShutdownManagerWrapper wrapper, Debug debug) {
        this.parser = parser;
        this.debug = debug;
        wrapper.addShutdownListener(this);

        ServerGroupConfiguration config = getServerConfiguration("sms", "default");
        this.factory = initialiseBalancer(config);
    }

    /**
     * Establish which ServerGroup contains suitable connection details for an Admin connection.
     *
     * Note: If each configuration is invalid, a runtime exception will be thrown.
     *
     * @param groups Each group will be tried in order until one of them has suitable credentials.
     * @return A non null ServerGroupConfiguration which can be used for connections to LDAP.
     *
     * @throws IllegalStateException If all configurations were invalid. There is nothing we can do to recover.
     */
    private synchronized ServerGroupConfiguration getServerConfiguration(String... groups) {
        LDAPUser.Type type = LDAPUser.Type.AUTH_ADMIN;
        for (String group : groups) {
            /**
             * Fetch configuration for the named Group. If the group fails to
             * retrieve valid credentials, this is not necessarily an error
             * as there are a number of groups to try.
             */
            try {
                return parser.getServerConfiguration(group, type);
            } catch (ServerConfigurationNotFound e) {
                debug.message("No Server Configuration found for " + group);
            } catch (ConnectionCredentialsNotFound e) {
                debug.message("Server Configuration missing " + type.toString() + " credentials for Group " + group);
            }
        }
        throw new IllegalStateException("No server configurations found");
    }

    /**
     * Examine the ServerGroupConfiguration and use this to initialise the Load Balancer algorithm.
     *
     * @param config Non null ServerGroupConfiguration describing the connection credentials.
     * @return A non null LoadBalancingAlgorithm implementation.
     */
    private synchronized ConnectionFactory initialiseBalancer(ServerGroupConfiguration config) {
        //At the moment heartbeat interval/timeunit is not configurable for configuration stores, so for now, let's
        //disable heartbeat feature.
        return LDAPUtils.newFailoverConnectionPool(config.getLDAPURLs(), config.getBindDN(), config.getBindPassword(),
                config.getMaxConnections(), -1, null, new LDAPOptions());
    }

    /**
     * Establishes a connection to the first available LDAP server that is monitored by this connection
     * factory.
     *
     * @return A non null connection to the first available LDAP server.
     * @throws ErrorResultException If there was a problem establishing a connection to a valid server.
     */
    public Connection getConnection() throws ErrorResultException {
        return factory.getConnection();
    }

    /**
     * Signal that the connection factory should shutdown and release any connections and resources.
     */
    @Override
    public void shutdown() {
        factory.close();
    }
}