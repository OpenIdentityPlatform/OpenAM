/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.session.model;

/**
 * This singleton class is used to keep statistics about the running db.
 * 
 * @author steve
 */
public class DBStatistics {
    private int numRecords;
    private static DBStatistics instance = null;
    private static long startTime;
    
    static {
        initialize();
    }
    
    private static void initialize() {
        // remember when we started.
        startTime = System.currentTimeMillis();
    }
    
    private DBStatistics() {
        // do nothing
    }
    
    public static synchronized DBStatistics getInstance() {
        if (instance == null) {
            instance = new DBStatistics();
        }
        
        return instance;
    }
    
    /**
     * Returns the number of records in the database
     * 
     * @return the number of records
     */
    public int getNumRecords() {
        return numRecords;
    }
    
    /**
     * Sets the number of records currently in the database
     * 
     * @param numRecords The current number of records
     */
    public synchronized void setNumRecords(int numRecords) {
        this.numRecords = numRecords;
    }
    
    /**
     * Returns the current uptime (in ms.) of the amsessiondb server
     * 
     * @return 
     */
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }
}
