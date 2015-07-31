package org.forgerock.openam.shared.concurrency;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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