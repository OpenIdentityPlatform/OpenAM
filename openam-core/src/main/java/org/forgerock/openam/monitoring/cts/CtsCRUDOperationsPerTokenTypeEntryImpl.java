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
 *
 */

package org.forgerock.openam.monitoring.cts;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.utils.Enums;

/**
 * Implementation of the monitoring endpoints for CTS CRUDL operations (except query and delete)
 * for specific token type.
 * <br/>
 * This endpoint should not be used for query(list) and delete operations on the CTS. This is because the token
 * type cannot be determined for these operations. For query and delete operations use the
 * {@link CtsCRUDOperationsEntryImpl}.
 *
 * @since 12.0.0
 */
public class CtsCRUDOperationsPerTokenTypeEntryImpl extends CtsCRUDOperationsPerTokenTypeEntry {

    private final CTSOperationsMonitoringStore monitoringStore;

    private Debug debug;

    /**
     * Constructs an instance of the CtsCRUDOperationsPerTokenTypeEntryImpl.
     *
     * @param myMib The Mib.
     * @param debug An instance of the Debug logger.
     */
    public CtsCRUDOperationsPerTokenTypeEntryImpl(SnmpMib myMib, Debug debug) {
        super(myMib);
        this.debug = debug;
        this.monitoringStore = InjectorHolder.getInstance(CTSOperationsMonitoringStore.class);
    }

    /**
     * Gets the TokenType enum from the OID endpoint.
     *
     * @return The TokenType from the endpoint.
     */
    private TokenType getTokenType() {
        TokenType token = null;

        try {
            token = Enums.getEnumFromOrdinal(TokenType.class, getTokenTableIndex().intValue() - 1);
        } catch (SnmpStatusException e) {
            if (debug.messageEnabled()) {
                debug.error("Unable to determine token type from supplied index.", e);
            }
        }

        if (token == null) {
            if (debug.messageEnabled()) {
                debug.error("Token type returned was null. Check supplied index.");
            }
        }

        return token;
    }

    /**
     * Gets the CTSOperation enum from the OID endpoint.
     *
     * @return The CTSOperation from the endpoint.
     */
    private CTSOperation getCTSOperation() {
        CTSOperation operation = null;

        try {
            operation = Enums.getEnumFromOrdinal(CTSOperation.class, getOperationTableIndex().intValue() - 1);
        } catch (SnmpStatusException e) {
            if (debug.messageEnabled()) {
                debug.error("Unable to determine CTS Operation from supplied index.", e);
            }
        }

        if (operation == null) {
            if (debug.messageEnabled()) {
                debug.error("CTS Operation returned was null. Check supplied index.");
            }
        }

        return operation;
    }

    /**
     * Gets the maximum rate that the specified CTS operation, on the specified Token type has been made on the CTS.
     *
     * @return The maximum rate.
     */
    @Override
    public Long getDMaximum() throws SnmpStatusException {
        final TokenType tokenType = getTokenType();
        final CTSOperation operation = getCTSOperation();

        if (tokenType == null || operation == null) {
            throw new InvalidSNMPQueryException();
        }

        return monitoringStore.getMaximumOperationsPerPeriod(getTokenType(), getCTSOperation());
    }

    /**
     * Gets the minimum rate that the specified CTS operation, on the specified Token type has been made on the CTS.
     *
     * @return The minimum rate.
     */
    @Override
    public Long getDMinimum() throws SnmpStatusException {
        final TokenType tokenType = getTokenType();
        final CTSOperation operation = getCTSOperation();

        if (tokenType == null || operation == null) {
            throw new InvalidSNMPQueryException();
        }

        return monitoringStore.getMinimumOperationsPerPeriod(getTokenType(), getCTSOperation());
    }

    /**
     * Gets the average rate that the specified CTS operation, on the specified Token type has been made on the CTS.
     *
     * @return The average rate.
     */
    @Override
    public Long getDAverage() throws SnmpStatusException {
        final TokenType tokenType = getTokenType();
        final CTSOperation operation = getCTSOperation();

        if (tokenType == null || operation == null) {
            throw new InvalidSNMPQueryException();
        }

        return (long) monitoringStore.getAverageOperationsPerPeriod(getTokenType(), getCTSOperation());
    }

    /**
     * Gets the cumulative count for the specified CTS operation, on the specified Token type.
     *
     * @return The operations cumulative count.
     */
    @Override
    public Long getDCumulativeCount() throws SnmpStatusException {
        final TokenType tokenType = getTokenType();
        final CTSOperation operation = getCTSOperation();

        if (tokenType == null || operation == null) {
            throw new InvalidSNMPQueryException();
        }

        return monitoringStore.getOperationsCumulativeCount(getTokenType(), getCTSOperation());
    }
}
