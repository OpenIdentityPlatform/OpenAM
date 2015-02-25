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
package org.forgerock.openam.shared.concurrency;

import java.util.concurrent.locks.Lock;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test exercises the functionality of {@link LockFactory}.
 */
public class LockFactoryTest {

    @Test
    public void ensureLocksAreUnique() {
        LockFactory<String> stringLockFactory = new LockFactory<String>();

        Lock lockA = stringLockFactory.acquireLock("abc");
        Lock lockB = stringLockFactory.acquireLock("def");

        assertThat(lockA).isNotEqualTo(lockB);

        LockFactory<Integer> intLockFactory = new LockFactory<Integer>();

        Lock lockC = intLockFactory.acquireLock(5);
        Lock lockD = intLockFactory.acquireLock(10);

        assertThat(lockC).isNotEqualTo(lockD);
    }

    @Test
    public void ensureLocksAreShared() {
        LockFactory<String> stringLockFactory = new LockFactory<String>();

        // New string to force new string instances.
        Lock lockA = stringLockFactory.acquireLock("xyz");
        Lock lockB = stringLockFactory.acquireLock("xyz");

        assertThat(lockA).isEqualTo(lockB);

        LockFactory<Integer> intLockFactory = new LockFactory<Integer>();

        // New integers to force new integer instances.
        Lock lockC = intLockFactory.acquireLock(new Integer(5));
        Lock lockD = intLockFactory.acquireLock(new Integer(5));

        assertThat(lockC).isEqualTo(lockD);
    }

    /**
     * Though this test passes locally it has been disable due to the nature of garbage
     * collection and that a call to {@link System#gc()} does not make any guarantees.
     */
    @Test(enabled = false)
    public void verifyCleanUp() {
        LockFactory<String> lockFactory = new LockFactory<String>();

        Lock lockA = lockFactory.acquireLock("abc");
        Lock lockB = lockFactory.acquireLock("abc");

        assertThat(lockA).isEqualTo(lockB);

        int hashCodeA = lockA.hashCode();
        int hashCodeB = lockB.hashCode();

        assertThat(hashCodeA).isEqualTo(hashCodeB);

        // Destroy strong references.
        lockA = null;
        lockB = null;

        // Invoke the garage collector to clean up dead objects.
        System.gc();

        Lock lockC = lockFactory.acquireLock("abc");
        int hashCodeC = lockC.hashCode();

        // Verifies that the factory cleaned itself after all strong references were removed.
        assertThat(hashCodeA).isEqualTo(hashCodeB).isNotEqualTo(hashCodeC);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void verifyDisallowNullKey() {
        LockFactory<String> lockFactory = new LockFactory<String>();
        lockFactory.acquireLock(null);
    }
}
