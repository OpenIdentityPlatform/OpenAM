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
package org.forgerock.openam.cts.monitoring.impl.persistence;

import org.forgerock.openam.monitoring.cts.OperationType;
import org.forgerock.openam.utils.Enums;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Used to verify the ordering of the OperationType enums.
 *
 * This is important, as they MUST stay in the defined order, else
 * our OID references as exposed via SNMP will be incorrect.
 */
public class OperationTypeTest {

    @Test
    public void verifyOperationTypeOrder() {
        OperationType ot0 = Enums.getEnumFromOrdinal(OperationType.class, 0);
        OperationType ot1 = Enums.getEnumFromOrdinal(OperationType.class, 1);
        OperationType ot2 = Enums.getEnumFromOrdinal(OperationType.class, 2);
        OperationType ot3 = Enums.getEnumFromOrdinal(OperationType.class, 3);
        OperationType ot4 = Enums.getEnumFromOrdinal(OperationType.class, 4);

        assertEquals(ot0, OperationType.CREATE);
        assertEquals(ot1, OperationType.READ);
        assertEquals(ot2, OperationType.UPDATE);
        assertEquals(ot3, OperationType.DELETE);
        assertEquals(ot4, OperationType.LIST);
    }

}
