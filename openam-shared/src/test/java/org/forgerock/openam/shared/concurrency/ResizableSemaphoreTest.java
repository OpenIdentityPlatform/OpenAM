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