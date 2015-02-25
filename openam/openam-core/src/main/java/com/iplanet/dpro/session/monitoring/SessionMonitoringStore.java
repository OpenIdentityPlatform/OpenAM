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
package com.iplanet.dpro.session.monitoring;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The store for session monitoring information. Each of the operations which can be applied to sessions
 * (refresh, setProperty, destroy and logout) have their own store, which maps the session type (e.g. REMOTE or LOCAL)
 * to the {@link SessionMonitoringTimingStore}. This timing store contains a (configurable) number of the most-recent
 * {@link SessionTimingEntry} samples, which can be used to calculate e.g. the slowest, or average speed of a given
 * operation.
 *
 * Uses an ExecutorService to perform writes out to the store (which may block).
 */
@Singleton
public class SessionMonitoringStore {

    private final ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> refreshStore;
    private final ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> propertyStore;
    private final ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> destroyStore;
    private final ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> logoutStore;

    private final SessionMonitoringTimingStoreFactory sessionMonitoringTimingStoreFactory;

    //for pushing off our monitoring writes to another thread
    private final ExecutorService executorService;
    public static final String EXECUTOR_BINDING_NAME = "SESSION_MONITORING_EXECUTOR";

    /**
     * Guice-powered constructor, setting ourselves up with an executor service (which we will use to offload
     * our monitoring writes). We generate a new synchronized map at construction time ready for monitoring data
     * to be pushed in.
     *
     * @param executorService the service to which to offload out writes
     */
    @Inject
    public SessionMonitoringStore(@Named(EXECUTOR_BINDING_NAME) ExecutorService executorService,
                                  SessionMonitoringTimingStoreFactory sessionMonitoringTimingStoreFactory) {
        this.executorService = executorService;
        this.sessionMonitoringTimingStoreFactory = sessionMonitoringTimingStoreFactory;

        this.refreshStore = new ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore>();
        this.propertyStore= new ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore>();
        this.destroyStore= new ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore>();
        this.logoutStore= new ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore>();
    }

    /**
     * For testing.
     *
     * @param executorService Service to perform writes with
     * @param sessionMonitoringTimingStoreFactory Factory for generating timing stores
     * @param refreshStore Refresh store
     * @param propertyStore Property store
     * @param destroyStore Destroy store
     * @param logoutStore Logout store
     */
    SessionMonitoringStore(ExecutorService executorService,
                                  SessionMonitoringTimingStoreFactory sessionMonitoringTimingStoreFactory,
                                  ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> refreshStore,
                                  ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> propertyStore,
                                  ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> destroyStore,
                                  ConcurrentHashMap<SessionMonitorType, SessionMonitoringTimingStore> logoutStore) {
        this.executorService = executorService;
        this.sessionMonitoringTimingStoreFactory = sessionMonitoringTimingStoreFactory;

        this.refreshStore = refreshStore;
        this.propertyStore = propertyStore;
        this.destroyStore = destroyStore;
        this.logoutStore = logoutStore;
    }

    /**
     * Stores an entry in the refreshStore, offloading the work to another thread.
     *
     * @param duration the length of time the new entry represents
     * @param type the type of session to which this entry pertains
     */
    public void storeRefreshTime(final long duration, final SessionMonitorType type) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                    if (!refreshStore.containsKey(type)) {
                        refreshStore.putIfAbsent(type,
                                sessionMonitoringTimingStoreFactory.createSessionMonitoringTimingStore());
                    }

                refreshStore.get(type).addTimingEntry(duration);
            }
        });
    }

    /**
     * Gets the average value of entries in the refreshStore.
     *
     * @param type the type of session whose averages we are interested in
     * @return the average duration (in nanoseconds)
     */
    public long getAverageRefreshTime(SessionMonitorType type) {
        if (refreshStore.get(type) == null) {
            return 0L;
        }

        return refreshStore.get(type).getDurationAverage();
    }

    /**
     * Stores an entry in the propertyStore, offloading the work to another thread.
     *
     * @param duration the length of time the new entry represents
     * @param type the type of session to which this entry pertains
     */
    public void storeSetPropertyTime(final long duration, final SessionMonitorType type) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!propertyStore.containsKey(type)) {
                     propertyStore.putIfAbsent(type,
                            sessionMonitoringTimingStoreFactory.createSessionMonitoringTimingStore());
                }

                propertyStore.get(type).addTimingEntry(duration);
            }
        });
    }

    /**
     * Gets the average value of entries in the propertyStore.
     *
     * @param type the type of session whose averages we are interested in
     * @return the average duration (in nanoseconds)
     */
    public long getAverageSetPropertyTime(SessionMonitorType type) {
        if (propertyStore.get(type) == null) {
            return 0L;
        }

        return propertyStore.get(type).getDurationAverage();
    }

    /**
     * Stores an entry in the destroyStore, offloading the work to another thread.
     *
     * @param duration the length of time the new entry represents
     * @param type the type of session to which this entry pertains
     */
    public void storeDestroyTime(final long duration, final SessionMonitorType type) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                    if (!destroyStore.containsKey(type)) {
                        destroyStore.putIfAbsent(type,
                                sessionMonitoringTimingStoreFactory.createSessionMonitoringTimingStore());
                    }

                destroyStore.get(type).addTimingEntry(duration);
            }
        });

    }

    /**
     * Gets the average value of entries in the destroyStore.
     *
     * @param type the type of session whose averages we are interested in
     * @return the average duration (in nanoseconds)
     */
    public long getAverageDestroyTime(SessionMonitorType type) {
        if (destroyStore.get(type) == null) {
            return 0L;
        }

        return destroyStore.get(type).getDurationAverage();
    }

    /**
     * Stores an entry in the logoutStore, offloading the work to another thread.
     *
     * @param duration the length of time the new entry represents
     * @param type the type of session to which this entry pertains
     */
    public void storeLogoutTime(final long duration, final SessionMonitorType type) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                    if (!logoutStore.containsKey(type)) {
                        logoutStore.putIfAbsent(type,
                                sessionMonitoringTimingStoreFactory.createSessionMonitoringTimingStore());
                    }

                logoutStore.get(type).addTimingEntry(duration);
            }
        });
    }

    /**
     * Gets the average value of entries in the logoutStore.
     *
     * @param type the type of session whose averages we are interested in
     * @return the average duration (in nanoseconds)
     */
    public long getAverageLogoutTime(SessionMonitorType type) {
        if (logoutStore.get(type) == null) {
            return 0L;
        }

        return logoutStore.get(type).getDurationAverage();
    }

}
