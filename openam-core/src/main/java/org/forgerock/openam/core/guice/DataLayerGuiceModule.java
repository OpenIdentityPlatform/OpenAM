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
package org.forgerock.openam.core.guice;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.adapters.JavaBeanAdapterFactory;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.SMSConfigurationFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConnectionModule;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.utils.ThreadSafeTokenIdGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Guice Module to capture the details of the Data Layer specific bindings. These are bound in a private binder so that
 * each {@link ConnectionType} can have different bindings, allowing different backend databases.
 * <p>
 * ConnectionFactory and TaskExecutor instances will be available outside the private binder using the
 * @{@link DataLayer} annotation with the desired {@link ConnectionType}.
 */
@GuiceModule
public class DataLayerGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TokenIdGenerator.class).to(ThreadSafeTokenIdGenerator.class);

        install(new FactoryModuleBuilder().implement(JavaBeanAdapter.class, JavaBeanAdapter.class)
                .build(JavaBeanAdapterFactory.class));

        Key<Map<ConnectionType, LdapDataLayerConfiguration>> connectionMapKey =
                Key.get(new TypeLiteral<Map<ConnectionType, LdapDataLayerConfiguration>>() {});
        binder().bind(connectionMapKey).toProvider(ConfigurationMapProvider.class).in(Singleton.class);

        for (ConnectionType connectionType : ConnectionType.values()) {
            try {
                DataLayerConnectionModule module = connectionType.getConfigurationClass().newInstance();
                module.setConnectionType(connectionType);
                binder().install(module);
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialise connection module for " + connectionType, e);
            }
        }

    }

    @Provides @Inject @Named(DataLayerConstants.SERVICE_MANAGER_CONFIG)
    ConnectionConfig getDataLayerConfig(SMSConfigurationFactory smsConfigurationFactory) {
        return smsConfigurationFactory.getSMSConfiguration();
    }

    private static final class ConfigurationMapProvider
            implements Provider<Map<ConnectionType, LdapDataLayerConfiguration>> {

        @Override
        public Map<ConnectionType, LdapDataLayerConfiguration> get() {
            Map<ConnectionType, LdapDataLayerConfiguration> configurations =
                    new HashMap<ConnectionType, LdapDataLayerConfiguration>();
            for (ConnectionType type : ConnectionType.values()) {
                LdapDataLayerConfiguration config = InjectorHolder.getInstance(
                        Key.get(LdapDataLayerConfiguration.class, DataLayer.Types.typed(type)));
                configurations.put(type, config);
            }
            return configurations;
        }

    }

}
