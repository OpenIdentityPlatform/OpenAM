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

public class ResizableLinkedBlockingQueueTest {

    @Test
    public void testDecreaseQueueSize() throws Exception {
        ResizableLinkedBlockingQueue<Object> queue = new ResizableLinkedBlockingQueue<>(10);

        queue.resizeQueue(6);

        int queueSize = queue.getMaximumQueueSize();
        assertEquals(queueSize, 6, "Expected a decrease to 6 places available.");
    }

    @Test
    public void testIncreaseQueueSize() throws Exception {
        ResizableLinkedBlockingQueue<Object> queue = new ResizableLinkedBlockingQueue<>(10);

        queue.resizeQueue(14);

        int queueSize = queue.getMaximumQueueSize();
        assertEquals(queueSize, 14, "Expected an increase to 14 places available.");
    }
}