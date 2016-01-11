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
 */
package com.iplanet.services.naming;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * ServiceListeners provides a simplified API for creating appropriate {@link ServiceListener} instances
 * to respond to changes in either Configuration or Schema.
 *
 * This class is intended to reduce the boiler plate code required to enable a service to respond
 * to configuration changes.
 *
 * For an example of usage, the following indicates an example of listening for changes to the
 * global configuration changes in iPlanetAMNamingService.
 *
 * <code>
 *     ServiceListeners builder = ...
 *     Action action = ... // Implementation to respond when config/service changes.
 *     builder.config("iPlanetAMNamingService").global(action).listen();
 * </code>
 *
 * Note: The listener triggering order should be considered non-deterministic. <b>Do not depend
 * on the order</b> of listener triggering.
 */
public class ServiceListeners {
    private final PrivilegedAction<SSOToken> action;

    @Inject
    public ServiceListeners(PrivilegedAction<SSOToken> action) {
        this.action = action;
    }

    public ListenerBuilder config(String serviceName) {
        try {
            return new ListenerBuilder(new ServiceConfigManager(serviceName, AccessController.doPrivileged(action)));
        } catch (SMSException | SSOException e) {
            throw new IllegalStateException(e);
        }
    }

    public ListenerBuilder schema(String serviceName) {
        try {
            return new ListenerBuilder(new ServiceSchemaManager(serviceName, AccessController.doPrivileged(action)));
        } catch (SMSException | SSOException e) {
            throw new IllegalStateException(e);
        }
    }

    private enum ConfigType {
        GLOBAL,
        ORGANISATION,
        SCHEMA
    }

    /**
     * Builder responsible for providing fluent-like functions for building up
     * Action instances which will respond to changes in Service configuration.
     */
    public static class ListenerBuilder {
        private final Map<ConfigType, Collection<Action>> actions;
        private ServiceSchemaManager schemaManager = null;
        private ServiceConfigManager configManager = null;

        public ListenerBuilder() {
            actions = new HashMap<>();
            for (ConfigType type : ConfigType.values()) {
                actions.put(type, new ArrayList<Action>());
            }
        }

        public ListenerBuilder(ServiceConfigManager configManager) {
            this();
            this.configManager = configManager;
        }

        public ListenerBuilder(ServiceSchemaManager schemaManager) {
            this();
            this.schemaManager = schemaManager;
        }

        public ListenerBuilder global(Action action) {
            add(ConfigType.GLOBAL, action);
            return this;
        }

        public ListenerBuilder organisation(Action action) {
            add(ConfigType.ORGANISATION, action);
            return this;
        }

        public ListenerBuilder schema(Action action) {
            add(ConfigType.SCHEMA, action);
            return this;
        }

        private void add(ConfigType t, Action a) {
            actions.get(t).add(a);
        }

        public void listen() {
            ServiceListener listener = new ServiceListener() {
                @Override
                public void schemaChanged(String serviceName, String version) {
                    for (Action a : actions.get(ConfigType.SCHEMA)) {
                        a.performUpdate();
                    }
                }

                @Override
                public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent, int type) {
                    for (Action a : actions.get(ConfigType.GLOBAL)) {
                        a.performUpdate();
                    }
                }

                @Override
                public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName, String serviceComponent, int type) {
                    for (Action a : actions.get(ConfigType.ORGANISATION)) {
                        a.performUpdate();
                    }
                }
            };
            if (schemaManager != null) {
                schemaManager.addListener(listener);
            }
            if (configManager != null) {
                configManager.addListener(listener);
            }
        }
    }

    /**
     * A generic listener which will respond to a configuration or schema change event.
     */
    public interface Action {
        void performUpdate();
    }
}
