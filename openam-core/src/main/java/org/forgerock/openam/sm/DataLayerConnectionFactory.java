/*
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
 *
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.sm;

import com.google.inject.Inject;
import com.iplanet.dpro.session.service.SessionConstants;
import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.FutureResult;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

/**
 * A factory for providing connections to LDAP.
 *
 * Note: This class uses the same connection details as the DataLayer, however this
 * class will use the OpenDJ LDAP SDK rather than the Netscape SDK.
 */
public class DataLayerConnectionFactory implements ConnectionFactory, ShutdownListener {
    // Injected
    private final Debug debug;

    // State for initialising and shutting down the factory.
    private final ConnectionFactory factory;

    /**
     * Initialise the connection factory.
     *
     * @param configurationFactory Required for resolving required configuration.
     * @param shutdownManager Required to monitor the state of system shutdown.
     * @param debug Required for debugging.
     */
    @Inject
    public DataLayerConnectionFactory(SMSConfigurationFactory configurationFactory,
                                      ShutdownManager shutdownManager,
                                      @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.factory = initialiseBalancer(configurationFactory.getSMSConfiguration());
        shutdownManager.addShutdownListener(this);
        this.debug = debug;
    }

    /**
     * Examine the ServerGroupConfiguration and use this to initialise the Load Balancer algorithm.
     *
     * @param config Non null ServerGroupConfiguration describing the connection credentials.
     * @return A non null LoadBalancingAlgorithm implementation.
     */
    private synchronized ConnectionFactory initialiseBalancer(ServerGroupConfiguration config) {
        return LDAPUtils.newFailoverConnectionPool(config.getLDAPURLs(), config.getBindDN(), config.getBindPassword(),
                config.getMaxConnections(), config.getLdapHeartbeat(), TimeUnit.SECONDS.toString(), new LDAPOptions());
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
    public void shutdown() {
        factory.close();
    }

    /**
     * Closes the underlying connection factory.
     */
    public void close() {
        factory.close();
    }

    /**
     * Returns an asynchronous connection from the underlying connection factory.
     * @param resultHandler the result handler
     * @return the FutureResult from the underlying factory.
     */
    public FutureResult<Connection> getConnectionAsync(ResultHandler<? super Connection> resultHandler) {
        return factory.getConnectionAsync(resultHandler);
    }
}