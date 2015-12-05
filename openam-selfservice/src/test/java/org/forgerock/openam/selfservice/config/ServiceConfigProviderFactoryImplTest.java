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
    private Injector injector;

    @BeforeMethod
    public void setUp() {
        injector = mock(Injector.class);
        providerFactory = new ServiceConfigProviderFactoryImpl(injector);
    }

    @Test
    public void retrievesValidProviderInstance() {
        // Given
        SelfServiceConsoleConfig config = mock(SelfServiceConsoleConfig.class);
        given(config.getConfigProviderClass()).willReturn(MockConfigProvider.class.getName());
        MockConfigProvider provider = new MockConfigProvider();
        given(injector.getInstance(MockConfigProvider.class)).willReturn(provider);

        // When
        ServiceConfigProvider<SelfServiceConsoleConfig> providerResult = providerFactory.getProvider(config);

        // Then
        assertThat(providerResult).isEqualTo(provider);
    }

    @Test
    public void retrievesSameInstance() {
        // Given
        SelfServiceConsoleConfig config = mock(SelfServiceConsoleConfig.class);
        given(config.getConfigProviderClass()).willReturn(MockConfigProvider.class.getName());
        MockConfigProvider provider = new MockConfigProvider();
        given(injector.getInstance(MockConfigProvider.class)).willReturn(provider);

        // When
        ServiceConfigProvider<SelfServiceConsoleConfig> providerResultA = providerFactory.getProvider(config);
        ServiceConfigProvider<SelfServiceConsoleConfig> providerResultB = providerFactory.getProvider(config);

        // Then
        assertThat(providerResultA).isEqualTo(provider);
        assertThat(providerResultA).isEqualTo(providerResultB);
    }

    static final class MockConfigProvider implements ServiceConfigProvider<SelfServiceConsoleConfig> {

        @Override
        public boolean isServiceEnabled(SelfServiceConsoleConfig config) {
            return false;
        }

        @Override
        public ProcessInstanceConfig getServiceConfig(SelfServiceConsoleConfig config, Context context, String realm) {
            return null;
        }

    }

}