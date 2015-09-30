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
package org.forgerock.openam.audit;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfigurationWrapper;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationListener;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * @since 13.0.0
 */
public class AuditServiceProviderImplTest {

    private AuditServiceProvider provider;
    private AuditServiceConfigurationProvider configurator;
    private AuditServiceConfigurationListener configListener;
    private AuditEventHandlerConfigurationWrapper handlerConfig;

    @Mock
    private AMAuditServiceConfiguration mockServiceConfig;
    @Mock
    private AuditEventHandlerFactory mockHandlerFactory;
    @Mock
    private AuditEventHandler mockHandler;
    @Mock
    private ShutdownManager mockShutdownManager;

    @BeforeMethod
    protected void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        handlerConfig = new AuditEventHandlerConfigurationWrapper(mock(EventHandlerConfiguration.class),
                        AuditConstants.EventHandlerType.CSV, "handler", singleton("access"));

        configurator = new MockAuditServiceConfigurationProvider();
        provider = new AuditServiceProviderImpl(configurator, mockHandlerFactory, mockShutdownManager);

        when(mockHandlerFactory.create(any(AuditEventHandlerConfigurationWrapper.class))).thenReturn(mockHandler);
    }

    @Test
    public void shouldCreateDefaultAuditService() throws Exception {
        // Given

        // When

        // Then
        assertThat(provider.getDefaultAuditService()).isNotNull();
    }

    @Test
    public void shouldCreateRealmAuditService() throws Exception {
        // Given

        // When
        configListener.realmConfigurationChanged("anyRealm");

        // Then
        assertThat(provider.getAuditService("anyRealm")).isNotNull();
    }

    @Test
    public void shouldRemoveRealmAuditServiceAndFallBackToDefault() throws Exception {
        // Given

        // When
        configListener.realmConfigurationChanged("anyRealm");
        configListener.realmConfigurationRemoved("anyRealm");

        // Then
        assertThat(provider.getAuditService("anyRealm")).isNotNull();
        assertThat(provider.getAuditService("anyRealm")).isEqualTo(provider.getDefaultAuditService());
    }

    class MockAuditServiceConfigurationProvider implements AuditServiceConfigurationProvider {

        @Override
        public void setupComplete() {
        }

        @Override
        public void addConfigurationListener(AuditServiceConfigurationListener listener) {
            configListener = listener;
        }

        @Override
        public void removeConfigurationListener(AuditServiceConfigurationListener listener) {
        }

        @Override
        public AMAuditServiceConfiguration getDefaultConfiguration() {
            return mockServiceConfig;
        }

        @Override
        public AMAuditServiceConfiguration getRealmConfiguration(String realm) {
            return mockServiceConfig;
        }

        @Override
        public Set<AuditEventHandlerConfigurationWrapper> getDefaultEventHandlerConfigurations() {
            return singleton(handlerConfig);
        }

        @Override
        public Set<AuditEventHandlerConfigurationWrapper> getRealmEventHandlerConfigurations(String realm) {
            return singleton(handlerConfig);
        }
    }
}

