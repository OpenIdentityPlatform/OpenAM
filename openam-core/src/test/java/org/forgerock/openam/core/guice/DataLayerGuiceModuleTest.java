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

package org.forgerock.openam.core.guice;

import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.openam.shared.guice.SharedGuiceModule;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.SMSConfigurationFactory;
import org.forgerock.openam.sm.ServerGroupConfiguration;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.ExternalLdapConfig;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.openam.sm.utils.ConfigurationValidator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;

@GuiceModules({DataLayerGuiceModuleTest.TestIsolationGuiceModule.class, DataLayerGuiceModule.class, SharedGuiceModule.class})
public class DataLayerGuiceModuleTest extends GuiceTestCase {

    @DataProvider(name = "instances")
    public Object[][] guiceInstances() {
        return new Object[][] {
                new Object[]{QueryFactory.class, ConnectionType.DATA_LAYER},
                new Object[]{ConnectionFactory.class, ConnectionType.DATA_LAYER},
                new Object[]{TaskExecutor.class, ConnectionType.CTS_ASYNC},
                new Object[]{TaskFactory.class, ConnectionType.CTS_ASYNC},
                new Object[]{QueryFactory.class, ConnectionType.CTS_ASYNC},
                new Object[]{ConnectionFactory.class, ConnectionType.CTS_ASYNC},
                new Object[]{TaskExecutor.class, ConnectionType.CTS_REAPER},
                new Object[]{TaskFactory.class, ConnectionType.CTS_REAPER},
                new Object[]{QueryFactory.class, ConnectionType.CTS_REAPER},
                new Object[]{ConnectionFactory.class, ConnectionType.CTS_REAPER},
                new Object[]{TaskExecutor.class, ConnectionType.RESOURCE_SETS},
                new Object[]{TaskFactory.class, ConnectionType.RESOURCE_SETS},
                new Object[]{QueryFactory.class, ConnectionType.RESOURCE_SETS},
                new Object[]{ConnectionFactory.class, ConnectionType.RESOURCE_SETS},
                new Object[]{TokenDataStore.class, ConnectionType.RESOURCE_SETS},
        };
    }

    @Test(dataProvider = "instances")
    public void testBindingConfiguration(Class<?> type, ConnectionType connectionType) throws Exception {
        InjectorHolder.getInstance(Key.get(type, DataLayer.Types.typed(connectionType)));
    }

    public static class TestIsolationGuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            SMSConfigurationFactory smsConfigurationFactory = mock(SMSConfigurationFactory.class);
            when(smsConfigurationFactory.getSMSConfiguration()).thenReturn(mock(ServerGroupConfiguration.class));
            bind(SMSConfigurationFactory.class).toInstance(smsConfigurationFactory);

            bind(ExternalLdapConfig.class).toInstance(mock(ExternalLdapConfig.class));
            bind(CTSConnectionMonitoringStore.class).toInstance(mock(CTSConnectionMonitoringStore.class));

            bind(Debug.class).annotatedWith(Names.named(DataLayerConstants.DATA_LAYER_DEBUG)).toInstance(mock(Debug.class));
            bind(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_ASYNC_DEBUG)).toInstance(mock(Debug.class));

            LdapConnectionFactoryProvider factoryProvider = mock(LdapConnectionFactoryProvider.class);
            when(factoryProvider.createFactory()).thenReturn(mock(ConnectionFactory.class));
            bind(LdapConnectionFactoryProvider.class).toInstance(factoryProvider);

            bind(ExecutorService.class).annotatedWith(Names.named(CoreTokenConstants.CTS_WORKER_POOL)).toProvider(new Provider<ExecutorService>() {
                @Override
                public ExecutorService get() {
                    return mock(ExecutorService.class);
                }
            });

            bind(String.class).annotatedWith(Names.named(DataLayerConstants.ROOT_DN_SUFFIX)).toInstance("ou=root-dn");

            ConfigurationValidator validator = mock(ConfigurationValidator.class);
            doNothing().when(validator).validate(any(ConnectionConfig.class));
            bind(ConfigurationValidator.class).toInstance(validator);

            ConnectionConfigFactory connectionConfigFactory = mock(ConnectionConfigFactory.class);
            ConnectionConfig config = mock(ConnectionConfig.class);
            when(connectionConfigFactory.getConfig()).thenReturn(config);
            when(config.getMaxConnections()).thenReturn(10);
            bind(ConnectionConfigFactory.class).toInstance(connectionConfigFactory);

            bind(ObjectMapper.class).annotatedWith(Names.named("cts-json-object-mapper")).toInstance(new ObjectMapper());
        }

    }

}