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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.utils;

import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import javax.inject.Inject;

/**
 * Responsible for monitoring the system Shutdown state and providing this signal to callers
 * that need to poll the shutdown state.
 *
 * For example threads which can be interrupted have to detect spurious interrupts and ignore
 * them. This class assists those cases.
 */
public class ShutdownMonitor {
    private boolean shutdown = false;

    /**
     * Guice initialised constructor.
     * @param shutdownManager Required.
     */
    @Inject
    public ShutdownMonitor(ShutdownManager shutdownManager) {
        shutdownManager.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdown() {
                    setShutdown();
                }
            });
    }

    /**
     * Assign shutdown status.
     */
    private synchronized void setShutdown() {
        shutdown = true;
    }

    /**
     * @return True if the system shutdown has been signalled.
     */
    public synchronized boolean hasShutdown() {
        return shutdown;
    }
}
