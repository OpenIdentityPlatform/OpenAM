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

package com.sun.identity.shared;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This iterator allows next method to be called before it has all its elements.
 */
public class BufferedIterator implements Iterator {
    private List queue = new LinkedList();
    private boolean done = false;
    private Object lock = new Object();

    public void add(Object entry) {
        queue.add(entry);
        synchronized (lock) {
            lock.notify();
        }
    }

    public void add(List entry) {
        queue.addAll(entry);
        synchronized (lock) {
            lock.notify();
        }
    }

    public void isDone() {
        done = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    public Object next() {
        return queue.remove(0);
    }

    public boolean hasNext() {
        synchronized (lock) {
            if (queue.isEmpty() && !done) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    done = true;
                }
            }
        }

        return !queue.isEmpty();
    }

    public void remove() {
        //not supported.
    }
}

