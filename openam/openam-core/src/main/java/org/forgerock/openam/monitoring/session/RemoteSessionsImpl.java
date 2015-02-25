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
package org.forgerock.openam.monitoring.session;

import com.iplanet.dpro.session.monitoring.SessionMonitoringStore;
import com.iplanet.dpro.session.monitoring.SessionMonitorType;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;

/**
 * Implementation for hooking the SNMP {@link RemoteSessions} class in to OpenAM to report on our stats.
 */
public class RemoteSessionsImpl extends RemoteSessions {

    private final SessionMonitoringStore monitorStore;

    public RemoteSessionsImpl(SnmpMib myMib, SessionMonitoringStore monitorStore) {
        super(myMib);
        this.monitorStore = monitorStore;
    }

    /**
     * Getter for the "AverageRemoteSetPropertyTime" variable.
     */
    public Long getAverageRemoteSetPropertyTime() throws SnmpStatusException {
        return monitorStore.getAverageSetPropertyTime(SessionMonitorType.REMOTE);
    }

    /**
     * Getter for the "AverageRemoteDestroyTime" variable.
     */
    public Long getAverageRemoteDestroyTime() throws SnmpStatusException {
        return monitorStore.getAverageDestroyTime(SessionMonitorType.REMOTE);
    }

    /**
     * Getter for the "AverageRemoteLogoutTime" variable.
     */
    public Long getAverageRemoteLogoutTime() throws SnmpStatusException {
        return monitorStore.getAverageLogoutTime(SessionMonitorType.REMOTE);
    }

    /**
     * Getter for the "AverageRemoteRefreshTime" variable.
     */
    public Long getAverageRemoteRefreshTime() throws SnmpStatusException {
        return monitorStore.getAverageRefreshTime(SessionMonitorType.REMOTE);
    }

    /**
     * Getter for the "SumRemoteSessions" variable.
     */
    public Long getSumRemoteSessions() throws SnmpStatusException {
        return (long) SessionService.getSessionService().getRemoteSessionCount();
    }

}
