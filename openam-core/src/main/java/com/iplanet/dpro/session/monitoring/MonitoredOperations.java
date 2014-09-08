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

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.share.SessionInfo;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Wraps a provided {@link SessionOperations} instance with timing meta information, and
 * inserts that into an appropriate monitoring store. Uses a {@link SessionMonitorType} to inform
 * the {@link SessionMonitoringStore} under what collection to store its entries.
 */
public class MonitoredOperations implements SessionOperations {

    private final SessionOperations sessionOperations;
    private final SessionMonitorType monitorType;

    private final SessionMonitoringStore sessionMonitoringStore;

    /**
     * Sets up an appropriate {@link SessionOperations}, linking it to its {@link SessionMonitorType} and
     * giving it a link to the singleton {@link SessionMonitoringStore}.
     *
     * @param sessionOperations The SessionOperations to wrap
     * @param monitorType Whether these SessionOperations are for LOCAL, REMOTE, etc.
     * @param sessionMonitoringStore The store to use to write the data to
     */
    public MonitoredOperations(SessionOperations sessionOperations, SessionMonitorType monitorType,
                               SessionMonitoringStore sessionMonitoringStore) {
        this.sessionOperations = sessionOperations;
        this.monitorType = monitorType;
        this.sessionMonitoringStore = sessionMonitoringStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SessionInfo refresh(Session session, boolean reset) throws SessionException {
        final long start = System.nanoTime();

        final SessionInfo response = sessionOperations.refresh(session, reset);

        sessionMonitoringStore.storeRefreshTime(System.nanoTime() - start, monitorType);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(Session session) throws SessionException {
        final long start = System.nanoTime();
        sessionOperations.logout(session);

        sessionMonitoringStore.storeLogoutTime(System.nanoTime() - start, monitorType);
    }

    @Override
    public void destroy(Session requester, Session session) throws SessionException {
        final long start = System.nanoTime();
        sessionOperations.destroy(requester, session);

        sessionMonitoringStore.storeDestroyTime(System.nanoTime() - start, monitorType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty(Session session, String name, String value) throws SessionException {
        final long start = System.nanoTime();
        sessionOperations.setProperty(session, name, value);

        sessionMonitoringStore.storeSetPropertyTime(System.nanoTime() - start, monitorType);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MonitoredOperations)) {
            return false;
        }
        MonitoredOperations op = (MonitoredOperations) obj;
        return op.monitorType == this.monitorType && op.sessionMonitoringStore == this.sessionMonitoringStore &&
                op.sessionOperations == this.sessionOperations;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("monitorType", monitorType)
                .append("sessionOperations", sessionOperations)
                .append("sessionMonitoringStore", sessionMonitoringStore)
                .toString();
    }
}
