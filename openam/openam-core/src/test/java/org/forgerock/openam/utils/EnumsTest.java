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

package org.forgerock.openam.utils;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class EnumsTest {

    @Test
    public void testGetEnumFromOrdinal() {
        TestEnum e = Enums.getEnumFromOrdinal(TestEnum.class, 0);
        assertEquals(e, TestEnum.A);
    }

    @Test
    public void testGetEnumFromOrdinalFailBounds() {
        TestEnum e = Enums.getEnumFromOrdinal(TestEnum.class, 4);
        assertEquals(e, null);
    }

    /**
     * Private enum only used for running these tests.
     */
    private enum TestEnum {
        A(), B(), C();
    }

}
