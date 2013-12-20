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
import javax.inject.Named;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.utils.Enums;
import org.forgerock.opendj.ldap.Filter;

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

    //for building up our query
    private final QueryFactory factory;

    /**
     * Constructor allows us to pass in the QueryFactory as well as the
     * appropriate debug system.
     *
     * @param factory Factory used to connect and query the CTS
     * @param myMib Mib file this Entry implementation is a member of
     */
    public CtsTokenOperationsEntryImpl(QueryFactory factory, SnmpMib myMib,
                                       @Named(CoreTokenConstants.CTS_MONITOR_DEBUG) Debug debug) {
        super(myMib);
        this.factory = factory;
        this.debug = debug;
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
        final TokenType token;

        try {
            // -1 as our indexes start at 1, instead of 0.
            token = Enums.getEnumFromOrdinal(TokenType.class, getTokenTableIndex().intValue() - 1);
        } catch (SnmpStatusException e) {
            String message = "Unable to determine token type from supplied index.";
            if (debug.messageEnabled()) {
                debug.error(message, e);
            }
            throw new InvalidSNMPQueryException(message, e);
        }

        if (token == null) {
            String message = "Token type returned was null. Check supplied index.";
            if (debug.messageEnabled()) {
                debug.error(message);
            }
            throw new InvalidSNMPQueryException(message);
        }

        //create the filter to restrict by token type
        final Filter filter = factory.createFilter().and().attribute(CoreTokenField.TOKEN_TYPE, token).build();

        try {

            result = factory.createInstance()
                    .returnTheseAttributes(CoreTokenField.TOKEN_ID)
                    .withFilter(filter)
                    .executeRawResults().size();

        } catch (CoreTokenException e) {
            String message = "CTS Persistence did not return a valid result.";
            if (debug.messageEnabled()) {
                debug.error(message, e);
            }
            throw new InvalidSNMPQueryException(message, e);
        }

        return result;
    }

}
