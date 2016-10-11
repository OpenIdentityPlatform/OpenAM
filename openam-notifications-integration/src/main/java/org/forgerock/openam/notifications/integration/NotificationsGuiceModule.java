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
 */

package org.forgerock.openam.notifications.integration;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.brokers.SingleQueueNotificationBroker;
import org.forgerock.util.thread.ExecutorServiceFactory;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * Guice bindings for notifications.
 *
 * @since 14.0.0
 */
@GuiceModule
public class NotificationsGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(NotificationBroker.class).to(SingleQueueNotificationBroker.class).in(Singleton.class);
        bindConstant().annotatedWith(Names.named("queueTimeout")).to(500L);
        bindConstant().annotatedWith(Names.named("queueSize")).to(10000);

        expose(NotificationBroker.class);
    }

    @Provides
    @Inject
    ExecutorService executorService(ExecutorServiceFactory factory) {
        return factory.createFixedThreadPool(1);
    }

}
