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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.shared.concurrency;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ResizableSemaphoreTest {

    private ResizableSemaphore semaphore;

    @Test
    public void testReducePermits() throws Exception {
        semaphore = new ResizableSemaphore(10);

        semaphore.reducePermits(6);

        int availablePermits = semaphore.availablePermits();
        assertEquals(availablePermits, 4, "Expected a decrease to 4 permits available.");
    }

    @Test
    public void testIncreasePermits() throws Exception {
        semaphore = new ResizableSemaphore(10);

        semaphore.increasePermits(4);

        int availablePermits = semaphore.availablePermits();
        assertEquals(availablePermits, 14, "Expected an increase to 14 permits available.");
    }
}