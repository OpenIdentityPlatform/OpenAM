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

package org.forgerock.openam.cts.api;

import org.forgerock.openam.utils.Enums;

/**
 * Responsible for defining the available token types in the Core Token Service.
 *
 * If new tokens are added, this enum must be updated via APPENDING to the end of the enum list.
 *
 * Existing operations MUST STAY in the order they are defined. This is validated by TokenTypeTest.
 *
 * @author Robert Wapshott
 */
public enum TokenType {

    SESSION,
    SAML2,
    OAUTH,
    REST,
    GENERIC;

    /**
     * Retrieves the appropriate TokenType from the list of avaliable
     * enums that matches on the ordinal index.
     *
     * @param ordinalIndex the ordinal index to look up
     * @return the TokenType this ordinal value represents, null otherwise
     */
    public static TokenType getTokenFromOrdinalIndex(int ordinalIndex) {
        return Enums.getEnumFromOrdinal(TokenType.class, ordinalIndex);
    }

}
