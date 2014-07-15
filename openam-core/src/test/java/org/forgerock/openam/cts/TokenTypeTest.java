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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts;

import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.utils.Enums;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Used to verify the ordering of the TokenType enums.
 *
 * This is important, as they MUST stay in the defined order, else
 * our OID references as exposed via SNMP will be incorrect.
 */
public class TokenTypeTest {

    @Test
    public void verifyOperationTypeOrder() {
        TokenType tt0 = Enums.getEnumFromOrdinal(TokenType.class, 0);
        TokenType tt1 = Enums.getEnumFromOrdinal(TokenType.class, 1);
        TokenType tt2 = Enums.getEnumFromOrdinal(TokenType.class, 2);
        TokenType tt3 = Enums.getEnumFromOrdinal(TokenType.class, 3);

        assertEquals(tt0, TokenType.SESSION);
        assertEquals(tt1, TokenType.SAML2);
        assertEquals(tt2, TokenType.OAUTH);
        assertEquals(tt3, TokenType.REST);
    }

}
