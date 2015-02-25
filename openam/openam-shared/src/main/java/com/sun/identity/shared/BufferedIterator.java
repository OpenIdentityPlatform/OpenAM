/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: BufferedIterator.java,v 1.1 2009/04/02 19:41:01 veiming Exp $
 */

/**
 * Portions Copyright 2014 ForgeRock AS
 */

package com.sun.identity.shared;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This iterator allows next method to be called before it has all its elements.
 */
public class BufferedIterator<T> implements Iterator<T> {
    private final BlockingQueue<T> queue = new LinkedBlockingQueue<T>();
    private volatile boolean done = false;

    public void add(T entry) {
        queue.add(entry);
    }

    public void add(List<T> entry) {
        queue.addAll(entry);
    }

    public void isDone() {
        done = true;
    }

    public T next() {
        while (hasNext()) {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new NoSuchElementException();
    }

    public boolean hasNext() {
        return !(done && queue.isEmpty());
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}