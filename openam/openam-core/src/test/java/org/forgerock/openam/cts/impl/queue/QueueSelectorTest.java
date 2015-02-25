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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.queue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class QueueSelectorTest {

    private String tokenId;

    @BeforeMethod
    public void setup() {
        tokenId = "badger";
    }

    @Test
    public void shouldBeConsistentWithTokens() {
        assertThat(QueueSelector.select(tokenId, 2)).isEqualTo(QueueSelector.select(tokenId, 2));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldEnforceQueuesAsPositive() {
        QueueSelector.select(tokenId, 0);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotSupportNullToken() {
        QueueSelector.select(null, 2);
    }

    @Test
    public void shouldNotReturnNegativeNumber() {
        for (int ii = 0; ii < 1000; ii++) {
            int select = QueueSelector.select(Integer.toString(ii), 32);
            assertThat(select).isGreaterThanOrEqualTo(0);
        }
    }
}