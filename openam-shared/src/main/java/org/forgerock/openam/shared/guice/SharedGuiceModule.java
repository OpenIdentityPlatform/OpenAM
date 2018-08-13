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
package org.forgerock.openam.shared.guice;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.http.Client;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.audit.context.AuditRequestContextPropagatingExecutorServiceFactory;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.shared.security.crypto.KeyPairProviderFactory;
import org.forgerock.openam.shared.security.crypto.KeyPairProviderFactoryImpl;
import org.forgerock.util.thread.listener.ShutdownManager;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;

/**
 * Guice module for OpenAM shared bindings.
 */
@GuiceModule
public class SharedGuiceModule extends AbstractModule {

    /**
     * The Debug instance annotation name for the thread manager.
     */
    public static final String DEBUG_THREAD_MANAGER = "amThreadManager";

    @Override
    protected void configure() {
        bind(Debug.class)
                .annotatedWith(Names.named(DEBUG_THREAD_MANAGER))
                .toInstance(Debug.getInstance(DEBUG_THREAD_MANAGER));
        bind(ShutdownManager.class).toInstance(com.sun.identity.common.ShutdownManager.getInstance());
        bind(KeyPairProviderFactory.class).to(KeyPairProviderFactoryImpl.class);
        bind(Client.class).toProvider(CloseableHttpClientProvider.class).in(Scopes.SINGLETON);
    }

    @Provides @Inject
    AMExecutorServiceFactory provideAMExecutorServiceFactory(ShutdownManager manager) {
        return new AuditRequestContextPropagatingExecutorServiceFactory(manager);
    }

    @Provides @Inject @Singleton
    ThreadMonitor provideThreadMonitor(AMExecutorServiceFactory factory,
            ShutdownManager wrapper, @Named(DEBUG_THREAD_MANAGER) Debug debug) {
        return new ThreadMonitor(factory.createCachedThreadPool(DEBUG_THREAD_MANAGER), wrapper, debug);
    }
}
