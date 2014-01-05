/*
 * Copyright 2013 ForgeRock AS.
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
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.monitoring.impl.persistence.CtsPersistenceOperationsMonitor;
import org.forgerock.openam.utils.Enums;

/**
 * This class represents those operations which only take a token type as the input.
 *
 * The token type is input as part of the OID which results in this class being called and
 * is available through {@link org.forgerock.openam.monitoring.cts.CtsTokenOperationsEntry#getTokenTableIndex()}.
 *
 * For example, querying for the total number of a given type of token in the CTS at
 * any given moment.
 *
 */
public class CtsTokenOperationsEntryImpl extends CtsTokenOperationsEntry {

    //on error
    private final Debug debug;

    //from which to retrieve the data
    private final CtsPersistenceOperationsMonitor ctsPersistenceOperationsMonitor;

    /**
     * Constructor allows us to pass in the CtsPersistenceOperationsMonitor as well as appropriate
     * debugger.
     *
     * @param myMib Mib file this Entry implementation is a member of
     */
    public CtsTokenOperationsEntryImpl(SnmpMib myMib, Debug debug,
                                       CtsPersistenceOperationsMonitor ctsPersistenceOperationsMonitor) {
        super(myMib);
        this.debug = debug;
        this.ctsPersistenceOperationsMonitor = ctsPersistenceOperationsMonitor;

    }

    /**
     * Returns the total number of current tokens of the type specified
     * in the CTS at the current instant.
     *
     * @return a long of the number of tokens in the store, min. 0.
     */
    @Override
    public Long getTotalCount() {
        final long result;

        try {

            TokenType tokenType = Enums.getEnumFromOrdinal(TokenType.class, getTokenTableIndex().intValue() - 1);

            result = ctsPersistenceOperationsMonitor.getTotalCount(tokenType);

        } catch (CoreTokenException e) {
            String message = "CTS Persistence did not return a valid result.";
            if (debug.messageEnabled()) {
                debug.error(message, e);
            }
            throw new InvalidSNMPQueryException(message, e);
        } catch (SnmpStatusException e) {
            String message = "Unable to determine token type from supplied index.";
            if (debug.messageEnabled()) {
                debug.error(message, e);
            }
            throw new InvalidSNMPQueryException(message, e);
        }

        return result;
    }

    /**
     * Returns the average (mean) duration of the length of a token type's lifetime
     *
     * @return the average (mean) length of time for all tokens of the supplied type since the epoch in seconds
     */
    public Long getAverageDuration() {
        long result;

        try {
            TokenType tokenType = Enums.getEnumFromOrdinal(TokenType.class, getTokenTableIndex().intValue() - 1);

            result = ctsPersistenceOperationsMonitor.getAverageDuration(tokenType);

        } catch (CoreTokenException e) {
            String message = "CTS Persistence did not return a valid result.";
            if (debug.messageEnabled()) {
                debug.error(message, e);
            }
            throw new InvalidSNMPQueryException(message, e);
        } catch (SnmpStatusException e) {
            String message = "Unable to determine token type from supplied index.";
            if (debug.messageEnabled()) {
                debug.error(message, e);
            }
            throw new InvalidSNMPQueryException(message, e);
        }

        return result;
    }

}
