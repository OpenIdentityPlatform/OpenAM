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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.providers;

import com.iplanet.dpro.session.service.SessionConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.forgerock.openam.sm.datalayer.utils.TimeoutConfig;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPOptions;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Responsible for generating ConnectionFactory instances. The instances generated are tailored to
 * the {@link ConnectionType} required by the caller.
 * <p/>
 * This factory provider is aware of two main use cases for the service management layer (also known
 * as Data Layer).
 * <p/>
 * Default - Uses the service management configuration for connections. This will connect to the
 * defined LDAP server, whether that is embedded or external.
 * <p/>
 * External - Uses CTS Configuration for CTS connections which are pointed towards an external
 * LDAP server. Uses service management configuration for {@link StoreMode#DEFAULT} connections.
 */
public class DataLayerConnectionFactoryProvider implements ConnectionFactoryProvider {

    // Injected
    private final TimeoutConfig timeoutConfig;
    private final ConnectionConfigFactory configFactory;
    private final ConnectionCount count;
    private final Debug debug;

    /**
     * Generates an instance and registers the shutdown listener.
     *
     * @param connectionConfigFactory Required to resolve configuration parameters, non null.
     * @param timeoutConfig Timeout Configuration, Non null.
     * @param count Connection Count logic, Non null.
     * @param debug Required for debugging.
     */
    @Inject
    public DataLayerConnectionFactoryProvider(ConnectionConfigFactory connectionConfigFactory,
                                              TimeoutConfig timeoutConfig,
                                              ConnectionCount count,
                                              @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.configFactory = connectionConfigFactory;
        this.timeoutConfig = timeoutConfig;
        this.count = count;
        this.debug = debug;
    }

    /**
     * Creates instances of ConnectionFactory which are aware of the need to share the
     * DataLayer and CTS connections in the same connection pool.
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     */
    public ConnectionFactory createFactory(ConnectionType type) throws InvalidConfigurationException {
        ConnectionConfig config = configFactory.getConfig(type);
        int timeout = timeoutConfig.getTimeout(type);

        LDAPOptions options = new LDAPOptions();
        options.setTimeout(timeout, SECONDS);

        debug("Creating Embedded Factory:\nURL: {0}\nMax Connections: {1}\nHeartbeat: {2}\nOperation Timeout: {3}",
                config.getLDAPURLs(),
                config.getMaxConnections(),
                config.getLdapHeartbeat(),
                timeout);

        return LDAPUtils.newFailoverConnectionPool(
                config.getLDAPURLs(),
                config.getBindDN(),
                config.getBindPassword(),
                count.getConnectionCount(config.getMaxConnections(), type),
                config.getLdapHeartbeat(),
                SECONDS.toString(),
                options);
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }
}
