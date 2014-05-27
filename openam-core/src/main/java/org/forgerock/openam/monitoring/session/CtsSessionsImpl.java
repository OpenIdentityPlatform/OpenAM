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
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.monitoring.impl.persistence.CtsPersistenceOperationsMonitor;
import org.forgerock.openam.monitoring.cts.InvalidSNMPQueryException;

/**
 * Implementation for hooking the SNMP {@link CtsSessions} class in to OpenAM to report on our stats.
 */
public class CtsSessionsImpl extends CtsSessions {

    private final CtsPersistenceOperationsMonitor ctsMonitor;
    private final SessionMonitoringStore monitorStore;

    public CtsSessionsImpl(CtsPersistenceOperationsMonitor ctsMonitor, SnmpMib myMib,
                           SessionMonitoringStore monitorStore) {
        super(myMib);
        this.ctsMonitor = ctsMonitor;
        this.monitorStore = monitorStore;
    }

    /**
     * Getter for the "SumCTSSessions" variable.
     */
    public Long getSumCTSSessions() throws SnmpStatusException {
        try {
            return ctsMonitor.getTotalCount();
        } catch (CoreTokenException e) {
            throw new InvalidSNMPQueryException("CTS Persistence did not return a valid result.", e);
        }
    }

    /**
     * Getter for the "AverageRemoteSetPropertyTime" variable.
     */
    public Long getAverageCTSSetPropertyTime() throws SnmpStatusException {
        return monitorStore.getAverageSetPropertyTime(SessionMonitorType.CTS);
    }

    /**
     * Getter for the "AverageRemoteDestroyTime" variable.
     */
    public Long getAverageCTSDestroyTime() throws SnmpStatusException {
        return monitorStore.getAverageDestroyTime(SessionMonitorType.CTS);
    }

    /**
     * Getter for the "AverageRemoteLogoutTime" variable.
     */
    public Long getAverageCTSLogoutTime() throws SnmpStatusException {
        return monitorStore.getAverageLogoutTime(SessionMonitorType.CTS);
    }

    /**
     * Getter for the "AverageRemoteRefreshTime" variable.
     */
    public Long getAverageCTSRefreshTime() throws SnmpStatusException {
        return monitorStore.getAverageRefreshTime(SessionMonitorType.CTS);
    }

}
