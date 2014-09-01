/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NetworkMonitor.java,v 1.2 2009/12/17 18:03:51 veiming Exp $
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 */

package com.sun.identity.entitlement.util;

import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.PrivilegeManager;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility to collect response time and throughput on a per second basis.
 * 
 */
public class NetworkMonitor extends HttpServlet {

    // Static variables
    private static HashMap<String, NetworkMonitor> stats = new HashMap();
    private static EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            PrivilegeManager.superAdminSubject, "/");
    private static boolean collectStats = ec.networkMonitorEnabled();

    // Instance variables
    int maxHistory = 600; // 10 minutes
    LinkedList<StatsData> history = new LinkedList<StatsData>();

    // Current statistics
    float throughput;
    float totalResponseTime;
    
    /**
     * @return the collectStats
     */
    public static boolean isCollectStats() {
        return collectStats;
    }

    /**
     * @param aCollectStats the collectStats to set
     */
    public static void setCollectStats(boolean aCollectStats) {
        ec.setNetworkMonitorEnabled(aCollectStats);
        collectStats = aCollectStats;
    }
    
    public static Set<String> getInstanceNames() {
    	return (stats.keySet());
    }

    public static NetworkMonitor getInstance(String name) {
        name = name.toLowerCase();
        NetworkMonitor answer = stats.get(name);
        if (answer == null) {
            answer = new NetworkMonitor();
            stats.put(name, answer);
        }
        return (answer);
    }

    public long start() {
        if (!isCollectStats()) {
            return 0;
        }
        return (System.currentTimeMillis());
    }

    public synchronized void end(long start) {
        if (isCollectStats()) {
            long rs = 0;
            throughput++;
            if (start != 0) {
                rs = System.currentTimeMillis() - start;
                totalResponseTime += rs;
            }
            StatsData sd = getNewStats(rs);
            if (history.isEmpty()) {
                history.addLast(sd);
            } else {
                StatsData hsd = history.getLast();
                if (hsd != null && hsd.equals(sd)) {
                    hsd.updateStatsData(sd);
                } else {
                    if (hsd != null) {
                        // Check if there are any missing intervals
                        long hsdTime = hsd.getTime();
                        while (hsdTime < sd.getTime() - 1) {
                            history.addLast(new StatsData(hsdTime++));
                        }
                    }
                    history.addLast(sd);
                    while (history.size() > maxHistory) {
                        history.removeFirst();
                    }
                }
            }
        }
    }

    public void reset() {
        throughput = 0;
        totalResponseTime = 0;
    }

    private StatsData getNewStats(long responsetime) {
        long currentTime = System.currentTimeMillis();
        // nearest second
        long seconds = (long) (currentTime/1000);
        StatsData sd = new StatsData(seconds);
        sd.addResponseTimes(responsetime);
        return(sd);
    }

    public float[] getHistoryResponseTime() {
        float[] rs = null;
        if (history.isEmpty()) {
            rs = new float[1];
            rs[0] = 0;
        } else {
            int size = history.size();
            rs = new float[size];
            for (StatsData s : history) {
                if (s.getCount() == 0) {
                    rs[size-1] = (float) s.getResponseTimes();
                } else {
                    rs[size-1] = ((float) s.getResponseTimes())/s.getCount();
                }
                size--;
            }
        }
        return (rs);
    }

    public float[] getHistoryThroughput() {
        float[] rs = null;
        if (history.isEmpty()) {
            rs = new float[1];
            rs[0] = 0;
        } else {
            int size = history.size();
            rs = new float[size];
            for (StatsData s : history) {
                rs[size-1] = ((float) s.getCount());
                size--;
            }
        }
        return (rs);
    }

    public float responseTime() {
        if (throughput > 0) {
            return (totalResponseTime/throughput);
        }
        return (0);
    }

    public float getLastHistoryResponseTime() {
        StatsData sd = history.getLast();
        if (sd != null) {
            float ans = sd.getResponseTimes();
            return ((ans)/sd.getCount());
        }
        return (0);
    }

    public float throughput() {
        return (throughput);
    }

    public float getLastHistoryThroughput() {
        StatsData sd = history.getLast();
        if (sd != null) {
            return ((float) sd.getCount());
        }
        return 0;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        // samples();
        NetworkMonitor sm = null;
        String path = req.getRequestURI();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() -1);
        }
        path = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
        Writer writer = res.getWriter();
        res.setHeader("content-type", "text/html");
        if (path != null && path.endsWith("entitlementmonitor")) {
            writer.write(stats.keySet().toString());
        } else if ((sm = stats.get(path)) != null) {
            float[] rs = sm.getHistoryResponseTime();
            float[] th = sm.getHistoryThroughput();
            writer.write(path);
            writer.write("<br>responsetime=[");
            for (float f : rs) {
                writer.write(Float.toString(f));
                writer.write(',');
            }
            writer.write("]<br>throughput=[");
            for (float f : th) {
                writer.write(Float.toString(f));
                writer.write(',');
            }
            writer.write(']');
        } else {
            writer.write("Unknown StatsMonitor: " + path);
        }
    }

//    private static long sampleCount = 10;
//    private void samples() {
//        NetworkMonitor sm = NetworkMonitor.getInstance("Evaluator");
//        long s = sm.start();
//        try {
//            Thread.sleep(sampleCount++);
//        } catch(InterruptedException ie) {
//
//        }
//        if (sampleCount > 50) {
//            sampleCount = 10;
//        }
//        sm.end(s);
//    }

    public class StatsData {
        private String time;
        private long seconds;
        private long responseTimes;
        private long count;

        protected StatsData(long seconds) {
            this.seconds = seconds;
            time = Long.toString(seconds);
        }

        public boolean updateStatsData(StatsData s) {
            if (time.equals(s.time)) {
                responseTimes += s.responseTimes;
                count += s.count;
                return (true);
            }
            return (false);
        }

        /**
         * @return the responseTimes
         */
        public long getResponseTimes() {
            return responseTimes;
        }

        /**
         * @param responseTimes the responseTimes to set
         */
        public void addResponseTimes(long responseTimes) {
            this.responseTimes += responseTimes;
            count++;
        }

        /**
         * @return the count
         */
        public long getCount() {
            return count;
        }

        public long getTime() {
            return (seconds);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof StatsData) {
                StatsData s = (StatsData) o;
                return (s.time.equals(time));
            }
            return (false);
        }

        @Override
        public int hashCode() {
            return time.hashCode();
        }
    }
}
