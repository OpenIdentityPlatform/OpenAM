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

package com.sun.identity.authentication.service;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.identity.authentication.spi.AMAuthCallBackImpl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.openam.utils.TimeTravelUtil.FrozenTimeService;
import org.forgerock.util.time.TimeService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@PrepareForTest({AuthD.class, AMAuthCallBackImpl.class})
@SuppressStaticInitializationFor("com.sun.identity.authentication.service.AuthD")
public class AMAccountLockoutTest extends PowerMockTestCase {
    private LoginState state;
    private AMIdentity identity;
    private AuthD auth;
    private String userDN;

    @BeforeMethod
    public void setup() throws Exception {
        FrozenTimeService.INSTANCE.setCurrentTimeMillis(System.currentTimeMillis());
        TimeTravelUtil.setBackingTimeService(FrozenTimeService.INSTANCE);
        PowerMockito.mockStatic(AuthD.class);
        auth = mock(AuthD.class);
        when(AuthD.getAuth()).thenReturn(auth);
        PowerMockito.mockStatic(AMAuthCallBackImpl.class);
        when(AMAuthCallBackImpl.getInstance(anyString())).thenReturn(mock(AMAuthCallBackImpl.class));
        userDN = "id=" + UUID.randomUUID() + ",ou=user,dc=example";
        identity = mock(AMIdentity.class);
        when(identity.getUniversalId()).thenReturn(userDN);
        when(identity.getRealm()).thenReturn("/");
        when(identity.isActive()).thenReturn(true);
        when(identity.getAttributes(anySet())).thenReturn(Collections.emptyMap());
        when(identity.getAttribute(anyString())).thenReturn(Collections.emptySet());
        doThrow(new IdRepoException("Read-only identity store")).when(identity).store();
        when(auth.getIdentity(eq(IdType.USER), anyString(), eq("/"))).thenReturn(identity);
        state = mock(LoginState.class);
        when(state.getOrgDN()).thenReturn("/");
        when(state.getUserDN()).thenReturn(userDN);
        when(state.getLoginFailureLockoutMode()).thenReturn(true);
        when(state.getLoginFailureLockoutStoreInDS()).thenReturn(true);
        when(state.getLoginFailureLockoutCount()).thenReturn(5);
        when(state.getLoginFailureLockoutTime()).thenReturn(MINUTES.toMillis(5));
        when(state.getLoginFailureLockoutDuration()).thenReturn(0L);
        when(state.getLoginFailureLockoutMultiplier()).thenReturn(1);
        when(state.getInvalidAttemptsDataAttrName()).thenReturn("sunAMAuthInvalidAttemptsData");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
    }

    @Test
    public void physicalModeFailureIsEnforcedAcrossFreshRequestsAndAliases() throws Exception {
        failAttempts(5);
        assertTrue(new AMAccountLockout(state).isLockedOut("alice"));
        assertTrue(new AMAccountLockout(state).isLockedOut("alice@example.com"));
        assertTrue(new AMAccountLockout(state).isLockedOut());
        assertTrue(new AMAccountLockout(state).isAccountLocked("alice"));
        verify(identity, times(1)).store();
    }

    @Test
    public void expiredFallbackUnlocksEvenWhenAccountCheckRunsFirst() {
        failAttempts(5);
        FrozenTimeService.INSTANCE.fastForward(31, MINUTES);
        assertFalse(new AMAccountLockout(state).isAccountLocked("alice"));
        assertFalse(new AMAccountLockout(state).isLockedOut());
        failAttempts(4);
        assertFalse(new AMAccountLockout(state).isLockedOut());
        failAttempts(1);
        assertTrue(new AMAccountLockout(state).isLockedOut());
    }

    @Test
    public void successfulLoginResetsTheSameCounterThroughAnAlias() throws Exception {
        failAttempts(3);
        new AMAccountLockout(state).resetPasswdLockout("alice@example.com", true);
        failAttempts(4);
        assertFalse(new AMAccountLockout(state).isLockedOut());
        failAttempts(1);
        assertTrue(new AMAccountLockout(state).isLockedOut());
        verify(identity, times(1)).store();
    }

    @Test
    public void identicalNamesInDifferentRealmsDoNotShareFallback() throws Exception {
        failAttempts(5);
        AMIdentity otherIdentity = mock(AMIdentity.class);
        when(otherIdentity.getUniversalId()).thenReturn("id=alice,ou=user,o=other,dc=example");
        when(otherIdentity.isActive()).thenReturn(true);
        when(otherIdentity.getAttributes(anySet())).thenReturn(Collections.emptyMap());
        when(otherIdentity.getAttribute(anyString())).thenReturn(Collections.emptySet());
        when(auth.getIdentity(eq(IdType.USER), anyString(), eq("/other"))).thenReturn(otherIdentity);
        when(state.getOrgDN()).thenReturn("/other");
        assertFalse(new AMAccountLockout(state).isLockedOut("alice"));
        assertFalse(new AMAccountLockout(state).isAccountLocked("alice"));
    }

