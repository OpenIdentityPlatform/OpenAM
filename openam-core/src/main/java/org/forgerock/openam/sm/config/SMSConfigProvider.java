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

package org.forgerock.openam.sm.config;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides an abstract from the SMS configuration layer.
 *
 * @since 13.0.0
 */
@Singleton
class SMSConfigProvider {

    private final static String SERVICE_VERSION = "1.0";

    private final PrivilegedAction<SSOToken> ssoTokenPrivilegedAction;
    private final Map<String, ServiceConfigManager> configManageCache;

    @Inject
    SMSConfigProvider(PrivilegedAction<SSOToken> ssoTokenPrivilegedAction) {
        this.ssoTokenPrivilegedAction = ssoTokenPrivilegedAction;
        configManageCache = new HashMap<>();
    }

    Map<String, Set<String>> getAttributes(String source, String realm) {
        try {
            ServiceConfigManager configManager = getConfigManager(source);

            // Safe cast due to known legacy API
            @SuppressWarnings("unchecked")
            Map<String, Set<String>> attributes = configManager
                    .getOrganizationConfig(realm, null).getAttributes();

            return attributes;
        } catch (SMSException | SSOException e) {
            throw new ConfigRetrievalException("Unable to retrieve organisation config", e);
        }
    }

    void registerListener(String source, ServiceListener listener) {
        ServiceConfigManager configManager = getConfigManager(source);
        configManager.addListener(listener);
    }

    private ServiceConfigManager getConfigManager(String source) {
        try {
            ServiceConfigManager configManager = configManageCache.get(source);

            if (configManager == null) {
                synchronized (configManageCache) {
                    configManager = configManageCache.get(source);

                    if (configManager == null) {
                        configManager = new ServiceConfigManager(
                                ssoTokenPrivilegedAction.run(), source, SERVICE_VERSION);
                        configManageCache.put(source, configManager);
                    }
                }
            }

            return configManager;
        } catch (SMSException | SSOException e) {
            throw new ConfigRetrievalException("Unable to retrieve config manager " + source, e);
        }
    }

}
