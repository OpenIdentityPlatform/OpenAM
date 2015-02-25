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

package org.forgerock.openam.monitoring.cts;

import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.guice.core.InjectorHolder;

/**
 * Implementation of the endpoint created by the monitoring framework.
 *
 * The injected monitoring store is shared between the monitoring framework
 * and a {@link org.forgerock.openam.cts.monitoring.impl.connections.MonitoredCTSConnectionFactory}
 */
public class CtsConnectionFailureRateImpl extends CtsConnectionFailureRate {

    private final CTSConnectionMonitoringStore monitoringStore;

    /**
     * Constructs an instance of the CtsConnectionFailureRateImpl
     *
     * @param myMib The Mib.
     */
    public CtsConnectionFailureRateImpl(SnmpMib myMib) {
        super(myMib);
        this.monitoringStore = InjectorHolder.getInstance(CTSConnectionMonitoringStore.class);
    }

    /**
     * Getter for the "FailureMaximum" variable.
     */
    public Long getFailureMaximum() throws SnmpStatusException {
        return Math.round(monitoringStore.getMaximumOperationsPerPeriod(false));
    }

    /**
     * Getter for the "FailureMinimum" variable.
     */
    public Long getFailureMinimum() throws SnmpStatusException {
        return Math.round(monitoringStore.getMinimumOperationsPerPeriod(false));
    }

    /**
     * Getter for the "FailureAverage" variable.
     */
    public Long getFailureAverage() throws SnmpStatusException {
        return Math.round(monitoringStore.getAverageConnectionsPerPeriod(false));
    }

    /**
     * Getter for the "FailureCumulative" variable.
     */
    public Long getFailureCumulative() throws SnmpStatusException {
        return Math.round(monitoringStore.getConnectionsCumulativeCount(false));
    }


}
