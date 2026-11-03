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

package com.sun.identity.common;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.identity.authentication.spi.AMAuthCallBackImpl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.openam.utils.TimeTravelUtil.FrozenTimeService;
import org.forgerock.util.time.TimeService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

@PrepareForTest(AMAuthCallBackImpl.class)
public class ISAccountLockoutTest extends PowerMockTestCase {
    private static final String ATTRIBUTE = "sunAMAuthInvalidAttemptsData";
    private String userDN;
    private AMIdentity identity;
    private Map<String, Set<String>> stored;
    private Map<String, Set<String>> pending;
    private boolean rejectWrites;
    private int writes;
    private Debug originalDebug;

    @BeforeMethod
    public void setup() throws Exception {
        userDN = "id=" + UUID.randomUUID() + ",ou=user,dc=example";
        FrozenTimeService.INSTANCE.setCurrentTimeMillis(System.currentTimeMillis());
        TimeTravelUtil.setBackingTimeService(FrozenTimeService.INSTANCE);
        originalDebug = ISAccountLockout.debug;
        ISAccountLockout.debug = mock(Debug.class);
        PowerMockito.mockStatic(AMAuthCallBackImpl.class);
        when(AMAuthCallBackImpl.getInstance(anyString())).thenReturn(mock(AMAuthCallBackImpl.class));
        stored = new HashMap<>();
        pending = new HashMap<>();
        rejectWrites = true;
        writes = 0;
        identity = mock(AMIdentity.class);
        when(identity.getRealm()).thenReturn("/");
        when(identity.getAttribute(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
        doAnswer(call -> { pending.putAll(call.getArgument(0)); return null; }).when(identity).setAttributes(anyMap());
        doAnswer(call -> {
            writes++;
            if (rejectWrites) {
                throw new IdRepoException("Read-only identity store");
            }
            stored.putAll(pending);
            pending.clear();
            return null;
        }).when(identity).store();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
        ISAccountLockout.debug = originalDebug;
        ISAccountLockout.loginFailHash.remove(userDN);
    }

    @Test
    public void failedWriteRetainsFirstAttemptAcrossInstances() {
        failAttempt(10);
        assertEquals(read(10).getFailCount(), 1);
        assertFalse(lockout(10).isLockedOut(read(10)));
    }

    @Test
    public void repeatedFailuresLockWithoutRetryingTheStore() {
        failAttempts(5, 10);
        assertEquals(read(10).getFailCount(), 5);
        assertTrue(lockout(10).isLockedOut(read(10)));
        assertEquals(writes, 1);
        verify(ISAccountLockout.debug, times(1)).error(anyString(), any(Throwable.class));
    }

    @Test
    public void failureAtThresholdRetainsPersistedHistory() {
        rejectWrites = false;
        failAttempts(4, 10);
        rejectWrites = true;
        failAttempt(10);
        assertEquals(read(10).getFailCount(), 5);
        assertTrue(lockout(10).isLockedOut(read(10)));
        assertEquals(writes, 5);
    }

    @Test
    public void zeroDurationFallsBackToThirtyMinutesWithoutPhysicalLocking() {
        failAttempts(5, 0);
        assertTrue(lockout(0).isLockedOut(read(0)));
        FrozenTimeService.INSTANCE.fastForward(29, MINUTES);
        assertTrue(lockout(0).isLockedOut(read(0)));
        FrozenTimeService.INSTANCE.fastForward(2, MINUTES);
        assertFalse(lockout(0).isLockedOut(read(0)));
        assertEquals(writes, 1);
        assertFalse(pending.containsKey("inetuserstatus"));
    }

    @Test
    public void positiveDurationIsPreserved() {
        failAttempts(5, 2);
        FrozenTimeService.INSTANCE.fastForward(1, MINUTES);
        assertTrue(lockout(2).isLockedOut(read(2)));
        FrozenTimeService.INSTANCE.fastForward(2, MINUTES);
        assertFalse(lockout(2).isLockedOut(read(2)));
    }

    @Test
    public void successfulLoginResetsMemoryWithoutRetryingTheStore() {
        failAttempts(3, 10);
        lockout(10).resetLockoutAttempts(userDN, identity, read(10), true);
        failAttempt(10);
        assertEquals(read(10).getFailCount(), 1);
        assertFalse(lockout(10).isLockedOut(read(10)));
        assertEquals(writes, 1);
    }

    @Test
    public void resetPreservesDefaultFallbackDuration() {
        failAttempts(5, 0);
        FrozenTimeService.INSTANCE.fastForward(31, MINUTES);
        lockout(0).resetLockoutAttempts(userDN, identity, read(0), true);
        failAttempts(5, 0);
        assertTrue(lockout(0).isLockedOut(read(0)));
        FrozenTimeService.INSTANCE.fastForward(31, MINUTES);
        assertFalse(lockout(0).isLockedOut(read(0)));
        assertEquals(writes, 1);
    }

    @Test
    public void anotherIdentityStillUsesTheStore() {
        failAttempt(10);
        rejectWrites = false;
        String other = "id=other,ou=user,dc=elsewhere";
        ISAccountLockout lockout = lockout(10);
        lockout.invalidPasswd(other, "other", identity, lockout.getAcInfo(other, identity));
        assertEquals(writes, 2);
        assertNotNull(stored.get(ATTRIBUTE));
        assertEquals(read(10).getFailCount(), 1);
    }

    @Test
    public void writableStoreKeepsPersistingAttempts() {
        rejectWrites = false;
        failAttempts(5, 10);
        assertTrue(lockout(10).isLockedOut(read(10)));
        assertEquals(writes, 5);
    }

    @Test
    public void configuredMemoryModeStillCountsAcrossInstances() {
        for (int i = 0; i < 5; i++) {
            ISAccountLockout lockout = lockout(10);
            lockout.setStoreInvalidAttemptsInDS(false);
            lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        }
        ISAccountLockout lockout = lockout(10);
        lockout.setStoreInvalidAttemptsInDS(false);
        assertTrue(lockout.isLockedOut(lockout.getAcInfo(userDN, identity)));
        assertEquals(writes, 0);
    }

    @Test
    public void staleSnapshotsDoNotLoseConcurrentFallbackAttempts() throws Exception {
        AccountLockoutInfo stale = read(10);
        failAttempt(10);
        CountDownLatch counted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger debugCalls = new AtomicInteger();
        when(ISAccountLockout.debug.messageEnabled()).thenAnswer(call -> {
            // The second call is after reading and incrementing the local fail_count.
            if (Thread.currentThread().getName().equals("first-increment")
                    && debugCalls.incrementAndGet() == 2) {
                counted.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return false;
        });
        ExecutorService first = Executors.newSingleThreadExecutor(task -> new Thread(task, "first-increment"));
        Thread[] secondThread = new Thread[1];
        ExecutorService second = Executors.newSingleThreadExecutor(task -> {
            secondThread[0] = new Thread(task, "second-increment");
            return secondThread[0];
        });
        try {
            Future<?> one = first.submit(() -> lockout(10).invalidPasswd(userDN, "alice", identity, stale));
            assertTrue(counted.await(10, TimeUnit.SECONDS));
            Future<?> two = second.submit(() -> lockout(10).invalidPasswd(userDN, "alice", identity, stale));
            // Wait for actual contention, or completion if the monitor was removed.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean contended = false;
            while (!two.isDone() && System.nanoTime() < deadline) {
                StackTraceElement[] stack = secondThread[0].getStackTrace();
                if (secondThread[0].getState() == Thread.State.BLOCKED && stack.length > 0
                        && stack[0].getClassName().equals(ISAccountLockout.class.getName())
                        && stack[0].getMethodName().equals("invalidPasswd")) {
                    contended = true;
                    break;
                }
                Thread.sleep(1);
            }
            assertTrue(contended || two.isDone(), "Second increment did not reach the counter");
            release.countDown();
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
            assertEquals(read(10).getFailCount(), 3);
            assertEquals(writes, 1);
        } finally {
            release.countDown();
            first.shutdownNow();
            assertTrue(first.awaitTermination(10, TimeUnit.SECONDS));
            second.shutdownNow();
            assertTrue(second.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void failedResetWriteSwitchesToMemoryWithAClearedCounter() {
        rejectWrites = false;
        failAttempts(4, 10);
        rejectWrites = true;
        lockout(10).resetLockoutAttempts(userDN, identity, read(10), true);
        assertEquals(read(10).getFailCount(), 0);
        failAttempt(10);
        assertEquals(read(10).getFailCount(), 1);
        assertEquals(writes, 5);
    }

    @Test
    public void failedReadAndWriteStillCountInMemory() throws Exception {
        when(identity.getAttribute(anyString())).thenThrow(new IdRepoException("Unavailable counter"));
        failAttempts(5, 10);
        assertTrue(lockout(10).isLockedOut(read(10)));
        assertEquals(writes, 1);
        verify(identity, times(1)).getAttribute(ATTRIBUTE);
    }

    @Test
    public void simultaneousFirstWriteFailuresPreserveEveryAttempt() throws Exception {
        AMIdentity concurrentIdentity = mock(AMIdentity.class);
        when(concurrentIdentity.getRealm()).thenReturn("/");
        CyclicBarrier writing = new CyclicBarrier(5);
        doAnswer(call -> {
            writing.await(10, TimeUnit.SECONDS);
            throw new IdRepoException("Read-only identity store");
        }).when(concurrentIdentity).store();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 5; i++) {
                futures.add(executor.submit(() -> {
                    ISAccountLockout lockout = lockout(0);
                    lockout.invalidPasswd(userDN, "alice", concurrentIdentity,
                            lockout.getAcInfo(userDN, concurrentIdentity));
                }));
            }
            for (Future<?> future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }
            assertEquals(read(0).getFailCount(), 5);
            assertTrue(lockout(0).isLockedOut(read(0)));
            verify(ISAccountLockout.debug, times(1)).error(anyString(), any(Throwable.class));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void writableStoreRetainsPhysicalLocking() {
        rejectWrites = false;
        failAttempts(5, 0);
        assertTrue(lockout(0).isAccountLocked(identity));
        assertEquals(writes, 6);
    }

    @Test
    public void zeroConfiguredDurationOverridesAnOldPersistedTimedDuration() {
        rejectWrites = false;
        failAttempts(4, 2);
        rejectWrites = true;
        failAttempt(0);
        FrozenTimeService.INSTANCE.fastForward(3, MINUTES);
        assertTrue(lockout(0).isLockedOut(read(0)));
        FrozenTimeService.INSTANCE.fastForward(28, MINUTES);
        assertFalse(lockout(0).isLockedOut(read(0)));
    }

    @DataProvider
    public Object[][] notificationStages() {
        return new Object[][] {{false, false}, {true, false}, {false, true}, {true, true}};
    }

    @Test(dataProvider = "notificationStages")
    public void blockedNotificationDoesNotBlockCounterReaders(boolean callback, boolean transition) throws Exception {
        rejectWrites = !transition;
        failAttempts(4, 10);
        rejectWrites = true;
        ISAccountLockout triggering = spy(lockout(10));
        AMAuthCallBackImpl plugin = AMAuthCallBackImpl.getInstance("/");
        CountDownLatch notifying = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        org.mockito.stubbing.Answer<Void> block = call -> {
            notifying.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            return null;
        };
        if (callback) {
            doAnswer(block).when(plugin).processedAccounttLockout(any(Long.class), eq("alice"));
        } else {
            doAnswer(block).when(triggering).sendLockOutNotice("alice");
        }
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> trigger = executor.submit(() -> triggering.invalidPasswd(
                    userDN, "alice", identity, triggering.getAcInfo(userDN, identity)));
            assertTrue(notifying.await(10, TimeUnit.SECONDS));
            Future<Boolean> reader = executor.submit(() -> lockout(10).isLockedOut(read(10)));
            assertTrue(reader.get(2, TimeUnit.SECONDS), "The lock must be visible before notification completes");
            release.countDown();
            trigger.get(10, TimeUnit.SECONDS);
            triggering.invalidPasswd(userDN, "alice", identity, read(10));
            verify(triggering, times(1)).sendLockOutNotice("alice");
            verify(plugin, times(1)).processedAccounttLockout(any(Long.class), eq("alice"));
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void writableStoreLocksOnTheFirstFailureWhenThresholdIsOne() {
        rejectWrites = false;
        ISAccountLockout lockout = lockout(10, 1, 1);
        lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        assertTrue(lockout.isLockedOut(lockout.getAcInfo(userDN, identity)));
        assertEquals(writes, 1);
    }

    @DataProvider
    public Object[][] healthyNotificationSettings() {
        return new Object[][] {{true, 0L}, {true, 10L}, {false, 10L}};
    }

    @Test(dataProvider = "healthyNotificationSettings")
    public void healthyStoreAndConfiguredMemoryNotifyOnceAtTheThreshold(boolean persist, long minutes)
            throws Exception {
        rejectWrites = false;
        ISAccountLockout lockout = spy(lockout(minutes));
        lockout.setStoreInvalidAttemptsInDS(persist);
        doNothing().when(lockout).sendLockOutNotice("alice");
        AMAuthCallBackImpl plugin = AMAuthCallBackImpl.getInstance("/");
        for (int i = 0; i < 4; i++) {
            lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        }
        verify(lockout, never()).sendLockOutNotice(anyString());
        verifyZeroInteractions(plugin);
        long lockedAt = FrozenTimeService.INSTANCE.now();
        lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        if (minutes == 0) {
            assertTrue(lockout.isAccountLocked(identity));
        } else {
            assertTrue(lockout.isLockedOut(lockout.getAcInfo(userDN, identity)));
        }
        lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        verify(lockout, times(1)).sendLockOutNotice("alice");
        verify(plugin, times(1)).processedAccounttLockout(eq(lockedAt), eq("alice"));
        assertEquals(writes, persist ? (minutes == 0 ? 7 : 6) : 0);
    }

    @Test
    public void writableStoreKeepsTheDurationWhenTheMultiplierIsZero() {
        rejectWrites = false;
        ISAccountLockout lockout = lockout(10, 2, 0);
        for (int i = 0; i < 2; i++) {
            lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        }
        FrozenTimeService.INSTANCE.fastForward(11, MINUTES);
        lockout.resetLockoutAttempts(userDN, identity, lockout.getAcInfo(userDN, identity), false);
        assertEquals(lockout.getAcInfo(userDN, identity).getActualLockoutDuration(), MINUTES.toMillis(10));
        for (int i = 0; i < 2; i++) {
            lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        }
        assertTrue(lockout.isLockedOut(lockout.getAcInfo(userDN, identity)));
    }

    @Test
    public void multiplierAppliesOnceWhenTheThresholdWriteFails() {
        rejectWrites = false;
        for (int i = 0; i < 5; i++) {
            ISAccountLockout lockout = lockout(10, 5, 2);
            lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        }
        FrozenTimeService.INSTANCE.fastForward(11, MINUTES);
        for (int i = 0; i < 4; i++) {
            ISAccountLockout lockout = lockout(10, 5, 2);
            lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        }
        rejectWrites = true;
        ISAccountLockout lockout = lockout(10, 5, 2);
        lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
        assertEquals(read(10).getActualLockoutDuration(), MINUTES.toMillis(20));
    }

    @Test
    public void readFailureAloneKeepsPersisting() throws Exception {
        rejectWrites = false;
        when(identity.getAttribute(anyString()))
                .thenThrow(new IdRepoException("Unavailable"))
                .thenAnswer(call -> stored.get(call.getArgument(0)));
        failAttempt(10);
        failAttempt(10);
        assertEquals(writes, 2);
        assertEquals(read(10).getFailCount(), 2);
    }

    private void failAttempts(int attempts, long minutes) {
        for (int i = 0; i < attempts; i++) {
            failAttempt(minutes);
        }
    }

    private void failAttempt(long minutes) {
        ISAccountLockout lockout = lockout(minutes);
        lockout.invalidPasswd(userDN, "alice", identity, lockout.getAcInfo(userDN, identity));
    }

    private AccountLockoutInfo read(long minutes) {
        return lockout(minutes).getAcInfo(userDN, identity);
    }

    private ISAccountLockout lockout(long minutes) {
        return lockout(minutes, 5, 1);
    }

    private ISAccountLockout lockout(long minutes, int count, int multiplier) {
        return new ISAccountLockout(true, MINUTES.toMillis(5), count, null, 3, null, null,
                MINUTES.toMillis(minutes), multiplier, ATTRIBUTE, "amAuth");
    }
}
