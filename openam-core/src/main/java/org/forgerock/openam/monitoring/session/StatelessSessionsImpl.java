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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.monitoring.session;

import com.iplanet.dpro.session.monitoring.SessionMonitorType;
import com.iplanet.dpro.session.monitoring.SessionMonitoringStore;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;

/**
 * Monitoring for stateless sessions.
 */
public class StatelessSessionsImpl extends StatelessSessions {
    private final SessionMonitoringStore monitoringStore;

    public StatelessSessionsImpl(final SnmpMib mib, final SessionMonitoringStore monitoringStore) {
        super(mib);
        this.monitoringStore = monitoringStore;
    }

    /**
     * Getter for the "AverageStatelessSetPropertyTime" variable.
     */
    @Override
    public Long getAverageStatelessSetPropertyTime() throws SnmpStatusException {
        return monitoringStore.getAverageSetPropertyTime(SessionMonitorType.STATELESS);
    }

    /**
     * Getter for the "AverageStatelessDestroyTime" variable.
     */
    @Override
    public Long getAverageStatelessDestroyTime() throws SnmpStatusException {
        return monitoringStore.getAverageDestroyTime(SessionMonitorType.STATELESS);
    }

    /**
     * Getter for the "AverageStatelessLogoutTime" variable.
     */
    @Override
    public Long getAverageStatelessLogoutTime() throws SnmpStatusException {
        return monitoringStore.getAverageLogoutTime(SessionMonitorType.STATELESS);
    }

    /**
     * Getter for the "AverageStatelessRefreshTime" variable.
     */
    @Override
    public Long getAverageStatelessRefreshTime() throws SnmpStatusException {
        return monitoringStore.getAverageRefreshTime(SessionMonitorType.STATELESS);
    }

    /**
     * Getter for the "SumStatelessSessions" variable.
     */
    @Override
    public Long getSumStatelessSessions() throws SnmpStatusException {
        // This figure is difficult to know for stateless sessions. We still return a placeholder, for the sake of
        // consistency with the other session types. It may be possible to record/estimate this information in future.
        return 0L;
    }

}
