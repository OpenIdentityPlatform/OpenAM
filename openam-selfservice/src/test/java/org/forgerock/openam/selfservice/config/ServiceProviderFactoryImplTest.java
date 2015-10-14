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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.inject.Injector;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.services.context.Context;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ServiceProviderFactoryImpl}.
 *
 * @since 13.0.0
 */
public final class ServiceProviderFactoryImplTest {

    private ServiceProviderFactory providerFactory;
    private Injector injector;

    @BeforeMethod
    public void setUp() {
        injector = mock(Injector.class);
        providerFactory = new ServiceProviderFactoryImpl(injector);
    }

    @Test
    public void retrievesValidProviderInstance() {
        // Given
        MockConfig config = new MockConfig();
        MockProvider provider = new MockProvider();
        given(injector.getInstance(MockProvider.class)).willReturn(provider);

        // When
        ServiceProvider<MockConfig> providerResult = providerFactory.getProvider(config);

        // Then
        assertThat(providerResult).isEqualTo(provider);
    }

    @Test
    public void retrievesSameInstance() {
        // Given
        MockConfig config = new MockConfig();
        MockProvider provider = new MockProvider();
        given(injector.getInstance(MockProvider.class)).willReturn(provider);

        // When
        ServiceProvider<MockConfig> providerResultA = providerFactory.getProvider(config);
        ServiceProvider<MockConfig> providerResultB = providerFactory.getProvider(config);

        // Then
        assertThat(providerResultA).isEqualTo(provider);
        assertThat(providerResultA).isEqualTo(providerResultB);
    }

    static final class MockConfig implements ConsoleConfig {

        @Override
        public String getConfigProviderClass() {
            return MockProvider.class.getName();
        }

    }

    static final class MockProvider implements ServiceProvider<MockConfig> {

        @Override
        public boolean isServiceEnabled(MockConfig config) {
            return false;
        }

        @Override
        public RequestHandler getService(MockConfig config, Context context, String realm) {
            return null;
        }

    }

}