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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package org.forgerock.openam.sm.datalayer.impl;

import javax.inject.Inject;
import javax.inject.Named;

import com.iplanet.am.util.AMPasswordUtil;
import com.iplanet.am.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.utils.ModifiedProperty;
import org.forgerock.opendj.ldap.DN;

/**
 * A data layer configuration for use with UMA pending requests.
 * @see org.forgerock.openam.sm.datalayer.api.ConnectionType#UMA_PENDING_REQUESTS
 */
public class UmaPendingRequestDataLayerConfiguration extends LdapDataLayerConfiguration {

    private static final String STORE_LOCATION = "org.forgerock.services.uma.pendingrequests.store.location";
    private static final String STORE_HOSTNAME = "org.forgerock.services.uma.pendingrequests.store.directory.name";
    private static final String STORE_USERNAME = "org.forgerock.services.uma.pendingrequests.store.loginid";
    private static final String STORE_PASSWORD = "org.forgerock.services.uma.pendingrequests.store.password";
    private static final String STORE_MAX_CONNECTIONS = "org.forgerock.services.uma.pendingrequests.store.max.connections";
    private static final String STORE_SSL_ENABLED = "org.forgerock.services.uma.pendingrequests.store.ssl.enabled";
    private static final String STORE_HEARTBEAT = "org.forgerock.services.uma.pendingrequests.store.heartbeat";
    private static final String STORE_CUSTOM_ROOT_SUFFIX = "org.forgerock.services.uma.pendingrequests.store.root.suffix";

    @Inject
    public UmaPendingRequestDataLayerConfiguration(@Named(DataLayerConstants.ROOT_DN_SUFFIX) String rootSuffix) {
        super(rootSuffix);
    }

    @Override
    public StoreMode getStoreMode() {
        String mode = SystemProperties.get(STORE_LOCATION);
        if (StringUtils.isNotEmpty(mode)) {
            return StoreMode.valueOf(mode.toUpperCase());
        } else {
            return StoreMode.DEFAULT;
        }
    }

    @Override
    public void updateExternalLdapConfiguration(ModifiedProperty<String> hosts, ModifiedProperty<String> username,
            ModifiedProperty<String> password, ModifiedProperty<String> maxConnections,
            ModifiedProperty<Boolean> sslMode, ModifiedProperty<Integer> heartbeat,
            ModifiedProperty<Boolean> affinityEnabled) {
        hosts.set(SystemProperties.get(STORE_HOSTNAME));
        username.set(SystemProperties.get(STORE_USERNAME));
        password.set(AMPasswordUtil.decrypt(SystemProperties.get(STORE_PASSWORD)));
        maxConnections.set(SystemProperties.get(STORE_MAX_CONNECTIONS));
        sslMode.set(SystemProperties.getAsBoolean(STORE_SSL_ENABLED, false));
        heartbeat.set(SystemProperties.getAsInt(STORE_HEARTBEAT, -1));
    }

    @Override
    protected DN setDefaultTokenDNPrefix(DN root) {
        return root.child("ou=uma_pending_requests");
    }

    @Override
    protected String getCustomTokenRootSuffixProperty() {
        return STORE_CUSTOM_ROOT_SUFFIX;
    }
}
