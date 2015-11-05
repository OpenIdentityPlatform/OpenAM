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
package org.forgerock.openam.selfservice.config;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.core.guice.CoreGuiceModule.DNWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utilises the SMS framework to retrieve and handle console configuration for self service.
 *
 * @since 13.0.0
 */
@Singleton
public final class ConsoleConfigHandlerImpl implements ConsoleConfigHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleConfigHandlerImpl.class);

    private final static String SERVICE_NAME = "RestSecurity";
    private final static String SERVICE_VERSION = "1.0";

    private final ServiceConfigManager configManager;
    private final List<ConsoleConfigChangeListener> changeListeners;

    private final DNWrapper dnUtils;

    @Inject
    public ConsoleConfigHandlerImpl(DNWrapper dnUtils, PrivilegedAction<SSOToken> ssoTokenPrivilegedAction) {
        this.dnUtils = dnUtils;
        changeListeners = new CopyOnWriteArrayList<>();

        try {
            configManager = new ServiceConfigManager(ssoTokenPrivilegedAction.run(), SERVICE_NAME, SERVICE_VERSION);
            configManager.addListener(new ConfigChangeHandler());
        } catch (SMSException | SSOException e) {
            throw new ConfigRetrievalException("Unable to retrieve the config manager", e);
        }
    }

    @Override
    public <C extends ConsoleConfig> C getConfig(String realm, ConsoleConfigExtractor<C> extractor) {
        // Mapping from legacy API
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> attributes = getServiceConfig(realm).getAttributes();

        return extractor.extract(attributes);
    }

    private ServiceConfig getServiceConfig(String realm) {
        try {
            return configManager.getOrganizationConfig(realm, null);
        } catch (SMSException | SSOException e) {
            throw new ConfigRetrievalException("Unable to retrieve organisation config", e);
        }
    }

    @Override
    public void registerListener(ConsoleConfigChangeListener listener) {
        changeListeners.add(listener);
    }

    private void notifyListeners(String orgName) {
        String realm = dnUtils.orgNameToRealmName(orgName);

        for (ConsoleConfigChangeListener listener : changeListeners) {
            try {
                listener.configUpdate(realm);
            } catch (RuntimeException rE) {
                logger.error("Unexpected exception whilst updating self service config", rE);
            }
        }
    }

    private final class ConfigChangeHandler implements ServiceListener {

        @Override
        public void organizationConfigChanged(String serviceName, String version,
                String orgName, String groupName, String serviceComponent, int type) {
            if (SERVICE_NAME.equals(serviceName) && SERVICE_VERSION.equals(version)) {
                notifyListeners(orgName);
            }
        }

        @Override
        public void schemaChanged(String serviceName, String version) {
            // Do nothing
        }

        @Override
        public void globalConfigChanged(String serviceName, String version,
                String groupName, String serviceComponent, int type) {
            // Do nothing
        }

    }

}
