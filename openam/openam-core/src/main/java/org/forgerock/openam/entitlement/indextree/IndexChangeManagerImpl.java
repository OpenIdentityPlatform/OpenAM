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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.entitlement.indextree;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.entitlement.indextree.events.ErrorEventType;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeEvent;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObservable;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObserver;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This implementation delegates the responsibility of listening in for index changes to a monitor. The manager then
 * ensures the monitor is up and running and handles fail over by re-initiating the monitor. The monitor initiation is
 * done via a scheduler, so that it periodically attempts to start the monitor, until it's happily running. The creation
 * of the index change observable is delegated to the initiating framework, so that it can be shared between all
 * interested parties.
 *
 * @author andrew.forrest@forgerock.com
 */
public class IndexChangeManagerImpl implements IndexChangeManager, IndexChangeObserver {

    private static final Debug DEBUG = Debug.getInstance("amEntitlements");

    private static final int LOG_DURATION = 10000;
    private static final long INIT_DELAY = 0L;
    private static final long RETRY_DELAY = 1L;
    private static final TimeUnit DELAY_UNIT = TimeUnit.SECONDS;

    private final IndexChangeMonitor monitor;
    private final IndexChangeObservable observable;

    private final ScheduledExecutorService scheduler;
    private final MonitorTask monitorTask;
    private final TryAgainTask tryAgainTask;

    private volatile boolean shutdown;

    @Inject
    public IndexChangeManagerImpl(IndexChangeMonitor monitor, IndexChangeObservable observable) {
        this.monitor = monitor;
        this.observable = observable;

        // Associated tasks expect to be invoked by a single thread.
        scheduler = Executors.newScheduledThreadPool(1);
        monitorTask = new MonitorTask();
        tryAgainTask = new TryAgainTask();

        // Register to receive policy change events.
        observable.registerObserver(this);

        // Attempt to start the monitor.
        initiateMonitor();
    }

    /**
     * Initiates a task to immediately start the monitor.
     */
    private void initiateMonitor() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Initialising monitor to listen for policy path index modifications.");
        }

        scheduler.schedule(monitorTask, INIT_DELAY, DELAY_UNIT);
    }

    /**
     * {@inheritDoc}
     */
    public void update(IndexChangeEvent event) {
        if (event.getType() == ErrorEventType.SEARCH_FAILURE) {
            // Index change rescheduled, re-initiate the monitor.
            initiateMonitor();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerObserver(IndexChangeObserver observer) {
        // Delegate to the observable.
        observable.registerObserver(observer);
    }

    /**
     * {@inheritDoc}
     */
    public void removeObserver(IndexChangeObserver observer) {
        // Delegate to the observable.
        observable.removeObserver(observer);
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        shutdown = true;
        observable.removeObserver(this);
        scheduler.shutdown();
        monitor.shutdown();
    }

    // The monitor task is responsible for ensuring the monitor is
    // successfully started. Expected to be invoked by a single thread.
    private final class MonitorTask implements Runnable {

        private boolean firstRun;
        private boolean rescheduled;
        private long lastLog;

        public MonitorTask() {
            firstRun = true;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                if (rescheduled || shutdown) {
                    return;
                }

                if (!firstRun) {
                    // Ensure the monitor is cleaned up before re-initiating.
                    monitor.shutdown();
                }

                // Initiate the monitor.
                monitor.start();

                if (shutdown) {
                    monitor.shutdown();
                    return;
                }

                firstRun = false;
                lastLog = 0;

                // Notify observers of potential data loss whilst the search has been firstRun.
                observable.notifyObservers(ErrorEventType.DATA_LOSS.createEvent());

            } catch (ChangeMonitorException cmE) {
                rescheduled = true;

                // Failed to start monitor, reschedule to try again.
                scheduler.schedule(tryAgainTask, RETRY_DELAY, DELAY_UNIT);

                long now = System.currentTimeMillis();

                if (lastLog == 0 || now - lastLog > LOG_DURATION) {
                    // Log every 60 seconds.
                    DEBUG.error("Error attempting to initiate index change monitor.", cmE);
                    lastLog = now;
                }
            }
        }

        /**
         * Informs the monitor task that it's about to be rescheduled.
         */
        public void rescheduling() {
            rescheduled = false;
        }

    }

    // Try again task reschedules the monitor task.
    private final class TryAgainTask implements Runnable {

        /**
         * {@inheritDoc}
         */
        public void run() {
            // Delegate to the monitor task.
            monitorTask.rescheduling();
            monitorTask.run();
        }

    }

}
