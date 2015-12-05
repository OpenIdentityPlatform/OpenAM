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

import org.forgerock.util.Reject;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link LinkedBlockingQueue} implementation which performs exactly as a standard {@link LinkedBlockingQueue}, but
 * which supports 'resizing' of the queue too.
 *
 * @param <E> The type of elements held in this collection.
 *
 * @since 13.0.0
 */
public class ResizableLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

    private int queueSize;
    private final ResizableSemaphore availablePlaces;

    /**
     * Creates a {@code ResizableLinkedBlockingQueue} with a capacity of
     * {@link Integer#MAX_VALUE}.
     */
    public ResizableLinkedBlockingQueue() {
        super();
        queueSize = Integer.MAX_VALUE;
        availablePlaces = new ResizableSemaphore(queueSize, true);
    }

    /**
     * Creates a {@code ResizableLinkedBlockingQueue} with a capacity of
     * {@link Integer#MAX_VALUE}, initially containing the elements of the
     * given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ResizableLinkedBlockingQueue(Collection<? extends E> c) {
        super(c);
        queueSize = Integer.MAX_VALUE;
        availablePlaces = new ResizableSemaphore(queueSize, true);
    }

    /**
     * Creates a {@code ResizableLinkedBlockingQueue} with the given number of
     * permits.
     *
     * @param initialCapacity the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     */
    public ResizableLinkedBlockingQueue(int initialCapacity) {
        super();
        queueSize = initialCapacity;
        availablePlaces = new ResizableSemaphore(initialCapacity, true);
    }

    /**
     * Set the queue to a size between 0 and Integer.MAX_VALUE. If the newSize supplied is a negative integer, then
     * a {@link IllegalArgumentException} will be thrown instead.
     *
     * @param newSize The new size of the queue.
     * @throws IllegalArgumentException thrown when the given newSize is a minus number.
     */
    public synchronized void resizeQueue(int newSize) throws IllegalArgumentException {
        if (newSize < 0) {
            throw new IllegalArgumentException("Cannot set queue size to a value below zero.");
        }

        int difference;

        if (newSize < queueSize) {
            difference = queueSize - newSize;
            availablePlaces.reducePermits(difference);
        } else if (newSize > queueSize) {
            difference = newSize - queueSize;
            availablePlaces.increasePermits(difference);
        }
        queueSize = newSize;

        //Nothing to do if newSize == queueSize.
    }

    /**
     * Get the current maximum capacity of the queue. This is not how many elements are currently in it, but how
     * many would be in it if it were full.
     *
     * @return The current maximum size of the queue.
     */
    public int getMaximumQueueSize() {
        return queueSize;
    }

    @Override
    public boolean offer(E e) {
        Reject.ifNull(e, "Element to offer cannot be null.");

        boolean returnValue;

        if (availablePlaces.tryAcquire()) {
            if (super.offer(e)) {
                returnValue = true;
            } else {
                returnValue = false;
                //If the queue.offer(e) fails, the availablePlaces.tryAcquire() will still have occurred, so we'll have
                //to release the acquired permit.
                availablePlaces.release();
            }
        } else {
            returnValue = false;
        }

        return returnValue;
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        Reject.ifNull(e, "Element to offer cannot be null.");

        boolean returnValue;

        if (availablePlaces.tryAcquire(timeout, unit)) {
            if (super.offer(e)) {
                returnValue = true;
            } else {
                returnValue = false;
                //If the queue.offer(e) fails, the availablePlaces.tryAcquire(timeout, unit) will still have occurred,
                //so we'll have to release the acquired permit.
                availablePlaces.release();
            }
        } else {
            returnValue = false;
        }

        return returnValue;
    }

    @Override
    public E poll() {
        E returnValue = super.poll();
        if (returnValue != null) {
            availablePlaces.release();
        }
        return returnValue;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E resultValue = super.poll(timeout, unit);

        if (resultValue != null) {
            availablePlaces.release();
        }
        return resultValue;
    }

    @Override
    public void put(E e) throws InterruptedException {
        availablePlaces.acquire();
        super.put(e);
    }

    @Override
    public int remainingCapacity() {
        return availablePlaces.availablePermits();
    }

    @Override
    public boolean remove(Object o) {
        if (super.remove(o)) {
            availablePlaces.release();
            return true;
        }

        return false;
    }

    @Override
    public E take() throws InterruptedException {
        E result = super.take();
        availablePlaces.release();
        return result;
    }

}
