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

package org.forgerock.openam.selfservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.selfservice.config.SelfServiceConsoleConfig;
import org.forgerock.openam.selfservice.config.ServiceConfigProvider;
import org.forgerock.openam.selfservice.config.ServiceConfigProviderFactory;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.services.context.Context;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link SelfServiceRequestHandler}.
 *
 * @since 13.0.0
 */
public final class SelfServiceRequestHandlerTest {

    @Mock
    private SelfServiceFactory serviceFactory;
    @Mock
    private ConsoleConfigHandler consoleConfigHandler;
    @Mock
    private ServiceConfigProviderFactory providerFactory;
    @Mock
    private ServiceConfigProvider<SelfServiceConsoleConfig> configProvider;
    @Mock
    private SelfServiceConsoleConfig consoleConfig;
    @Mock
    private RequestHandler underlyingService;

    private RealmTestHelper realmTestHelper;
    private Context context;

    private SelfServiceRequestHandler selfServiceHandler;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        context = new RealmContext(new HttpContext(json(object(field("headers", Collections.emptyMap()),
                field("parameters", Collections.emptyMap()))), null), Realm.root());

        selfServiceHandler = new SelfServiceRequestHandler<>(MockBuilder.class,
                consoleConfigHandler, providerFactory, serviceFactory);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void initialReadCallsIntoUnderlyingService() throws ResourceException {
        // When
        ReadRequest request = Requests.newReadRequest("/someEndpoint");

        given(consoleConfigHandler.getConfig("/", MockBuilder.class)).willReturn(consoleConfig);
        given(providerFactory.getProvider(consoleConfig)).willReturn(configProvider);
        given(configProvider.isServiceEnabled(consoleConfig)).willReturn(true);
        ProcessInstanceConfig config = new ProcessInstanceConfig();
        given(configProvider.getServiceConfig(consoleConfig, context, "/")).willReturn(config);
        given(serviceFactory.getService("/", config)).willReturn(underlyingService);

        // Given
        selfServiceHandler.handleRead(context, request);

        // Then
        verify(underlyingService).handleRead(context, request);
    }

    @Test
    public void initialActionCallsIntoUnderlyingService() throws ResourceException {
        // When
        ActionRequest request = Requests.newActionRequest("/someEndpoint", "submitRequirements");

        given(consoleConfigHandler.getConfig("/", MockBuilder.class)).willReturn(consoleConfig);
        given(providerFactory.getProvider(consoleConfig)).willReturn(configProvider);
        given(configProvider.isServiceEnabled(consoleConfig)).willReturn(true);
        ProcessInstanceConfig config = new ProcessInstanceConfig();
        given(configProvider.getServiceConfig(consoleConfig, context, "/")).willReturn(config);
        given(serviceFactory.getService("/", config)).willReturn(underlyingService);

        // Given
        selfServiceHandler.handleAction(context, request);

        // Then
        verify(underlyingService).handleAction(context, request);
    }

    @Test
    public void shouldProducePerBuilderApiDescriptor() throws Exception {
        // Given
        ApiDescription descriptor = selfServiceHandler.api(null);

        // Then
        Resource resource = descriptor.getPaths().get("").get(VersionedPath.UNVERSIONED);
        assertThat(resource.getResourceSchema().getSchema().get("title").getObject().toString())
                .startsWith("i18n:api-descriptor/SelfServiceRequestHandler_MockBuilder#");
    }

    private static final class MockBuilder implements ConsoleConfigBuilder<SelfServiceConsoleConfig> {

        @Override
        public SelfServiceConsoleConfig build(Map<String, Set<String>> attributes) {
            return null;
        }

    }

}