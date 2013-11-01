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
import java.util.concurrent.ScheduledFuture;
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
public class IndexChangeManagerImpl implements IndexChangeManager, Runnable, IndexChangeObserver {

    private static final Debug DEBUG = Debug.getInstance("amEntitlements");

    private final IndexChangeMonitor monitor;
    private final IndexChangeObservable observable;

    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> schedulerStatus;

    private long lastLog;

    @Inject
    public IndexChangeManagerImpl(IndexChangeMonitor monitor, IndexChangeObservable observable) {
        this.monitor = monitor;
        this.observable = observable;

        scheduler = Executors.newScheduledThreadPool(1);
        // Register to receive policy change events.
        observable.registerObserver(this);
    }

    @Override
    public  void init() {
        if (schedulerStatus == null || schedulerStatus.isDone()) {

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Initialising monitor to listen for policy path index modifications.");
            }

            // Kick of the scheduler to attempt to initiate the listener every second.
            schedulerStatus = scheduler.scheduleWithFixedDelay(this, 0L, 1000L, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void run() {
        try {
            // Before starting the monitor, ensure it is not currently running.
            monitor.shutdown();

            // Initiate the monitor.
            monitor.start();

            // Listener has been established, no more attempts necessary.
            schedulerStatus.cancel(false);

            // Notify observers of potential data loss whilst the search has been initiated.
            observable.notifyObservers(ErrorEventType.DATA_LOSS.createEvent());

            lastLog = 0;

        } catch (ChangeMonitorException sfE) {
            long now = System.currentTimeMillis();

            if (lastLog == 0 || now - lastLog > 60000) {
                // Log every 60 seconds.
                DEBUG.error("Error attempting to initiate index change monitor.", sfE);
                lastLog = now;
            }
        }
    }

    @Override
    public void update(IndexChangeEvent event) {
        if (event.getType() == ErrorEventType.SEARCH_FAILURE) {
            // Index change failure, re-initiate the monitor.
            init();
        }
    }

    @Override
    public void registerObserver(IndexChangeObserver observer) {
        // Delegate to the observable.
        observable.registerObserver(observer);
    }

    @Override
    public void removeObserver(IndexChangeObserver observer) {
        // Delegate to the observable.
        observable.removeObserver(observer);
    }

    @Override
    public void shutdown() {
        observable.removeObserver(this);

        // Cleanup any outstanding scheduled task.
        if (schedulerStatus != null) {
            schedulerStatus.cancel(true);
        }

        scheduler.shutdown();
        monitor.shutdown();
    }

}
