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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.cts;

import org.forgerock.openam.utils.Enums;

/**
 * An Enum which contains constants for each of the possible CTS operations.
 *
 * @since 12.0.0
 */
public enum CTSOperation {

    /**
     * CTS Create Operation.
     */
    CREATE,
    /**
     * CTS Read Operation.
     */
    READ,
    /**
     * CTS Update Operation.
     */
    UPDATE,
    /**
     * CTS Delete Operation.
     */
    DELETE,
    /**
     * CTS List/Query Operation.
     */
    LIST;

    /**
     * Retrieves the appropriate CTSOperation from the list of available enums that matches on the ordinal index.
     *
     * @param ordinalIndex the ordinal index to look up
     * @return the CTSOperation this ordinal value represents, null otherwise
     */
    public static CTSOperation getOperationFromOrdinalIndex(int ordinalIndex) {
        return Enums.getEnumFromOrdinal(CTSOperation.class, ordinalIndex);
    }
}
