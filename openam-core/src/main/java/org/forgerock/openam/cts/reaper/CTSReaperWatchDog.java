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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.CoreTokenConstants;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for ensuring that the CTS Reaper continues to operate,
 * even under conditions where exceptions are being thrown. The principle of
 * this class depends on the implementation of the ScheduledExecutorService.
 *
 * This code is a little unintuitive, because of the concept that a fixed
 * running task has no logical end point.
 *
 * When the scheduled task fails to execute by throwing an exception the
 * ScheduledExecutorService will re-throw the exception as an
 * ExecutionException for the future. This is what is being monitored by
 * this code, and used as the signal to log the error and re-schedule the
 * CTS Reaper.
 *
 * @see java.util.concurrent.ScheduledFuture#get()
 * @see <a href="http://stackoverflow.com/questions/6894595/scheduledexecutorservice-exception-handling">Stack Overflow</a>
 * @see <a href="http://www.cosmocode.de/en/blog/schoenborn/2009-12/17-uncaught-exceptions-in-scheduled-tasks">CosmoCode</a>
 */
public class CTSReaperWatchDog {
    private final CTSReaper reaper;
    private final ReaperShutdownMonitor monitor;
    private final ScheduledExecutorService service;
    private final ExecutorService workerPool;
    private final Debug debug;
    private final CoreTokenConfig config;

    /**
     * Create a new instance of the CTSReaperWatchDog with all required dependencies.
     *
     * @param reaper Required as the instance to reschedule.
     * @param config Required for determining reschedule rate.
     * @param monitor Required to monitor System Shutdown.
     * @param scheduledService Required for scheduling.
     * @param workerPool Required for running the WatchDog task which monitors the reaper.
     * @param debug Required for debugging.
     */
    @Inject
    public CTSReaperWatchDog(CTSReaper reaper, CoreTokenConfig config,
                             ReaperShutdownMonitor monitor,
                             @Named(CoreTokenConstants.CTS_SCHEDULED_SERVICE) ScheduledExecutorService scheduledService,
                             @Named(CoreTokenConstants.CTS_WORKER_POOL) ExecutorService workerPool,
                             @Named(CoreTokenConstants.CTS_REAPER_DEBUG) Debug debug) {
        this.reaper = reaper;
        this.monitor = monitor;
        this.service = scheduledService;
        this.workerPool = workerPool;
        this.debug = debug;
        this.config = config;
    }

    /**
     * Start the CTS Reaper by executing the WatchDog.
     *
     * The WatchDog will start and monitor the scheduled task until such time as the task
     * fails, or the system is shutdown.
     */
    public void startReaper() {
        if (monitor.isShutdown()) {
            log("Not scheduling the Watch Dog, shutdown in progress.");
            return;
        }

        log("Scheduling the CTS Reaper every " + config.getRunPeriod() + "ms");
        workerPool.submit(new WatchDog());
    }

    /**
     * WatchDog will monitor a Scheduled runnable. Scheduled Runnable tasks do not
     * return from the Future#get call unless they fail. In this case, the
     * WatchDog will reschedule the task.
     */
    private class WatchDog implements Runnable {
        public void run() {
            if (monitor.isShutdown()) {
                log("Not scheduling the CTS Reaper, shutdown in progress");
                return;
            }

            final ScheduledFuture<?> future = service.scheduleAtFixedRate(
                    reaper,
                    config.getRunPeriod(),
                    config.getRunPeriod(),
                    TimeUnit.MILLISECONDS);

            // Ensure the future is registered with the monitor for shutdown detection
            monitor.setFuture(future);

            // This call will block as long as the scheduled task continues to run
            // ok. If it fails for any reason, the future.get call will throw an
            // ExecutionException.
            try {
                future.get();
            } catch (InterruptedException e) {
                err("Interrupted whilst watching the CTS Reaper future", e);
            } catch (ExecutionException e) {
                err("Error on the CTS Reaper, restarting", e);
            }
            log("WatchDog complete");

            // Regardless of out come, restart the reaper
            startReaper();
        }
    }

    // Controls formatting for logging statements.
    private void log(String msg) {
        debug.message(header() + msg);
    }

    // Controls formatting for error statements.
    private void err(String msg, Throwable t) {
        debug.error(header() + msg, t);
    }

    private static String header() {
        return CoreTokenConstants.DEBUG_HEADER + "Watchdog: ";
    }
}
