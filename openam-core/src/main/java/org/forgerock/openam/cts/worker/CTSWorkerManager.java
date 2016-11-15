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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.worker;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.CoreTokenConfigListener;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.thread.ExecutorServiceFactory;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for starting and when required restarting the CTS Worker and coordinating the required
 * dependencies. This simplifies concerns around the CTS Workers.
 * <p>
 * Uses intrinsic lock to ensure that starting and reconfiguring worker tasks is thread safe,
 * (avoiding race conditions and ensuring memory visibility).
 */
public class CTSWorkerManager {

    private final Collection<CTSWorkerTask> workers;
    private final ThreadMonitor monitor;
    private final CoreTokenConfig config;
    private final ExecutorServiceFactory executorServiceFactory;
    private final Debug debug;
    private boolean started = false;
    private ScheduledExecutorService scheduledService;
    private int runPeriod;

    /**
     * Create a new default instance of {@link CTSWorkerManager}.
     * <p>
     * This factory method should be used to ensure that CTS workers are updated to reflect
     * any changes made to {@link CoreTokenConfig}.
     *
     * @param taskProvider Non null, required.
     * @param monitor Non null, required for thread monitoring.
     * @param config Non null, required for configuration.
     * @param executorServiceFactory Non null, required for scheduling.
     * @param debug Non null, required for debugging.
     */
    public static CTSWorkerManager newCTSWorkerInit(CTSWorkerTaskProvider taskProvider, ThreadMonitor monitor,
            CoreTokenConfig config, ExecutorServiceFactory executorServiceFactory,
            @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        final CTSWorkerManager manager = new CTSWorkerManager(taskProvider, monitor, config, executorServiceFactory, debug);
        manager.config.addListener(
                new CoreTokenConfigListener() {
                    @Override
                    public void configChanged() {
                        manager.configChanged();
                    }
                });
        return manager;
    }

    @VisibleForTesting
    CTSWorkerManager(CTSWorkerTaskProvider workerTaskProvider, ThreadMonitor monitor, CoreTokenConfig config,
            ExecutorServiceFactory executorServiceFactory, @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.workers = workerTaskProvider.getTasks();
        this.monitor = monitor;
        this.config = config;
        this.executorServiceFactory = executorServiceFactory;
        this.debug = debug;
    }

    /**
     * Initialise the {@link CTSWorkerTask} framework. All tasks are run by a single thread which is monitored by a
     * {@link ThreadMonitor}.
     *
     * @throws IllegalStateException If this function is called more than once.
     */
    public synchronized void startTasks() throws IllegalStateException {
        Reject.ifTrue(started);
        runPeriod = config.getRunPeriod();
        scheduledService = executorServiceFactory.createScheduledService(1);
        for (CTSWorkerTask worker : workers) {
            debug.message(CoreTokenConstants.DEBUG_HEADER + "Starting {}", worker);

            monitor.watchScheduledThread(
                    scheduledService,
                    worker,
                    runPeriod,
                    runPeriod,
                    TimeUnit.MILLISECONDS);

            debug.message(CoreTokenConstants.DEBUG_HEADER + "Started {}", worker);
        }
        started = true;
    }

    /**
     * Shuts down the CTS worker scheduled service.
     *
     * @throws IllegalStateException If this function is called more than once.
     */
    private synchronized void stopTasks() throws IllegalStateException {
        Reject.ifFalse(started);
        debug.message(CoreTokenConstants.DEBUG_HEADER + "Shutting down CTS worker scheduled service");
        scheduledService.shutdownNow();
        started = false;
    }

    @VisibleForTesting
    synchronized void configChanged() {
        int newRunPeriod = config.getRunPeriod();
        if (runPeriod != newRunPeriod) {
            stopTasks();
            startTasks();
        }
    }
}
