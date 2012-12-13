/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CacheStats.java,v 1.4 2008/06/25 05:41:24 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.common;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;
import com.sun.identity.shared.stats.StatsListener;

// Class used for synchronization purpose.
public class CacheStats implements StatsListener {

    String name;

    int intervalCount = 0; // interval counter

    // Store the Cache Size at every update to display correct size
    int cacheSize = 0;

    long totalGetRequests = 0; // Overall get requests

    long totalCacheHits = 0; // Overall cache hits

    long totalIntervalHits = 0; // Hits during interval

    Debug debug;

    private static Stats stats = null;

    private static final String CACHE_STATS_FILE_NAME = "amSDKStats";

    static {
        stats = Stats.getInstance(CACHE_STATS_FILE_NAME);
    }

    /**
     * Creates a new CacheStats object, adds the object as a listener to the
     * Stats class and returns the object.
     * 
     * @param instanceName
     *            name associated with the CacheStats instance.
     * @param debugObject
     *            the debug instance to which the CacheStats object will be
     *            associated.
     * @return a CacheStats object
     */
    public static CacheStats createInstance(String instanceName,
            Debug debugObject) {
        CacheStats cStats = new CacheStats(instanceName, debugObject);
        if (stats.isEnabled()) {
            stats.addStatsListener(cStats);
        }
        return cStats;
    }

    public CacheStats(String instanceName, Debug debugObject) {
        name = instanceName;
        debug = debugObject;
        if (debug.messageEnabled()) {
            debug.message("CacheStats() Stats : " + stats.isEnabled());
        }
    }

    public String getName() {
        return name;
    }

    public void updateHitCount(int sizeOfCache) {
        if (stats.isEnabled()) {
            synchronized (this) {
                totalCacheHits++;
                totalIntervalHits++;
                cacheSize = sizeOfCache;
            }
        }
    }

    public void incrementRequestCount(int sizeOfCache) {
        if (stats.isEnabled()) {
            synchronized (this) {
                totalGetRequests++;
                intervalCount++;
                cacheSize = sizeOfCache;
            }
        }
    }

    protected synchronized int getIntervalCount() {
        return intervalCount;
    }

    public synchronized void printStats() {
        // Print Stats information
        stats.record("SDK Cache Statistics" + "\n--------------------"
                + "\nNumber of requests during this interval: " + intervalCount
                + "\nNumber of Cache Hits during this interval: "
                + totalIntervalHits + "\nHit ratio for this interval: "
                + (double) totalIntervalHits / (double) intervalCount
                + "\nTotal number of requests since server start: "
                + totalGetRequests
                + "\nTotal number of Cache Hits since server start: "
                + totalCacheHits + "\nOverall Hit ratio: "
                + (double) totalCacheHits / (double) totalGetRequests
                + "\nTotal Cache Size: " + cacheSize + "\n");

        // Reset interval hits to 0
        intervalCount = 0;
        totalIntervalHits = 0;
    }
}
