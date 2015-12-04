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

import com.google.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory delivers up service providers based of the passed console configuration
 * by using reflection to instantiate an instance based of the provider class name.
 *
 * @since 13.0.0
 */
@Singleton
public final class ServiceConfigProviderFactoryImpl implements ServiceConfigProviderFactory {

    private final ConcurrentMap<String, ServiceConfigProvider<?>> providers;
    private final Injector injector;

    /**
     * Constructs a new service provider factory instance.
     *
     * @param injector
     *         dependency injector
     */
    @Inject
    public ServiceConfigProviderFactoryImpl(Injector injector) {
        providers = new ConcurrentHashMap<>();
        this.injector = injector;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends SelfServiceConsoleConfig> ServiceConfigProvider<C> getProvider(C config) {
        String providerClassName = config.getConfigProviderClass();
        ServiceConfigProvider<?> provider = providers.get(providerClassName);

        if (provider == null) {
            provider = constructNewProvider(providerClassName);
            ServiceConfigProvider<?> old = providers.putIfAbsent(providerClassName, provider);

            if (old != null) {
                provider = old;
            }
        }

        return (ServiceConfigProvider<C>) provider;
    }

    private ServiceConfigProvider<?> constructNewProvider(String className) {
        try {
            Class<? extends ServiceConfigProvider> providerClass = Class
                    .forName(className)
                    .asSubclass(ServiceConfigProvider.class);

            return injector.getInstance(providerClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown class name " + className + " for provider", e);
        }
    }
}
