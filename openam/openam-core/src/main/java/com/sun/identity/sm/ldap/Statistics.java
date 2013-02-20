/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */
package com.sun.identity.sm.ldap;

import com.iplanet.am.util.SystemProperties;

import java.io.Serializable;

/**
 * Statistics implementation used by the store to keep track of the number
 * and type of received requests.
 * 
 * Statistics can be enabled/disabled using the configuration file.
 * 
 * @author steve
 */
public class Statistics implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * Globals
     */
    private static boolean enabled = true;
    private static Statistics instance = null;

    private static final String STATS_ENABLED = "amsessiondb.enabled";

    /**
     * Static Initialization Stanza.
     */
    static {
        initialize();
    }
    
    private static void initialize() {
        enabled = SystemProperties.getAsBoolean(STATS_ENABLED, true);
    }
    
    private int totalRequests;
    private int totalReads;
    private int totalWrites;
    private int totalDeletes;
    private int totalReadRecordCount;
    private long writeTimeMin;
    private long writeTimeMax;
    private long writeTimeAverage;
    private long writeCumulativeCount;
    private long readTimeMin;
    private long readTimeMax;
    private long readTimeAverage;
    private long readCumulativeCount;
    private long deleteTimeMin;
    private long deleteTimeMax;
    private long deleteTimeAverage;
    private long deleteCumulativeCount;
    private long readRecordTimeMin;
    private long readRecordTimeMax;
    private long readRecordTimeAverage;
    private long readRecordCumulativeCount;
    
    private Statistics() {
        // do nothing
    }
    
    /**
     * Returns the singleton instance
     * 
     * @return 
     */
    public static synchronized Statistics getInstance() {
        if (instance == null) {
            instance = new Statistics();
        }
        
        return instance;
    }
    
    /**
     * Returns the total number of requests
     * 
     * @return The total number of requests
     */
    public int getTotalRequests() {
        return totalRequests;
    }
    
    /**
     * Returns the total number of reads
     * 
     * @return The total number of reads
     */
    public int getTotalReads() {
        return totalReads;
    }
    
    /**
     * Increment the total read count
     */
    public void incrementTotalReads() {
        totalReads++;
        totalRequests++;
    }
    
    /**
     * Update the read request time count
     * 
     * @param time The time in ms of the last read request
     */
    public void updateReadTime(long time) {
        if (time > 0 && time < readTimeMin) {
            readTimeMin = time;
        }
        
        if (time > readTimeMax) {
            readTimeMax = time;
        }
        
        readCumulativeCount += time;
        readTimeAverage = readCumulativeCount / totalReads;
    }
    
    /**
     * Returns the read request minimum time
     * 
     * @return the minimum read request time in ms
     */
    public long getReadRequestTimeMinimum() {
        return readTimeMin;
    }
    
    /**
     * Returns the read request maximum time
     * 
     * @return the maximum read request time in ms 
     */
    public long getReadRequestTimeMaximum() {
        return readTimeMax;
    }
    
    /**
     * Returns the read request average time
     * 
     * @return the average read request time in ms
     */
    public long getReadRequestAverageTime() {
        return readTimeAverage;
    }
    
    /**
     * Get the total number of writes
     * 
     * @return The total number of writes
     */
    public int getTotalWrites() {
        return totalWrites;
    }
    
    /**
     * Increment the total number of writes
     */
    public void incrementTotalWrites() {
        totalWrites++;
        totalRequests++;
    }
    
    /**
     * Update the write request time count
     * 
     * @param time The time in ms of the last write request
     */
    public void updateWriteTime(long time) {
        if (time > 0 && time < writeTimeMin) {
            writeTimeMin = time;
        }
        
        if (time > writeTimeMax) {
            writeTimeMax = time;
        }
        
        writeCumulativeCount += time;
        writeTimeAverage = writeCumulativeCount / totalWrites;
    }
    
    /**
     * Returns the write request minimum time
     * 
     * @return the minimum write request time in ms
     */
    public long getWriteRequestTimeMinimum() {
        return writeTimeMin;
    }
    
    /**
     * Returns the write request maximum time
     * 
     * @return the maximum write request time in ms 
     */
    public long getWriteRequestTimeMaximum() {
        return writeTimeMax;
    }
    
    /**
     * Returns the write request average time
     * 
     * @return the average write request time in ms
     */
    public long getWriteRequestAverageTime() {
        return writeTimeAverage;
    }
    
    /**
     * Get the total number of deletes
     * 
     * @return The total number of deletes 
     */
    public int getTotalDeletes() {
        return totalDeletes;
    }
    
    /**
     * Increment the total number of deletes
     */
    public void incrementTotalDeletes() {
        totalDeletes++;
        totalRequests++;
    }
    
    /**
     * Update the delete request time count
     * 
     * @param time The time in ms of the last delete request
     */
    public void updateDeleteTime(long time) {
        if (time > 0 && time < deleteTimeMin) {
            deleteTimeMin = time;
        }
        
        if (time > deleteTimeMax) {
            deleteTimeMax = time;
        }
        
        deleteCumulativeCount += time;
        deleteTimeAverage = deleteCumulativeCount / totalDeletes;
    }
    
    /**
     * Returns the delete request minimum time
     * 
     * @return the minimum delete request time in ms
     */
    public long getDeleteRequestTimeMinimum() {
        return deleteTimeMin;
    }
    
    /**
     * Returns the delete request maximum time
     * 
     * @return the maximum delete request time in ms 
     */
    public long getDeleteRequestTimeMaximum() {
        return deleteTimeMax;
    }
    
    /**
     * Returns the delete request average time
     * 
     * @return the average delete request time in ms
     */
    public long getDeleteRequestAverageTime() {
        return deleteTimeAverage;
    }
    
    /**
     * Get the total number of reads record count
     * 
     * @return 
     */
    public int getTotalReadRecordCount() {
        return totalReadRecordCount;
    }
    
    /**
     * Increment the total read record count total
     */
    public void incrementTotalReadRecordCount() {
        totalReadRecordCount++;
        totalRequests++;
    }
    
    /**
     * Update the read record count request time count
     * 
     * @param time The time in ms of the last read record count request
     */
    public void updateReadRecordCountTime(long time) {
        if (time > 0 && time < readRecordTimeMin) {
            readRecordTimeMin = time;
        }
        
        if (time > readRecordTimeMax) {
            readRecordTimeMax = time;
        }
        
        readRecordCumulativeCount += time;
        readRecordTimeAverage = readRecordCumulativeCount / totalReadRecordCount;
    }
    
    /**
     * Returns the read record count request minimum time
     * 
     * @return the minimum read record count request time in ms
     */
    public long getReadRecordRequestTimeMinimum() {
        return readRecordTimeMin;
    }
    
    /**
     * Returns the read record count request maximum time
     * 
     * @return the maximum read record count request time in ms 
     */
    public long getReadRecordRequestTimeMaximum() {
        return readRecordTimeMax;
    }
    
    /**
     * Returns the read record count request average time
     * 
     * @return the average read record count request time in ms
     */
    public long getReadRecordRequestAverageTime() {
        return readRecordTimeAverage;
    }
    
    /**
     * Resets the statistics counters to zero.
     */
    public void resetStatistics() {
        totalRequests = 0;
        totalReads = 0;
        totalWrites = 0;
        totalDeletes = 0;
        totalReadRecordCount = 0;
        writeTimeMin = 0;
        writeTimeMax = 0;
        writeTimeAverage = 0;
        writeCumulativeCount = 0;
        readTimeMin = 0;
        readTimeMax = 0;
        readTimeAverage = 0;
        readCumulativeCount = 0;
        deleteTimeMin = 0;
        deleteTimeMax = 0;
        deleteTimeAverage = 0;
        deleteCumulativeCount = 0;
        readRecordTimeMin = 0;
        readRecordTimeMax = 0;
        readRecordTimeAverage = 0;
        readRecordCumulativeCount = 0;
    }
    
    /** 
     * Used to determine if statistics is enabled in the server 
     * 
     * @return true if stats are enabled, false otherwise.
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
}
