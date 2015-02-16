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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.utils.ModifiedProperty;
import org.forgerock.opendj.ldap.DN;

import com.iplanet.am.util.AMPasswordUtil;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

/**
 * A data layer configuration for use with resource set tokens.
 * @see org.forgerock.openam.sm.datalayer.api.ConnectionType#RESOURCE_SETS
 */
public class ResourceSetDataLayerConfiguration extends LdapDataLayerConfiguration {

    private static final String STORE_LOCATION = "org.forgerock.services.resourcesets.store.location";
    private static final String STORE_HOSTNAME = "org.forgerock.services.resourcesets.store.directory.name";
    private static final String STORE_USERNAME = "org.forgerock.services.resourcesets.store.loginid";
    private static final String STORE_PASSWORD = "org.forgerock.services.resourcesets.store.password";
    private static final String STORE_MAX_CONNECTIONS = "org.forgerock.services.resourcesets.store.max.connections";
    private static final String STORE_SSL_ENABLED = "org.forgerock.services.resourcesets.store.ssl.enabled";
    private static final String STORE_HEARTBEAT = "org.forgerock.services.resourcesets.store.heartbeat";
    private static final String STORE_CUSTOM_ROOT_SUFFIX = "org.forgerock.services.resourcesets.store.root.suffix";

    @Inject
    public ResourceSetDataLayerConfiguration(@Named(DataLayerConstants.ROOT_DN_SUFFIX) String rootSuffix) {
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
            ModifiedProperty<Boolean> sslMode, ModifiedProperty<Integer> heartbeat) {
        hosts.set(SystemProperties.get(STORE_HOSTNAME));
        username.set(SystemProperties.get(STORE_USERNAME));
        password.set(AMPasswordUtil.decrypt(SystemProperties.get(STORE_PASSWORD)));
        maxConnections.set(SystemProperties.get(STORE_MAX_CONNECTIONS));
        sslMode.set(SystemProperties.getAsBoolean(STORE_SSL_ENABLED, false));
        heartbeat.set(SystemProperties.getAsInt(STORE_HEARTBEAT, -1));
    }

    @Override
    protected DN setDefaultTokenDNPrefix(DN root) {
        return root.child("ou=resource_sets");
    }

    @Override
    protected String getCustomTokenRootSuffixProperty() {
        return STORE_CUSTOM_ROOT_SUFFIX;
    }
}
