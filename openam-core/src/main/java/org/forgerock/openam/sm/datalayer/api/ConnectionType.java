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
package org.forgerock.openam.sm.datalayer.api;

import org.forgerock.openam.cts.impl.CTSConnectionModule;
import org.forgerock.openam.cts.impl.CTSDataLayerConfiguration;
import org.forgerock.openam.cts.impl.CTSAsyncConnectionModule;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.reaper.CTSReaper;
import org.forgerock.openam.entitlement.indextree.IndexTreeService;
import org.forgerock.openam.sm.SMSConnectionModule;
import org.forgerock.openam.sm.SMSDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.impl.ResourceSetConnectionModule;
import org.forgerock.openam.sm.datalayer.impl.ResourceSetDataLayerConfiguration;
import org.forgerock.openam.utils.StringUtils;

import com.iplanet.am.util.SystemProperties;

/**
 * Defines the types of connections factories that should be used to provide connections
 * to callers.
 * <p>
 * When adding a new ConnectionType, you will need to add a new default configuration class,
 * and both the ConnectionCount and TimeoutConfig logic for deducing how many connections to
 * allocate and with what timeout will also need to be updated.
 * <p>
 * When customising the data layer for a particular type (i.e. to use a non-LDAP backend),
 * you will need to implement a separate implementation of the configuration class and all
 * the different types it configures, as well as setting a system property of
 * {@code org.forgerock.openam.sm.datalayer.[ConnectionType.name()]}.
 */
public enum ConnectionType {

    /**
     * @see TaskDispatcher
     */
    CTS_ASYNC(CTSAsyncConnectionModule.class),
    /**
     * @see CTSReaper
     */
    CTS_REAPER(CTSConnectionModule.class),
    /**
     * @see IndexTreeService
     */
    DATA_LAYER(SMSConnectionModule.class),
    /**
     * @see org.forgerock.oauth2.resources.ResourceSetStore
     */
    RESOURCE_SETS(ResourceSetConnectionModule.class);

    private static final String CONFIGURATION_CLASS_PROPERTY_PREFIX = "org.forgerock.openam.sm.datalayer.module.";
    private final Class<? extends DataLayerConnectionModule> configurationClass;

    private ConnectionType(Class<? extends DataLayerConnectionModule> defaultConfigurationClass) {
        this.configurationClass = defaultConfigurationClass;
    }

    public Class<? extends DataLayerConnectionModule> getConfigurationClass() {
        String configuredTypeKey = CONFIGURATION_CLASS_PROPERTY_PREFIX + this.name();
        String configuredType = SystemProperties.get(configuredTypeKey);
        if (StringUtils.isNotBlank(configuredType)) {
            try {
                return Class.forName(configuredType).asSubclass(DataLayerConnectionModule.class);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Configured data layer configuration type does not exist: " +
                        configuredTypeKey + " is configured as " + configuredType);
            }
        }
        return configurationClass;
    }
}
