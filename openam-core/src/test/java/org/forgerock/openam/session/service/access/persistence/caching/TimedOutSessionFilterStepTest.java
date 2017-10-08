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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.session.service.access.persistence.caching;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceException;
import org.forgerock.openam.session.service.access.persistence.TimeOutSessionFilterStep;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

public class TimedOutSessionFilterStepTest {

    private static String TIMED_OUT_SESSSION_ID = "timedouthandle";
    private static String NOT_TIMED_OUT_SESSSION_ID = "nottimedouthandle";
    private static String INVALID_SESSSION_ID = "invalid";

    TimeOutSessionFilterStep testClass = new TimeOutSessionFilterStep();

    @Mock
    InternalSessionStore mockNextStep;
    @Mock
    InternalSession timedOutSession;
    @Mock
    InternalSession notTimedOutSession;
    @Mock
    SessionID timedOutSessionID;
    @Mock
    SessionID notTimedOutSessionID;
    @Mock
    SessionID invalidSessionID;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Define timed out and not timed out sessions
        when(timedOutSession.isTimedOut()).thenReturn(true);
        when(timedOutSession.willExpire()).thenReturn(true);
        when(timedOutSession.getTimeLeft()).thenReturn(0L);
        when(timedOutSession.getMaxIdleTime()).thenReturn(1L);
        when(timedOutSession.getIdleTime()).thenReturn(10L);

        when(notTimedOutSession.isTimedOut()).thenReturn(false);
        when(notTimedOutSession.willExpire()).thenReturn(false);
        when(notTimedOutSession.getTimeLeft()).thenReturn(100000L);
        when(notTimedOutSession.getMaxIdleTime()).thenReturn(100000L);
        when(notTimedOutSession.getIdleTime()).thenReturn(1L);

