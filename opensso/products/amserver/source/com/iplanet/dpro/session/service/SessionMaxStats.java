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
 * $Id: SessionMaxStats.java,v 1.4 2008/06/25 05:41:31 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session.service;

import java.util.Hashtable;

import com.sun.identity.shared.stats.StatsListener;

/** 
 * <code>SessionMaxStats</code> implements the <code>StatsListener</code>
 * and provides the information of the total number of sessions in the session 
 * table and the number of active sessions in the table.
 */
public class SessionMaxStats implements StatsListener {

    private Hashtable sessionTable;
    private int peakSessions = 0;
    private int peakActiveSessions = 0;
    private int peakNotificationQueue = 0;


   /**
    * Creates a new SessionMaxStats
    * @param table session table
    */
   public SessionMaxStats(Hashtable table) {
        sessionTable = table;
    }

    /**
     * Prints the session statistics for the given session table.
     *
     */
   public void printStats() {
       if (!sessionTable.isEmpty() ) {
           
           int maxSessions = sessionTable.size();
           int maxActiveSessions = SessionService.getActiveSessions();
           int notificationQueue = SessionService.getNotificationQueueSize();
           
           if (maxSessions > peakSessions) {
               peakSessions = maxSessions;
           }
           if (maxActiveSessions > peakActiveSessions) {
               peakActiveSessions = maxActiveSessions;
           }
           if (notificationQueue > peakNotificationQueue) {
               peakNotificationQueue = notificationQueue;
           }
           
           SessionService.stats.record(
                 "Max sessions in session table Current/Peak:" + 
                     maxSessions + "/" + peakSessions + "\n" +
                 "Max active sessions Current/Peak:" + 
                     maxActiveSessions + "/" + peakActiveSessions + "\n" +
                 "Session Notifications in Queue Current/Peak:" 
                     + notificationQueue + "/" + peakNotificationQueue);
       } 
       else {
         SessionService.stats.record("No sessions found in session table"); 
       }
   }
}

