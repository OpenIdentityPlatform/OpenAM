/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IdCacheStats.java,v 1.2 2008/08/07 17:22:06 arviranga Exp $
 *
 */

package com.sun.identity.idm.common;

import com.sun.identity.shared.stats.Stats;
import com.sun.identity.shared.stats.StatsListener;

/** 
 * <code>IdCacheStats</code> implements the <code>StatsListener</code>
 * and provides the information of the total number of entry in cache 
 * table and the number of hits and total number of reads.
 */
public class IdCacheStats implements StatsListener {

    private String nameOfCache;

    int cacheSize = 0;     // number of entries in cache

    long totalGetRequests = 0; // Overall get requests

    long totalGetCacheHits = 0; // Overall cache hits

    long totalIntervalHits = 0; // Hits during interval

    long intervalCount = 0; // interval counter

    long totalSearchRequests = 0;  // Overall search request

    long totalSearchHits = 0;   // Overall search cache hits

    private Stats stats = null;


    /**
     * Creates a new IdRepo Stats
     *
     * @param name Name of Cache
     */
    public IdCacheStats(String name) {
        nameOfCache = name;
        stats = Stats.getInstance(name);
    }

    public void updateGetHitCount(int sizeOfCache) {
        if (stats.isEnabled()) {
            totalGetCacheHits++;
            totalIntervalHits++;
            cacheSize = sizeOfCache;
        }
    }

    public void incrementGetRequestCount(int sizeOfCache) {
        if (stats.isEnabled()) {
            totalGetRequests++;
            intervalCount++;
            cacheSize = sizeOfCache;
        }
    }

    public void updateSearchHitCount(int sizeOfCache) {
        if (stats.isEnabled()) {
            totalSearchHits++;
            totalIntervalHits++;
            cacheSize = sizeOfCache;
        }
    }

    public void incrementSearchRequestCount(int sizeOfCache) {
        if (stats.isEnabled()) {
            totalSearchRequests++;
            intervalCount++;
            cacheSize = sizeOfCache;
        }
    }


    /**
     * Prints the session statistics for the given session table.
     *
     */
    public void printStats() {
        // Print Stats information
        stats.record("Idm  Cache Statistics: " + nameOfCache + "\n--------------------"
                + "\nNumber of Get and Search requests during this interval: " 
                + (double) intervalCount
                + "\nNumber of Hits during this interval: "
                + totalIntervalHits + "\nHit ratio for this interval: "
                + (double) totalIntervalHits / (double) intervalCount
                + "\nTotal number of Get requests since server start: "
                + totalGetRequests
                + "\nTotal number of Get Hits since server start: "
                + totalGetCacheHits + "\nOverall Hit ratio: "
                + (double) totalGetCacheHits / (double) totalGetRequests
                + "\nTotal number of Search requests since server start: "
                + totalSearchRequests
                + "\nTotal number of FQDN Search hits since server start: "
                + totalSearchHits + "\nOverall Hit ratio: "
                + (double) totalSearchHits / (double) totalSearchRequests
                + "\nTotal Cache Size: " + cacheSize + "\n");

        // Reset interval hits to 0
        intervalCount = 0;
        totalIntervalHits = 0;
    
   }
}

