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

package org.forgerock.openam.cts.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.QueueConfiguration;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.utils.ModifiedProperty;
import org.forgerock.opendj.ldap.DN;

import com.iplanet.am.util.AMPasswordUtil;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

public class CTSDataLayerConfiguration extends LdapDataLayerConfiguration {

    @Inject
    public CTSDataLayerConfiguration(@Named(DataLayerConstants.ROOT_DN_SUFFIX) String rootDnSuffix) {
        super(rootDnSuffix);
    }

    @Override
    public StoreMode getStoreMode() {
        String mode = SystemProperties.get(CoreTokenConstants.CTS_STORE_LOCATION);
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
        hosts.set(SystemProperties.get(CoreTokenConstants.CTS_STORE_HOSTNAME));
        username.set(SystemProperties.get(CoreTokenConstants.CTS_STORE_USERNAME));
        password.set(AMPasswordUtil.decrypt(SystemProperties.get(CoreTokenConstants.CTS_STORE_PASSWORD)));
        maxConnections.set(SystemProperties.get(CoreTokenConstants.CTS_STORE_MAX_CONNECTIONS));
        sslMode.set(SystemProperties.getAsBoolean(CoreTokenConstants.CTS_STORE_SSL_ENABLED, false));
        heartbeat.set(SystemProperties.getAsInt(Constants.LDAP_HEARTBEAT, -1));
    }

    @Override
    protected DN setDefaultTokenDNPrefix(DN root) {
        return getTokenRootDN(root);
    }

    public static DN getTokenRootDN(DN root) {
        return root.child("ou=tokens")
                .child("ou=openam-session")
                .child("ou=famrecords");
    }

    @Override
    protected String getCustomTokenRootSuffixProperty() {
        return CoreTokenConstants.CTS_ROOT_SUFFIX;
    }
}
