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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.cts.monitoring.impl.reaper;

import java.util.ArrayList;
import java.util.List;

/**
 * This class maintains a store of information about each CTS Reaper run since server start up.
 *
 * @since 12.0.0
 */
public class ReaperMonitor {

    private final List<ReaperRun> reaperRuns = new ArrayList<ReaperRun>();

    /**
     * {@inheritDoc}
     */
    public void add(final long startTime, final long runTime, final long numberOfDeletedSessions) {
        reaperRuns.add(new ReaperRun(startTime, runTime, numberOfDeletedSessions));
    }

    /**
     * {@inheritDoc}
     */
    public double getRateOfDeletion() {

        if (reaperRuns.isEmpty()) {
            return 0D;
        }

        double numDeletedSessions = 0D;
        for (ReaperRun reaperRun : reaperRuns) {
            numDeletedSessions += reaperRun.getNumberOfDeletedSessions();
        }
        return numDeletedSessions / reaperRuns.size();
    }

    /**
     * Models a run by the CTS Reaper and holds information about when the run started and stopped and the number of
     * sessions the run deleted.
     */
    private static class ReaperRun {

        private final long startTime;
        private final long runTime;
        private final long numberOfDeletedSessions;

        /**
         * Creates a new Reaper Run instance.
         *
         * @param startTime The start time of the reaper run.
         * @param runTime The end time of the reaper run.
         * @param numberOfDeletedSessions The total number of deleted sessions.
         */
        public ReaperRun(final long startTime, final long runTime, final long numberOfDeletedSessions) {
            this.startTime = startTime;
            this.runTime = runTime;
            this.numberOfDeletedSessions = numberOfDeletedSessions;
        }

        /**
         * Gets the start time of the reaper run.
         *
         * @return The start time of the run.
         */
        private long getStartTime() {
            return startTime;
        }

        /**
         * Gets the end time of the reaper run.
         *
         * @return The end time of the run.
         */
        private long getRunTime() {
            return runTime;
        }

        /**
         * Gets the total number of deleted sessions.
         *
         * @return Number of deleted sessions by the run.
         */
        private long getNumberOfDeletedSessions() {
            return numberOfDeletedSessions;
        }
    }
}
