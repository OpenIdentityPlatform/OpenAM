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
 * Implementation for hooking the SNMP {@link InternalSessions} class in to OpenAM to report on our stats.
 */
public class InternalSessionsImpl extends InternalSessions {

    private final SessionMonitoringStore monitorStore;

    public InternalSessionsImpl(SnmpMib myMib, SessionMonitoringStore monitorStore) {
        super(myMib);
        this.monitorStore = monitorStore;
    }

    /**
     * Getter for the "AverageInternalSetPropertyTime" variable.
     */
    public Long getAverageInternalSetPropertyTime() throws SnmpStatusException {
        return monitorStore.getAverageSetPropertyTime(SessionMonitorType.LOCAL);
    }

    /**
     * Getter for the "AverageInternalDestroyTime" variable.
     */
    public Long getAverageInternalDestroyTime() throws SnmpStatusException {
        return monitorStore.getAverageDestroyTime(SessionMonitorType.LOCAL);
    }

    /**
     * Getter for the "AverageInternalLogoutTime" variable.
     */
    public Long getAverageInternalLogoutTime() throws SnmpStatusException {
        return monitorStore.getAverageLogoutTime(SessionMonitorType.LOCAL);
    }

    /**
     * Getter for the "AverageInternalRefreshTime" variable.
     */
    public Long getAverageInternalRefreshTime() throws SnmpStatusException {
        return monitorStore.getAverageRefreshTime(SessionMonitorType.LOCAL);
    }

    /**
     * Getter for the "SumInternalSessions" variable.
     */
    public Long getSumInternalSessions() throws SnmpStatusException {
        return (long) SessionService.getSessionService().getInternalSessionCount();
    }


}