        // set up next step to return default timed out and not timed out session
        when(mockNextStep.getByHandle(Mockito.eq(TIMED_OUT_SESSSION_ID))).thenReturn(timedOutSession);
        when(mockNextStep.getByHandle(Mockito.eq(NOT_TIMED_OUT_SESSSION_ID))).thenReturn(notTimedOutSession);
        when(mockNextStep.getByHandle(null)).thenReturn(null);
        when(mockNextStep.getByHandle(INVALID_SESSSION_ID)).thenReturn(null);
        when(mockNextStep.getBySessionID(timedOutSessionID)).thenReturn(timedOutSession);
        when(mockNextStep.getBySessionID(notTimedOutSessionID)).thenReturn(notTimedOutSession);
        when(mockNextStep.getBySessionID(null)).thenReturn(null);
        when(mockNextStep.getBySessionID(invalidSessionID)).thenReturn(null);
        when(mockNextStep.getByRestrictedID(timedOutSessionID)).thenReturn(timedOutSession);
        when(mockNextStep.getByRestrictedID(notTimedOutSessionID)).thenReturn(notTimedOutSession);
        when(mockNextStep.getByRestrictedID(null)).thenReturn(null);
        when(mockNextStep.getByRestrictedID(invalidSessionID)).thenReturn(null);
    }

    @Test
    public void shouldReturnSessionsThatHaveNotTimedOut() throws SessionPersistenceException {
        // if (see setUp)
        // when
        final InternalSession byHandle = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        final InternalSession bySessionID = testClass.getBySessionID(notTimedOutSessionID, mockNextStep);
        final InternalSession byRestrictedID = testClass.getByRestrictedID(notTimedOutSessionID, mockNextStep);

        // then
        assertNotNull(byHandle, "TimeOutSessionFilterStep.getByHandle failed to get a non timed out session");
        assertNotNull(bySessionID, "TimeOutSessionFilterStep.getBySessionID failed to get a non timed out session");
        assertNotNull(byRestrictedID, "TimeOutSessionFilterStep.getByRestrictedID failed to get a non timed out session");
    }

    @Test
    public void shouldReturnNullForSessionsThatHaveTimedOut() throws SessionPersistenceException {
        // if (see setUp)
        // when
        final InternalSession byHandle = testClass.getByHandle(TIMED_OUT_SESSSION_ID, mockNextStep);
        final InternalSession bySessionID = testClass.getBySessionID(timedOutSessionID, mockNextStep);
        final InternalSession byRestrictedID = testClass.getByRestrictedID(timedOutSessionID, mockNextStep);

        // then
        assertNull(byHandle, "TimeOutSessionFilterStep.getByHandle did not filter out a non timed out session");
        assertNull(bySessionID, "TimeOutSessionFilterStep.getBySessionID did not filter out a non timed out session");
        assertNull(byRestrictedID, "TimeOutSessionFilterStep.getByRestrictedID did not filter out a non timed out session");
    }

    @Test
    public void shouldReturnNullWhenGivenANullSessionID() throws SessionPersistenceException {
        // if (see setUp)
        // when
        final InternalSession byHandle = testClass.getByHandle(null, mockNextStep);
        final InternalSession bySessionID = testClass.getBySessionID(null, mockNextStep);
        final InternalSession byRestrictedID = testClass.getByRestrictedID(null, mockNextStep);

        // then
        assertNull(byHandle, "TimeOutSessionFilterStep.getByHandle did not return null for a null session identifier");
        assertNull(bySessionID, "TimeOutSessionFilterStep.getBySessionID did not return null for a null session identifier");
        assertNull(byRestrictedID, "TimeOutSessionFilterStep.getByRestrictedID did not return null for a null session identifier");
    }

    @Test
    public void shouldReturnNullWhenSessionIdentifyDoesNotMatchASession() throws SessionPersistenceException {
        // if (see setUp)
        // when
        final InternalSession byHandle = testClass.getByHandle(INVALID_SESSSION_ID, mockNextStep);
        final InternalSession bySessionID = testClass.getBySessionID(invalidSessionID, mockNextStep);
        final InternalSession byRestrictedID = testClass.getByRestrictedID(invalidSessionID, mockNextStep);

        // then
        assertNull(byHandle, "TimeOutSessionFilterStep.getByHandle did not return null for an invalid session identifier");
        assertNull(bySessionID, "TimeOutSessionFilterStep.getBySessionID did not return null for an invalid session identifier");
        assertNull(byRestrictedID, "TimeOutSessionFilterStep.getByRestrictedID did not return null for an invalid session identifier");
    }

    @Test
    public void shouldStillReturnSessionsThatHaveNotTimedOutWithWillExpireTurnedOn() throws SessionPersistenceException {

        // if
        when(notTimedOutSession.willExpire()).thenReturn(false);
        // when
        final InternalSession byHandleWithWillExpireOn = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        // then
        assertNotNull(byHandleWithWillExpireOn, "TimeOutSessionFilterStep.getByHandle " +
                "failed to get a non timed out session - will expire is on but session should not be expired");
    }

    @Test
    public void shouldStillReturnSessionsThatHaveNotTimedOutWithTimeleftOf0AndWillExpireTurnedOff() throws SessionPersistenceException {

        // if
        when(notTimedOutSession.getTimeLeft()).thenReturn(0L);
        // when
        final InternalSession byHandleWithWillExpireOn = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        // then
        assertNotNull(byHandleWithWillExpireOn, "TimeOutSessionFilterStep.getByHandle " +
                "failed to get a non timed out session - time left is 0 but will expire is off");

    }

    @Test
    public void shouldNotReturnSessionsIfWillExpireIsOnAndTheTimeleftIs0() throws SessionPersistenceException {

        // if
        when(notTimedOutSession.willExpire()).thenReturn(true);
        when(notTimedOutSession.getTimeLeft()).thenReturn(0L);
        // when
        final InternalSession byHandleWithWillExpireOn = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        // then
        assertNull(byHandleWithWillExpireOn, "TimeOutSessionFilterStep.getByHandle " +
                "failed to filter a session - will expire is on, time left is 0");
    }

    @Test
    public void shouldStillReturnSessionsThatHaveAMaxIdleTimeBlowTheCurrentIdleTimeIfWillExpireIsOff() throws SessionPersistenceException {

        // if
        when(notTimedOutSession.getMaxIdleTime()).thenReturn(10L);
        when(notTimedOutSession.getIdleTime()).thenReturn(100L);
        // when
        final InternalSession byHandleWithWillExpireOffAndMaxIdleLessThanIdle = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        // then
        assertNotNull(byHandleWithWillExpireOffAndMaxIdleLessThanIdle, "TimeOutSessionFilterStep.getByHandle " +
                "failed to get a non timed out session - max idle < current idle but will expire is off");
    }

    @Test
    public void shouldNotReturnSessionsThatHaveAMaxIdleTimeBlowTheCurrentIdleTimeWhenWillExpireIsOff() throws SessionPersistenceException {

        // if
        when(notTimedOutSession.willExpire()).thenReturn(true);
        when(notTimedOutSession.getMaxIdleTime()).thenReturn(1L);
        when(notTimedOutSession.getIdleTime()).thenReturn(1000L);
        // when
        final InternalSession byHandleWithWillExpireOnAndMaxIdleLessThanIdle = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        // then
        assertNull(byHandleWithWillExpireOnAndMaxIdleLessThanIdle, "TimeOutSessionFilterStep.getByHandle " +
                "failed to filter a session - will expire on, max idle < current idle");
    }

    @Test
    public void shouldNotReturnSessionsThatHavebeenSetToTimedOut() throws SessionPersistenceException {

        // if
        when(notTimedOutSession.willExpire()).thenReturn(true);
        when(notTimedOutSession.isTimedOut()).thenReturn(true);
        // when
        final InternalSession byHandleWithWillExpireOffAndMaxIdleLessThanIdle = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        // then
        assertNull(byHandleWithWillExpireOffAndMaxIdleLessThanIdle, "TimeOutSessionFilterStep.getByHandle " +
                "failed to filter out session - timed out set to true");
    }

    @Test
    public void shouldNotReturnSessionsThatHaveBeenSetToTimedOutEvenIfWillExpireIsOff() throws SessionPersistenceException {

        // if
        when(notTimedOutSession.isTimedOut()).thenReturn(true);
        // when
        final InternalSession byHandleWithWillExpireOffAndMaxIdleLessThanIdle = testClass.getByHandle(NOT_TIMED_OUT_SESSSION_ID, mockNextStep);
        // then
        assertNull(byHandleWithWillExpireOffAndMaxIdleLessThanIdle, "TimeOutSessionFilterStep.getByHandle " +
                "failed to filter out session - timed out set to true");
    }
}
