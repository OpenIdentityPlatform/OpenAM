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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.notifications.integration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.notifications.LocalOnly;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.brokers.InMemoryNotificationBroker;
import org.forgerock.openam.notifications.integration.brokers.CTSNotificationBroker;
import org.forgerock.util.thread.ExecutorServiceFactory;

import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;

/**
 * Guice bindings for notifications.
 *
 * @since 14.0.0
 */
@GuiceModule
public class NotificationsGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(NotificationBroker.class)
                .annotatedWith(LocalOnly.class)
                .to(InMemoryNotificationBroker.class)
                .in(Singleton.class);
        bindConstant().annotatedWith(Names.named("queueSize"))
                .to(SystemProperties.getAsInt("org.forgerock.openam.notifications.local.queueSize", 10000));
        bindConstant().annotatedWith(Names.named("consumers"))
                .to(SystemProperties.getAsInt("org.forgerock.openam.notifications.local.consumers", 4));
        bindConstant().annotatedWith(Names.named("tokenExpirySeconds"))
                .to(SystemProperties.getAsLong("org.forgerock.openam.notifications.cts.tokenExpirySeconds", 600L));
        bindConstant().annotatedWith(Names.named("publishFrequencyMilliseconds"))
                .to(SystemProperties.getAsLong("org.forgerock.openam.notifications.cts.publishFrequencyMilliseconds",
                        100L));
        bindConstant().annotatedWith(Names.named("ctsQueueSize"))
                .to(SystemProperties.getAsInt("org.forgerock.openam.notifications.cts.queueSize", 10000));

        expose(NotificationBroker.class).annotatedWith(LocalOnly.class);
        expose(NotificationBroker.class);
    }

    @Provides
    @Inject
    @Exposed
    @Singleton
    @Named("webSocketScheduledExecutorService")
    ScheduledExecutorService scheduledExecutorService(AMExecutorServiceFactory factory) {
        return factory.createScheduledService(5, "NotificationsWebSocket");
    }

    @Provides
    @Exposed
    @Inject
    @Singleton
    NotificationBroker notificationBroker(CTSPersistentStore store,
            @LocalOnly NotificationBroker broker,
            @Named("ctsQueueSize") int queueSize,
            @Named("tokenExpirySeconds") long tokenExpirySeconds,
            @Named("publishFrequencyMilliseconds") long publishFrequencyMilliseconds,
            AMExecutorServiceFactory factory) {
        return new CTSNotificationBroker(store, broker, queueSize, tokenExpirySeconds,
                publishFrequencyMilliseconds, factory);
    }

}
