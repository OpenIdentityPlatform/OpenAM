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

import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.services.context.Context;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ServiceConfigProviderFactoryImpl}.
 *
 * @since 13.0.0
 */
public final class ServiceConfigProviderFactoryImplTest {

    private ServiceConfigProviderFactory providerFactory;

    @BeforeMethod
    public void setUp() {
        providerFactory = new ServiceConfigProviderFactoryImpl();
    }

    @Test
    public void retrievesValidProviderInstance() {
        // Given
        MockConfig config = new MockConfig();

        // When
        ServiceConfigProvider<MockConfig> provider = providerFactory.getProvider(config);

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(MockProvider.class);
    }

    @Test
    public void retrievesSameInstance() {
        // Given
        MockConfig config = new MockConfig();

        // When
        ServiceConfigProvider<MockConfig> providerA = providerFactory.getProvider(config);
        ServiceConfigProvider<MockConfig> providerB = providerFactory.getProvider(config);

        // Then
        assertThat(providerA).isNotNull();
        assertThat(providerB).isNotNull();
        assertThat(providerA).isEqualTo(providerB);
    }

    static final class MockConfig implements ConsoleConfig {

        @Override
        public String getConfigProviderClass() {
            return MockProvider.class.getName();
        }

    }

    static final class MockProvider implements ServiceConfigProvider<MockConfig> {

        @Override
        public boolean isServiceEnabled(MockConfig config) {
            return false;
        }

        @Override
        public ProcessInstanceConfig getServiceConfig(MockConfig config, Context context, String realm) {
            return null;
        }

    }

}