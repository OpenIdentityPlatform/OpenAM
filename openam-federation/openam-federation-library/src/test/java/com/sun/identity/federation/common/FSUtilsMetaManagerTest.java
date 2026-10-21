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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.shared.debug.Debug;

/**
 * {@code getIDFFMetaManager} builds the manager on the first call that finds none rather than
 * once at class load, so an SMS outage at startup no longer disables ID-FF until a restart. A
 * failed attempt is remembered for a while, so that the requests arriving during an outage do
 * not each queue on the class monitor to pay their own failing constructor. There is no SMS
 * here: every attempt fails, and writes the failure to the debug log - which is what tells an
 * attempt from none.
 */
public class FSUtilsMetaManagerTest {

    private static final String CREATION_FAILED =
        "FSUtils.getIDFFMetaManager: Could not create meta Manager";

    private IDFFMetaManager originalMetaInstance;
    private long originalFailedAt;
    private Debug originalDebug;
    private Debug debug;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        originalMetaInstance = FSUtils.metaInstance;
        originalFailedAt = MetaManagerFailureFixture.failedAt();
        originalDebug = FSUtils.debug;
        debug = mock(Debug.class);
        FSUtils.debug = debug;
        FSUtils.metaInstance = null;
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            FSUtils.metaInstance = originalMetaInstance;
            MetaManagerFailureFixture.setFailedAt(originalFailedAt);
            FSUtils.debug = originalDebug;
        } finally {
            FederationStaticsLock.release();
        }
    }

    @Test
    public void shouldNotAttemptAgainWithinTheRetryIntervalOfAFailure() throws Exception {
        MetaManagerFailureFixture.setFailedAt(System.currentTimeMillis() - 1000L);

        assertThat(FSUtils.getIDFFMetaManager()).isNull();

        verify(debug, never()).error(eq(CREATION_FAILED), any(Throwable.class));
    }

    @Test
    public void shouldAttemptAgainOnceTheRetryIntervalHasPassed() throws Exception {
        MetaManagerFailureFixture.setFailedAt(System.currentTimeMillis() - 2001L);

        FSUtils.getIDFFMetaManager();

        verify(debug, times(1)).error(eq(CREATION_FAILED), any(Throwable.class));
    }

    @Test
    public void shouldRememberAFailedAttempt() throws Exception {
        MetaManagerFailureFixture.setFailedAt(0L);

        FSUtils.getIDFFMetaManager();
        FSUtils.getIDFFMetaManager();

        verify(debug, times(1)).error(eq(CREATION_FAILED), any(Throwable.class));
    }

    /**
     * A caller that queued on the monitor while another built the manager takes that one rather
     * than building its own.
     */
    @Test
    public void shouldReturnTheManagerBuiltWhileThisCallerWaitedForTheLock() throws Exception {
        MetaManagerFailureFixture.setFailedAt(0L);
        IDFFMetaManager builtMeanwhile = mock(IDFFMetaManager.class);
        AtomicReference<IDFFMetaManager> returned = new AtomicReference<IDFFMetaManager>();
        Thread caller = new Thread(() -> returned.set(FSUtils.getIDFFMetaManager()));

        synchronized (IDFFMetaManager.class) {
            caller.start();
            awaitBlocked(caller);
            FSUtils.metaInstance = builtMeanwhile;
        }
        caller.join(10000L);

        assertThat(returned.get()).isSameAs(builtMeanwhile);
    }

    private static void awaitBlocked(Thread thread) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000L;
        while (thread.getState() != Thread.State.BLOCKED) {
            assertThat(System.currentTimeMillis()).as("caller never queued on the lock")
                .isLessThan(deadline);
            Thread.sleep(1L);
        }
    }
}
