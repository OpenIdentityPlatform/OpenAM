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

package org.forgerock.openam.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Hashtable;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionInfo;

public class SessionCacheTest {


    private SessionCache cache = SessionCache.getInstance();
    private Session session;
    private SessionID sessionID;
    private SessionInfo sessionInfo;

    @BeforeMethod
    public void setup() throws SessionException {
        sessionInfo = mock(SessionInfo.class);
        sessionID = mock(SessionID.class);
        session = spy(new Session(sessionID) {

            @Override
			public long getLatestRefreshTime() {
				return System.currentTimeMillis();
			}

			@Override
			public long getMaxSessionTime() {
				return 2;
			}

			@Override
			public long getMaxIdleTime() {
				return 1;
			}

			@Override
            public void refresh(boolean reset) throws SessionException {
                if (reset) {
                    given(sessionInfo.getTimeIdle()).willReturn(0l);
                    session.update(sessionInfo);
                }
            }
        });

        given(sessionInfo.getTimeIdle()).willReturn(10l);
        // Keep the session fresh when the clock crosses a second boundary during a test.
        given(sessionInfo.getMaxCaching()).willReturn(1L);
        given(sessionInfo.getSessionType()).willReturn("user");
        given(sessionInfo.getState()).willReturn("valid");
        given(sessionInfo.getProperties()).willReturn(new Hashtable<String, String>());

        cache.writeSession(session);
    }


    @DataProvider(name = "secondsSinceRefresh")
    public Object[][] secondsSinceRefresh() {
        return new Object[][] {{0L}, {1L}};
    }

    private void updateSession(long secondsSinceRefresh) throws SessionException, ReflectiveOperationException {
        session.update(sessionInfo);
        // Model crossing a second boundary without sleeping or changing the JVM-wide clock.
        Field latestRefreshTime = Session.class.getDeclaredField("latestRefreshTime");
        latestRefreshTime.setAccessible(true);
        latestRefreshTime.setLong(session, currentTimeMillis() / 1000 - secondsSinceRefresh);
    }

    @Test(dataProvider = "secondsSinceRefresh")
    public void shouldResetIdleTimeResetSetToTrue(long secondsSinceRefresh)
            throws SessionException, ReflectiveOperationException {
        //given
        updateSession(secondsSinceRefresh);

        //when
        session = cache.getSession(sessionID, false, true);


        //then
        verify(session, times(1)).refresh(true);
        verify(session, never()).refresh(false);
        assertThat(session.getIdleTime()).isEqualTo(0);
    }

    @Test(dataProvider = "secondsSinceRefresh")
    public void shouldNotResetIdleTimeWhenResetSetToFalse(long secondsSinceRefresh)
            throws SessionException, ReflectiveOperationException {
        //given
        updateSession(secondsSinceRefresh);

        //when
        session = cache.getSession(sessionID, false, false);


        //then
        verify(session, times(0)).refresh(true);
        verify(session, never()).refresh(false);
        assertThat(session.getIdleTime()).isEqualTo(10);
    }

    @Test(dataProvider = "secondsSinceRefresh")
    public void shouldIgnoreResetIdleTimeWhenInvaliodSessionIsAllowed(long secondsSinceRefresh)
            throws SessionException, ReflectiveOperationException {
        //given
        updateSession(secondsSinceRefresh);

        //when
        session = cache.getSession(sessionID, true, true);


        //then
        verify(session, times(0)).refresh(true);
        verify(session, never()).refresh(false);
        assertThat(session.getIdleTime()).isEqualTo(10);
    }

}
