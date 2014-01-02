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

package org.forgerock.openam.cts.monitoring;

/**
 * A data structure for storing monitoring information about the CTS Reaper.
 * The CTS Reaper will use an instance of this data structure to store information about runs as and when they
 * occur and then the CTS monitoring framework will us the same instance to pull information out to send to clients
 * as monitoring requests are made.
 *
 * @since 12.0.0
 */
public interface CTSReaperMonitoringStore {

    /**
     * Adds a CTS Reaper run to the monitoring store.
     *
     * @param startTime The start time of the reaper run.
     * @param runTime The end time of the reaper run.
     * @param numberOfDeletedSessions The total number of deleted sessions.
     */
    void addReaperRun(long startTime, long runTime, long numberOfDeletedSessions);

    /**
     * Gets the average rate of deletion based from all of the reaper runs since server start up.
     *
     * @return The rate of session deletion by the CTS Reaper.
     */
    double getRateOfDeletedSessions();
}
