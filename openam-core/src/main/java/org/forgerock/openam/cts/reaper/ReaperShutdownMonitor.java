/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.cts.reaper;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.guice.CoreGuiceModule;
import org.forgerock.openam.cts.api.CoreTokenConstants;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.Future;

/**
 * Responsible for monitoring the state of System Shutdown.
 *
 * When provided with a Future, will cancel the future during
 * shutdown, and indicate that shutdown has occurred.
 *
 * @see CTSReaper
 * @see CTSReaperWatchDog
 *
 * @author robert.wapshott@forgerock.com
 */
public class ReaperShutdownMonitor implements ShutdownListener {
    private final Debug debug;
    private boolean shutdown = false;
    private Future future = null;

    @Inject
    public ReaperShutdownMonitor(CoreGuiceModule.ShutdownManagerWrapper wrapper,
                                 @Named(CoreTokenConstants.CTS_REAPER_DEBUG) Debug debug) {
        this.debug = debug;
        wrapper.addShutdownListener(this);
    }

    /**
     * If a shutdown is detected, the assigned future will be cancelled,
     *
     * @param future Future to cancel during Shutdown.
     */
    public synchronized void setFuture(Future future) {
        this.future = future;
    }

    /**
     * @return A possibly null Future.
     */
    private synchronized Future getFuture() {
        return future;
    }

    /**
     * Mark the class as shutdown.
     */
    private synchronized void setShutdown() {
        shutdown = true;
    }

    /**
     * @return True if the ShutdownManager has signalled that shutdown has been started.
     */
    public synchronized boolean isShutdown() {
        return shutdown;
    }

    /**
     * Implements the shutdown logic which consists of cancelling the assigned
     * future and signalling that shutdown has been started.
     */
    public void shutdown() {
        log("Shutdown detected");
        // Mark the monitor as shutdown
        setShutdown();

        Future f = getFuture();
        if (f == null || f.isDone()) {
            return;
        }

        log("Cancelling future");
        f.cancel(true);
    }

    private void log(String msg) {
        debug.message(CoreTokenConstants.DEBUG_HEADER + msg);
    }
}
