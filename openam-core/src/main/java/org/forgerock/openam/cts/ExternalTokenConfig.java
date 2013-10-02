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
package org.forgerock.openam.cts;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.utils.ModifiedProperty;

import javax.inject.Inject;

/**
 * Responsible for modelling the external configuration options for the Core Token Service.
 *
 * These
 *
 * @author robert.wapshott@forgerock.com
 */
public class ExternalTokenConfig {

    /**
     * Describe the two possible modes the CTS connection factory can operate in.
     */
    public enum StoreMode {
        /**
         * The CTS can defer to the standard Configuration Store settings. These may be
         * the internal embedded store, or a configured external store.
         */
        DEFAULT,
        /**
         * The CTS Connection Factory can instead operate on an entirely external
         * data store. The connection details are assumed to be assigned.
         */
        EXTERNAL;
    }

    private ModifiedProperty<StoreMode> storeMode = new ModifiedProperty<StoreMode>();
    private ModifiedProperty<String> hostname = new ModifiedProperty<String>();
    private ModifiedProperty<String> port = new ModifiedProperty<String>();
    private ModifiedProperty<String> username = new ModifiedProperty<String>();
    private ModifiedProperty<String> password = new ModifiedProperty<String>();
    private ModifiedProperty<String> maxConnections = new ModifiedProperty<String>();
    private ModifiedProperty<Boolean> sslMode = new ModifiedProperty<Boolean>();

    @Inject
    public ExternalTokenConfig() {
        // This is the default value.
        storeMode.set(StoreMode.DEFAULT);
    }

    /**
     * The Store Mode indicates whether the CTSConnectionFactory should use an external
     * store, or passthrough to the default settings based on the Configuration Store.
     *
     * @see StoreMode for more details.
     *
     * @return The Store Mode which is to be used. Maybe null.
     */
    public StoreMode getStoreMode() {
        return storeMode.get();
    }

    /**
     * @return The External Token Store Hostname. Maybe null.
     */
    public String getHostname() {
        return hostname.get();
    }

    /**
     * @return The External Token Store port as a String. Maybe null.
     */
    public String getPort() {
        return port.get();
    }

    /**
     * @return The External Token Store username. Maybe null.
     */
    public String getUsername() {
        return username.get();
    }

    /**
     * @return The External Token Store password. Maybe null.
     */
    public String getPassword() {
        return password.get();
    }

    public String getMaxConnections() {
        return maxConnections.get();
    }

    /**
     * @return True indicates the External Token Store should use SSL for its connection.
     */
    public boolean isSslMode() {
        return sslMode.get();
    }

    public boolean hasChanged() {
        return storeMode.hasChanged() ||
               hostname.hasChanged() ||
               port.hasChanged() ||
               username.hasChanged() ||
               password.hasChanged() ||
               maxConnections.hasChanged() ||
               sslMode.hasChanged();
    }

    /**
     * {@inheritDoc}
     *
     * Causes this instance to refresh its configuration using System Properties.
     */
    public void update() {
        hostname.set(SystemProperties.get(Constants.CTS_STORE_HOSTNAME));
        port.set(SystemProperties.get(Constants.CTS_STORE_PORT));
        username.set(SystemProperties.get(Constants.CTS_STORE_USERNAME));
        password.set(SystemProperties.get(Constants.CTS_STORE_PASSWORD));
        maxConnections.set(SystemProperties.get(Constants.CTS_MAX_CONNECTIONS));
        sslMode.set(SystemProperties.getAsBoolean(Constants.CTS_SSL_ENABLED, false));

        String mode = SystemProperties.get(Constants.CTS_STORE_LOCATION);
        if (StringUtils.isNotEmpty(mode)) {
            storeMode.set(StoreMode.valueOf(mode.toUpperCase()));
        } else {
            storeMode.set(StoreMode.DEFAULT);
        }
    }
}