    @DataProvider
    public Object[][] durations() {
        return new Object[][] {{0L}, {2L}};
    }

    @Test(dataProvider = "durations")
    public void cachedDirectoryStateDoesNotHideAnotherRequestsFallback(long minutes) {
        when(state.getLoginFailureLockoutDuration()).thenReturn(MINUTES.toMillis(minutes));
        AMAccountLockout staleRequest = new AMAccountLockout(state);
        assertFalse(staleRequest.isAccountLocked("alice"));
        failAttempts(4);
        staleRequest.invalidPasswd("alice");
        assertTrue(staleRequest.isAccountLocked("alice"));
        assertTrue(staleRequest.isLockedOut());
    }

    @Test(dataProvider = "durations")
    public void disabledMultiplierDoesNotDisableTheNextMemoryLock(long minutes) {
        when(state.getLoginFailureLockoutDuration()).thenReturn(MINUTES.toMillis(minutes));
        when(state.getLoginFailureLockoutMultiplier()).thenReturn(0);
        failAttempts(5);
        assertTrue(new AMAccountLockout(state).isLockedOut());
        FrozenTimeService.INSTANCE.fastForward(31, MINUTES);
        assertFalse(new AMAccountLockout(state).isLockedOut());
        failAttempts(5);
        assertTrue(new AMAccountLockout(state).isLockedOut());
    }

    @Test(dataProvider = "durations")
    public void firstFailureLocksWhenThresholdIsOne(long minutes) {
        when(state.getLoginFailureLockoutDuration()).thenReturn(MINUTES.toMillis(minutes));
        when(state.getLoginFailureLockoutCount()).thenReturn(1);
        failAttempts(1);
        assertTrue(new AMAccountLockout(state).isLockedOut());
    }

    @Test
    public void fallbackStillHonorsACustomPhysicalLock() throws Exception {
        when(state.getLoginLockoutAttrName()).thenReturn("customLock");
        when(state.getLoginLockoutAttrValue()).thenReturn("blocked");
        failAttempts(5);
        FrozenTimeService.INSTANCE.fastForward(31, MINUTES);
        when(identity.getAttribute("customLock")).thenReturn(Collections.singleton("blocked"));
        assertTrue(new AMAccountLockout(state).isAccountLocked("alice"));
    }

    @Test
    public void fallbackStillHonorsAnAdministrativelyDisabledAccount() throws Exception {
        failAttempts(5);
        FrozenTimeService.INSTANCE.fastForward(31, MINUTES);
        when(identity.isActive()).thenReturn(false);
        assertTrue(new AMAccountLockout(state).isAccountLocked("alice"));
    }

    @Test
    public void delayedExpiryResetCannotEraseANewLockoutCycle() throws Exception {
        failAttempts(5);
        FrozenTimeService.INSTANCE.fastForward(31, MINUTES);
        CountDownLatch resetting = new CountDownLatch(1);
        CountDownLatch releaseReset = new CountDownLatch(1);
        AtomicInteger lookups = new AtomicInteger();
        when(auth.getIdentity(eq(IdType.USER), anyString(), eq("/"))).thenAnswer(call -> {
            if (Thread.currentThread().getName().equals("delayed-expiry") && lookups.incrementAndGet() == 2) {
                resetting.countDown();
                assertTrue(releaseReset.await(10, TimeUnit.SECONDS));
            }
            return identity;
        });
        ExecutorService delayed = Executors.newSingleThreadExecutor(
                task -> new Thread(task, "delayed-expiry"));
        ExecutorService next = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> expiry = delayed.submit(
                    () -> new AMAccountLockout(state).isLockedOut("alice"));
            assertTrue(resetting.await(10, TimeUnit.SECONDS));
            Future<?> newCycle = next.submit(() -> {
                new AMAccountLockout(state).isLockedOut("alice");
                failAttempts(5);
            });
            try {
                newCycle.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException expected) {
                // A serialized expiry reset holds the counter until releaseReset.
            }
            releaseReset.countDown();
            assertFalse(expiry.get(10, TimeUnit.SECONDS));
            newCycle.get(10, TimeUnit.SECONDS);
            assertTrue(new AMAccountLockout(state).isLockedOut("alice"));
        } finally {
            releaseReset.countDown();
            delayed.shutdownNow();
            next.shutdownNow();
        }
    }

    private void failAttempts(int count) {
        for (int i = 0; i < count; i++) {
            new AMAccountLockout(state).invalidPasswd("alice");
        }
    }
}
