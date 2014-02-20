/*
 * Copyright 2013-2014 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.monitoring.cts;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.guice.core.InjectorHolder;

/**
 * Implementation of the monitoring endpoints for CTS CRUDL operations (query and delete).
 * <br/>
 * This endpoint should only be used for query(list) and delete operations on the CTS. This is because the token
 * type cannot be determined for these operations. For all other operations use the
 * {@link CtsCRUDOperationsPerTokenTypeEntryImpl}.
 *
 * @since 12.0.0
 */
public class CtsCRUDOperationsEntryImpl extends CtsCRUDOperationsEntry {

    private final CTSOperationsMonitoringStore monitoringStore;

    private Debug debug;

    /**
     * Constructs an instance of the CtsCRUDOperationsEntryImpl.
     *
     * @param myMib The Mib.
     * @param debug An instance of the Debug logger.
     */
    public CtsCRUDOperationsEntryImpl(SnmpMib myMib, Debug debug) {
        super(myMib);
        this.debug = debug;
        this.monitoringStore = InjectorHolder.getInstance(CTSOperationsMonitoringStore.class);
    }

    /**
     * Gets the CTSOperation enum from the OID endpoint.
     *
     * @return The CTSOperation from the endpoint.
     */
    private CTSOperation getCTSOperation() {
        CTSOperation operation = null;

        try {
            operation = CTSOperation.getOperationFromOrdinalIndex(getOperationTableIndex().intValue() - 1);
        } catch (SnmpStatusException e) {
            if (debug.messageEnabled()) {
                debug.error("Unable to determine CTS Operation from supplied index.");
            }
        }

        if (operation == null) {
            if (debug.messageEnabled()) {
                debug.error("CTS Operation returned was null. Check supplied index.");
                throw new InvalidSNMPQueryException();
            }
        }

        return operation;
    }

    /**
     * Gets the maximum rate that the specified CTS operation has been made on the CTS.
     *
     * @return The maximum rate.
     */
    @Override
    public Long getSMaximum() {
        return monitoringStore.getMaximumOperationsPerPeriod(null, getCTSOperation());
    }

    /**
     * Gets the maximum failure rate of the specified CTS operation.
     *
     * @return the maximum failure rate.
     */
    @Override
    public Long getSFailureMaximum() {
        return monitoringStore.getMaximumOperationFailuresPerPeriod(getCTSOperation());
    }

    /**
     * Gets the minimum rate that the specified CTS operation has been made on the CTS.
     *
     * @return The minimum rate.
     */
    @Override
    public Long getSMinimum() throws SnmpStatusException {
        return monitoringStore.getMinimumOperationsPerPeriod(null, getCTSOperation());
    }

    /**
     * Gets the minimum failure rate of the given CTS operation in the current period.
     *
     * @return the minimum failure rate of the operation.
     */
    @Override
    public Long getSFailureMinimum() {
        return monitoringStore.getMinimumOperationFailuresPerPeriod(getCTSOperation());
    }

    /**
     * Gets the average rate that the specified CTS operation has been made on the CTS.
     *
     * @return The average rate.
     */
    @Override
    public Long getSAverage() throws SnmpStatusException {
        return (long) monitoringStore.getAverageOperationsPerPeriod(null, getCTSOperation());
    }

    /**
     * Gets the average failure rate of the given CTS operation in the current period.
     *
     * @return the average failure rate.
     */
    @Override
    public Long getSFailureAverage() {
        return (long) monitoringStore.getAverageOperationFailuresPerPeriod(getCTSOperation());
    }

    /**
     * Gets the cumulative count for the specified CTS operation.
     *
     * @return The operations cumulative count.
     */
    @Override
    public Long getSCumulativeCount() throws SnmpStatusException {
        final CTSOperation operation = getCTSOperation();

        if (operation == null) {
            throw new InvalidSNMPQueryException();
        }

        return monitoringStore.getOperationsCumulativeCount(null, getCTSOperation());
    }

    /**
     * Gets the cumulative count for the specified CTS operation.
     *
     * @return The operations cumulative count.
     */
    @Override
    public Long getSFailureCount() throws SnmpStatusException {
        final CTSOperation operation = getCTSOperation();

        if (operation == null) {
            throw new InvalidSNMPQueryException();
        }

        return monitoringStore.getOperationFailuresCumulativeCount(operation);
    }

}
