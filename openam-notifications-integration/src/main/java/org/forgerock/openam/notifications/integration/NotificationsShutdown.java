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

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.util.Reject;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import com.sun.identity.setup.SetupListener;

/**
 * Responsible for telling the notification broker to shutdown.
 *
 * @since 14.0.0
 */
public final class NotificationsShutdown implements SetupListener {

    @Override
    public void setupComplete() {
        ShutdownManager shutdownManager = InjectorHolder.getInstance(ShutdownManager.class);
        final NotificationBroker broker = InjectorHolder.getInstance(NotificationBroker.class);

        Reject.ifNull(shutdownManager, "Shutdown manager must not be null");
        Reject.ifNull(broker, "Notification broker must not be null");

        shutdownManager.addShutdownListener(new ShutdownListener() {

            @Override
            public void shutdown() {
                broker.shutdown();
            }

        });
    }

}
