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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.providers;

import static java.util.concurrent.TimeUnit.*;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.forgerock.openam.sm.datalayer.utils.TimeoutConfig;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;

import com.sun.identity.shared.debug.Debug;

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
@Singleton
public class LdapConnectionFactoryProvider implements ConnectionFactoryProvider<Connection> {

    // Injected
    private final TimeoutConfig timeoutConfig;
    private final ConnectionConfigFactory configFactory;
    private final ConnectionCount count;
    private final Debug debug;
    private final ConnectionType connectionType;

    /**
     * Generates an instance and registers the shutdown listener.
     *
     * @param connectionConfigFactory Required to resolve configuration parameters, non null.
     * @param timeoutConfig Timeout Configuration, Non null.
     * @param count Connection Count logic, Non null.
     * @param debug Required for debugging.
     */
    @Inject
    public LdapConnectionFactoryProvider(ConnectionType connectionType,
            ConnectionConfigFactory connectionConfigFactory,
            TimeoutConfig timeoutConfig,
            ConnectionCount count,
            @Named(DataLayerConstants.DATA_LAYER_DEBUG) Debug debug) {
        this.configFactory = connectionConfigFactory;
        this.timeoutConfig = timeoutConfig;
        this.count = count;
        this.debug = debug;
        this.connectionType = connectionType;
    }

    /**
     * Creates instances of ConnectionFactory which are aware of the need to share the
     * DataLayer and CTS connections in the same connection pool.
     *
     * @return {@inheritDoc}
     */
    public ConnectionFactory<Connection> createFactory() throws InvalidConfigurationException {
        ConnectionConfig config = configFactory.getConfig();
        int timeout = timeoutConfig.getTimeout(connectionType);

        LDAPOptions options = new LDAPOptions();
        options.setTimeout(timeout, SECONDS);

        debug("Creating Embedded Factory:\nURL: {0}\nMax Connections: {1}\nHeartbeat: {2}\nOperation Timeout: {3}",
                config.getLDAPURLs(),
                config.getMaxConnections(),
                config.getLdapHeartbeat(),
                timeout);

        final org.forgerock.opendj.ldap.ConnectionFactory ldapConnectionFactory = LDAPUtils.newFailoverConnectionPool(
                config.getLDAPURLs(),
                config.getBindDN(),
                config.getBindPassword(),
                count.getConnectionCount(config.getMaxConnections(), connectionType),
                config.getLdapHeartbeat(),
                SECONDS.toString(),
                options);

        return new LdapConnectionFactory(ldapConnectionFactory);
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(CoreTokenConstants.DEBUG_ASYNC_HEADER + format, args));
        }
    }

    private static class LdapConnectionFactory implements ConnectionFactory<Connection> {
        private final org.forgerock.opendj.ldap.ConnectionFactory ldapConnectionFactory;
        private static final Function<Connection, Connection, DataLayerException> IDENTITY_FUNCTION =
                new Function<Connection, Connection, DataLayerException>() {
                    @Override
                    public Connection apply(Connection value) throws DataLayerException {
                        return value;
                    }
                };
        private static final Function<ErrorResultException, Connection, DataLayerException> EXCEPTION_FUNCTION =
                new Function<ErrorResultException, Connection, DataLayerException>() {
                    @Override
                    public Connection apply(ErrorResultException value) throws DataLayerException {
                        throw new LdapOperationFailedException(value.getResult());
                    }
                };

        public LdapConnectionFactory(org.forgerock.opendj.ldap.ConnectionFactory ldapConnectionFactory) {
            this.ldapConnectionFactory = ldapConnectionFactory;
        }

        @Override
        public Promise<Connection, DataLayerException> createAsync() {
            final PromiseImpl<Connection, ErrorResultException> promise = PromiseImpl.create();
            ldapConnectionFactory.getConnectionAsync(new ConnectionResultHandler(promise));
            return promise.then(IDENTITY_FUNCTION, EXCEPTION_FUNCTION);
        }

        @Override
        public Connection create() throws DataLayerException {
            try {
                return ldapConnectionFactory.getConnection();
            } catch (ErrorResultException e) {
                throw new LdapOperationFailedException(e.getResult());
            }
        }

        @Override
        public void close() {
            ldapConnectionFactory.close();
        }

        @Override
        public boolean isValid(Connection connection) {
            return connection != null && !connection.isClosed() && connection.isValid();
        }

        private static class ConnectionResultHandler implements ResultHandler<Connection> {
            private final PromiseImpl<Connection, ErrorResultException> promise;

            public ConnectionResultHandler(PromiseImpl<Connection, ErrorResultException> promise) {
                this.promise = promise;
            }

            @Override
            public void handleErrorResult(ErrorResultException error) {
                promise.tryHandleError(error);
            }

            @Override
            public void handleResult(Connection result) {
                promise.tryHandleResult(result);
            }
        }
    }
}
