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
package org.forgerock.openam.cts.reaper;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for starting the CTS Reaper and coordinating the required
 * dependencies. This simplifies concerns around the CTS Reaper.
 */
@Singleton
public class CTSReaperInit {
    private final CTSReaper reaper;
    private final ThreadMonitor monitor;
    private final CoreTokenConfig config;
    private final ScheduledExecutorService scheduledService;
    private final Debug debug;
    private boolean started = false;

    /**
     * Default Guice provided instance.
     *
     * @param reaper Non null, required.
     * @param monitor Non null, required for thread monitoring.
     * @param config Non null, required for configuration.
     * @param scheduledService Non null, required for scheduling.
     * @param debug Non null, required for debugging.
     */
    @Inject
    public CTSReaperInit(CTSReaper reaper,
                         ThreadMonitor monitor,
                         CoreTokenConfig config,
                         @Named(CoreTokenConstants.CTS_SCHEDULED_SERVICE) ScheduledExecutorService scheduledService,
                         @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.reaper = reaper;
        this.monitor = monitor;
        this.config = config;
        this.scheduledService = scheduledService;
        this.debug = debug;
    }

    /**
     * Starts the CTS Reaper. Once started the CTS Reaper will be monitored by
     * the ThreadMonitor.
     *
     * @throws IllegalStateException If this function is called more than once.
     */
    public synchronized void startReaper() throws IllegalStateException {
        Reject.ifTrue(started);

        if (debug.messageEnabled()) {
            debug.message(CoreTokenConstants.DEBUG_HEADER + "Starting the CTS Reaper");
        }
        // Start the CTS Reaper watch dog which will monitor the CTS Reaper
        monitor.watchScheduledThread(
                scheduledService,
                reaper,
                config.getRunPeriod(),
                config.getRunPeriod(),
                TimeUnit.MILLISECONDS);

        started = true;
    }
}
