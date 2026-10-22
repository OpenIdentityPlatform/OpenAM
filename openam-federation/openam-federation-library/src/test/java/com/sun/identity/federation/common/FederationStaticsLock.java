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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.federation.common;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Serialises the tests that swap process-wide statics - {@code FSUtils.metaInstance},
 * {@code FSUtils.deploymentURI}, {@code SessionManager.sp} - around the call they exercise.
 * <p>
 * Those swaps are visible to every thread in the JVM, so two of these tests running at once
 * would see each other's fixture. They are deterministic today only because surefire runs
 * this module single-threaded; that is a build setting, not a property of the tests, and the
 * {@code SkipException} these replaced was the previous version of the same hazard. Take the
 * lock in {@code @BeforeMethod} and release it in {@code @AfterMethod}: TestNG runs both on
 * the thread that runs the test method, so a {@link ReentrantLock} holds across the three.
 */
public final class FederationStaticsLock {

    private static final ReentrantLock LOCK = new ReentrantLock();

    private FederationStaticsLock() {
    }

    /** Takes the lock. Call from {@code @BeforeMethod}. */
    public static void acquire() {
        LOCK.lock();
    }

    /** Releases the lock if this thread holds it. Call from {@code @AfterMethod}. */
    public static void release() {
        if (LOCK.isHeldByCurrentThread()) {
            LOCK.unlock();
        }
    }
}
