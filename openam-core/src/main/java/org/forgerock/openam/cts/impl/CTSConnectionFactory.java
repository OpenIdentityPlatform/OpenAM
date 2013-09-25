/*
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
package org.forgerock.openam.cts.impl;

import com.google.inject.name.Named;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.ExternalTokenConfig;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.ConnectionFailedException;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.FutureResult;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.ResultHandler;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides connections to the token store, which based on the state of the CTS configuration
 * may be the default mechanism, or external.
 *
 * Note: This class delegates its connection behaviour to the DataLayerConnectionFactory in the
 * default case.
 *
 * @author robert.wapshott@forgerock.com
 * @author jonathan.scudder@forgerock.com
 */
public class CTSConnectionFactory implements ConnectionFactory, ShutdownListener {

    private static final String DEBUG_CONNECTION_CHANGED =
            CoreTokenConstants.DEBUG_HEADER +
            "Connection Factory updated:\n" +
            "       Mode: {0}";
    private static final String DEBUG_EXT_DETAILS =
            "   Hostname: {0}\n" +
            "       Port: {1}\n" +
            "    Bind DN: {2}\n" +
            "Connections: {3}";

    // Injected
    private final Debug debug;
    private final DataLayerConnectionFactory dlcf;
    private final LDAPConfig ldapConfig;
    private final ExternalTokenConfig external;

    private ConnectionFactory factory = null;

    /**
     * The connection factory registers as a config listener in case these settings change.
     *
     * @param dlcf Non null, required for default case.
     * @param ldapConfig LDAP Configuration must be monitored for changes.
     * @param external External configuration must be monitored for changes.
     * @param debug Required debugging instance.
     */
    @Inject
    public CTSConnectionFactory(DataLayerConnectionFactory dlcf, LDAPConfig ldapConfig,
                                ExternalTokenConfig external, @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.dlcf = dlcf;
        this.ldapConfig = ldapConfig;
        this.external = external;
        this.debug = debug;

        // Refresh the configuration sources.
        this.ldapConfig.update();
        this.external.update();
        updateConnection();
    }

    /**
     * Trigger a test of the configuration to determine whether it has been updated
     * and if updated, reconfigure the connection.
     *
     * Synchronized because we want to avoid CTS operations happening at the same time
     * as the connection reconfiguration.
     */
    public synchronized void updateConnection() {
        if (ldapConfig.hasChanged() || external.hasChanged()) {
            reconfigureConnection();
        }
    }

    /**
     * Selects the appropriate connection factory based on the configuration.
     *
     * If there was a previous connection open, this is then closed.
     */
    private synchronized void reconfigureConnection() {
        // Save the old factory temporarily
        ConnectionFactory previous = factory;

        try {
            switch (external.getStoreMode()) {
                case DEFAULT:
                    factory = configureDefault();
                    if (debug.messageEnabled()) {
                        debug.message(MessageFormat.format(
                                DEBUG_CONNECTION_CHANGED,
                                ExternalTokenConfig.StoreMode.DEFAULT));
                    }
                    break;
                case EXTERNAL:
                    factory = configureExternal();
                    if (debug.messageEnabled()) {

                        String message = MessageFormat.format(DEBUG_CONNECTION_CHANGED,
                                ExternalTokenConfig.StoreMode.EXTERNAL);

                        String details = MessageFormat.format(
                                DEBUG_EXT_DETAILS,
                                external.getHostname(),
                                external.getPort(),
                                external.getUsername(),
                                external.getMaxConnections());

                        debug.message(message + "\n" + details);
                    }
                    break;
                default:
                    // Error, should never reach this point. Return without reconfiguring
                    debug.error("Illegal State for Store Mode: " + external.getStoreMode());
                    return;
            }

            // If no other error occurred, close the old factory
            if (previous != null && !previous.equals(dlcf)) {
                previous.close();
            }

        } catch (ConnectionFailedException cfe) {
            debug.error("Configuration of token store failed, check token store settings", cfe);

            // Revert to the original factory
            factory = previous;
        }
    }

    /**
     * @return The default DataLayerConnectionFactory connection.
     */
    private ConnectionFactory configureDefault() {
        return dlcf;
    }

    /**
     * @return A non null established connection to the external store.
     * @throws ConnectionFailedException If there was a problem with the configuration
     * or a problem establishing a connection to the store.
     */
    private ConnectionFactory configureExternal() throws ConnectionFailedException {
        if (!isExternalConfigValid()) {
            // Error situation - report then fail
            String msg = "Invalid settings: all settings must be provided";
            debug.error(msg);
            throw new ConnectionFailedException(msg);
        }

        int portNum = parseNumber(external.getPort());
        int maxConnections = parseNumber(external.getMaxConnections());

        if (portNum == -1 || maxConnections == -1) {
            String msg = "Invalid settings: Invalid numeric setting";
            debug.error(msg);
            throw new ConnectionFailedException(msg);
        }

        LDAPURL ldapurl = LDAPURL.valueOf(external.getHostname(), portNum, external.isSslMode());
        Set<LDAPURL> set = new HashSet<LDAPURL>();
        set.add(ldapurl);

        return LDAPUtils.newFailoverConnectionPool(
                set,
                external.getUsername(),
                external.getPassword().toCharArray(),
                maxConnections,
                -1,
                null,
                new LDAPOptions());
    }

    /**
     * @return True if the configuration is present.
     */
    private boolean isExternalConfigValid() {
        if (StringUtils.isEmpty(external.getHostname()) ||
            StringUtils.isEmpty(external.getUsername()) ||
            StringUtils.isEmpty(external.getPassword()) ||
            StringUtils.isEmpty(external.getPort()) ||
            StringUtils.isEmpty(external.getMaxConnections())) {
            return false;
        }
        return true;
    }

    /**
     * @param value The string to parse to an integer.
     * @return -1 if the number was invalid. Otherwise a valid integer.
     */
    private int parseNumber(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * Provides a connection to the applicable token store, whether embedded or external.
     * @return a connection to the token store.
     * @throws ErrorResultException unable to provide a connection.
     */
    public synchronized Connection getConnection() throws ErrorResultException {
        return factory.getConnection();
    }

    /**
     * Provides an asynchronous connection to the token store.
     * @param resultHandler the result handler.
     * @return an asynchronous connection.
     */
    public FutureResult<Connection> getConnectionAsync(ResultHandler<? super Connection> resultHandler) {
        return factory.getConnectionAsync(resultHandler);
    }

    /**
     * Closes the factory, tidying up connections as required.
     */
    public void close() {
        IOUtils.closeIfNotNull(factory);
    }

    /**
     * The function to run when the system shuts down.
     */
    public void shutdown() {
        close();
    }
}
