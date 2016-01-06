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
package org.forgerock.openam.audit;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.google.inject.Binder;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationListener;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 13.0.0
 */
public class AuditServiceProviderImplTest extends GuiceTestCase {

    private AuditServiceProvider provider;
    private AuditServiceConfigurationListener configListener;
    private Set<AuditEventHandlerConfiguration> handlerConfigs = new HashSet<>();
    private AMAuditServiceConfiguration auditServiceConfig;

    @Mock
    private ShutdownManager mockShutdownManager;

    @BeforeMethod
    protected void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Map<String, Set<String>> configAttributes = new HashMap<>();
        configAttributes.put("handlerFactory", singleton(MockAuditEventHandlerFactory.class.getName()));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("enabled", singleton("true"));
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder()
                .withName("Mock Handler")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(EventTopicsMetaDataBuilder.coreTopicSchemas().build()).build();
        handlerConfigs.add(configuration);

        configAttributes = new HashMap<>();
        configAttributes.put("handlerFactory", singleton("no.such.class"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("enabled", singleton("true"));
        configuration = AuditEventHandlerConfiguration.builder()
                .withName("No such handler")
                .withAttributes(configAttributes)
                .withEventTopicsMetaData(EventTopicsMetaDataBuilder.coreTopicSchemas().build()).build();
        handlerConfigs.add(configuration);

        auditServiceConfig = new AMAuditServiceConfiguration(true);
        provider = new AuditServiceProviderImpl(new MockAuditServiceConfigurationProvider(), mockShutdownManager);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(MockAuditEventHandlerFactory.class).toInstance(new MockAuditEventHandlerFactory());
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
        assertThat(provider.getAuditService("anyRealm")).isInstanceOf(RealmAuditServiceProxy.class);
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

    @Test
    public void shouldInstantiateHandlerFactoryAndCreateMockHandler() throws Exception {
        // Given

        // When
        configListener.globalConfigurationChanged();

        // Then
        assertThat(provider.getDefaultAuditService().getRegisteredHandler("Mock Handler")).isNotNull();
    }

    @Test
    public void shouldSilentlyFailToInstantiateHandlerFactory() throws Exception {
        // Given

        // When
        configListener.globalConfigurationChanged();

        // Then
        assertThat(provider.getDefaultAuditService().getRegisteredHandler("No such handler")).isNull();
    }

    @Test
    public void shouldNotRegisterHandlersWhenDefaultAuditServiceDisabled() throws Exception {
        // Given
        auditServiceConfig = new AMAuditServiceConfiguration(false);

        // When
        configListener.globalConfigurationChanged();

        // Then
        assertThat(provider.getDefaultAuditService().getRegisteredHandler("Mock Handler")).isNull();
    }

    @Test
    public void shouldNotRegisterHandlersWhenRealmAuditServiceDisabled() throws Exception {
        // Given
        auditServiceConfig = new AMAuditServiceConfiguration(false);

        // When
        configListener.realmConfigurationChanged("anyRealm");

        // Then
        assertThat(provider.getAuditService("anyRealm").getRegisteredHandler("Mock Handler")).isNull();
    }

    @Test
    public void shouldNotRegisterHandlersWhenFactoryReturnsNull() throws Exception {
        // Given
        for (AuditEventHandlerConfiguration config : handlerConfigs) {
            config.getAttributes().put("enabled", singleton("false"));
        }

        // When
        configListener.globalConfigurationChanged();

        // Then
        assertThat(provider.getDefaultAuditService().getRegisteredHandler("Mock Handler")).isNull();
        assertThat(provider.getDefaultAuditService().getRegisteredHandler("No such handler")).isNull();
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
            return auditServiceConfig;
        }

        @Override
        public AMAuditServiceConfiguration getRealmConfiguration(String realm) {
            return auditServiceConfig;
        }

        @Override
        public Set<AuditEventHandlerConfiguration> getDefaultEventHandlerConfigurations() {
            return handlerConfigs;
        }

        @Override
        public Set<AuditEventHandlerConfiguration> getRealmEventHandlerConfigurations(String realm) {
            return handlerConfigs;
        }

        @Override
        public EventTopicsMetaData getEventTopicsMetaData() {
            return EventTopicsMetaDataBuilder.coreTopicSchemas().build();
        }
    }

    class MockAuditEventHandlerFactory implements AuditEventHandlerFactory {

        @Override
        public AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException {
            if ("false".equals(configuration.getAttributes().get("enabled").iterator().next())) {
                return null;
            }
            AuditEventHandler mockHandler = mock(AuditEventHandler.class);
            when(mockHandler.getName()).thenReturn(configuration.getHandlerName());
            return mockHandler;
        }
    }
}

