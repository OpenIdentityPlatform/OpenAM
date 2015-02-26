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
 * $Id: CacheTaboo.java,v 1.2 2009/12/12 00:03:13 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class maintains a set of resource and subject keys that will never
 * be cached.
 */
public final class CacheTaboo {
    private static Map<String, Set<String>> tabooed = new
        HashMap<String, Set<String>>();
    private static final ReadWriteLock rwlock = new ReentrantReadWriteLock();

    private CacheTaboo() {
    }

    public static boolean isEmpty() {
        Lock lock = rwlock.readLock();
        try {
            lock.lock();
            return tabooed.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public static boolean isTaboo(String cache, String key) {
        Lock lock = rwlock.readLock();
        try {
            lock.lock();
            Set<String> set = tabooed.get(cache);
            if (set == null) {
                return false;
            }
            return set.contains(key);
        } finally {
            lock.unlock();
        }
    }

    public static void taboo(String cache, String key) {
        Lock lock = rwlock.writeLock();
        try {
            lock.lock();
            Set<String> set = tabooed.get(cache);
            if (set == null) {
                set = new HashSet<String>();
                tabooed.put(cache, set);
            }
            set.add(key);
        } finally {
            lock.unlock();
        }
    }

    public static void reset() {
        tabooed.clear();
    }
}
