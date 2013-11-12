/**
 * Copyright 2013 ForgeRock, Inc.
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
package org.forgerock.openam.cts.exceptions;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;

import java.text.MessageFormat;

/**
 * Indicates that a query operation has failed.
 *
 * @author robert.wapshott@forgerock.com
 */
public class QueryFailedException extends CoreTokenException {
    /**
     * Creates a formatted exception based on the values provided.
     *
     * @param connection Connection used to make the query.
     * @param dn May be null. DN which was used in the query.
     * @param filter May be null. Filter used in query.
     * @param cause Cause of the query failure.
     */
    public QueryFailedException(Connection connection, DN dn, Filter filter, Throwable cause) {
        super(MessageFormat.format(
                    "\n" +
                    CoreTokenConstants.DEBUG_HEADER+
                    "Failed to complete query:\n" +
                    "      DN: {0}\n" +
                    "    Conn: {1}\n" +
                    "  Filter: {2}",
                    dn,
                    connection,
                    filter),
                cause);
    }
}
