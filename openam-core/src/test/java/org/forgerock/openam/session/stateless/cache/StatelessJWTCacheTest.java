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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.session.stateless.cache;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.session.stateless.StatelessConfig;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.ServiceListeners;
import com.iplanet.services.naming.ServiceListeners.Action;
import com.iplanet.services.naming.ServiceListeners.ListenerBuilder;

public class StatelessJWTCacheTest {

    private StatelessJWTCache cache;
    private StatelessConfig mockConfig;

    private ServiceListeners mockListeners;
    // Required mocking for ServiceListener
    private ListenerBuilder mockListenerBuilder;
    private List<ServiceListeners.Action> actions = new ArrayList<>();

    @BeforeMethod
    public void setUp() {
        mockConfig = mock(StatelessConfig.class);

        mockListeners = mock(ServiceListeners.class);

        // Setup ServiceListener to capture actions provided.
        mockListenerBuilder = mock(ListenerBuilder.class);
        Answer<Object> capturingAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                actions.add((Action) invocationOnMock.getArguments()[0]);
                return mockListenerBuilder;
            }
        };
        given(mockListenerBuilder.global(any(Action.class))).willAnswer(capturingAnswer);
        given(mockListenerBuilder.organisation(any(Action.class))).willAnswer(capturingAnswer);
        given(mockListenerBuilder.schema(any(Action.class))).willAnswer(capturingAnswer);

        // ServiceListener will return mock Service Listener Builder.
        given(mockListeners.config(anyString())).willReturn(mockListenerBuilder);
    }

    @Test
    public void shouldNotContainNullJWT() {
        cache = new StatelessJWTCache(mockConfig, mockListeners);
        assertThat(cache.contains((String)null)).isFalse();
    }

    @Test
    public void shouldNotContainNullSessionInfo() {
        cache = new StatelessJWTCache(mockConfig, mockListeners);
        assertThat(cache.contains((SessionInfo)null)).isFalse();
    }

    @Test
    public void shouldCacheSessionInfoWithJWT() {
        // Given
        given(mockConfig.getJWTCacheSize()).willReturn(1);
        cache = new StatelessJWTCache(mockConfig, mockListeners);
        String jwt = "badger";
        SessionInfo mockInfo = mock(SessionInfo.class);
        // When
        cache.cache(mockInfo, jwt);
        // Then
        assertThat(cache.getSessionInfo(jwt)).isEqualTo(mockInfo);
    }

    @Test
    public void shouldUseConfigForSizing() {
        // Given
        given(mockConfig.getJWTCacheSize()).willReturn(0);
        cache = new StatelessJWTCache(mockConfig, mockListeners);
        String key = "badger";
        // When
        cache.cache(mock(SessionInfo.class), key);
        // Then
        assertThat(cache.contains(key)).isFalse();
    }

    @Test
    public void shouldClearCache() {
        // Given
        given(mockConfig.getJWTCacheSize()).willReturn(1);
        cache = new StatelessJWTCache(mockConfig, mockListeners);

        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        cache.cache(mockSessionInfo, "badger");

        // When
        cache.clear();

        // Then
        assertThat(cache.contains(mockSessionInfo)).isFalse();
    }

    @Test
    public void shouldRegisterListenersForNotification() {
        given(mockConfig.getJWTCacheSize()).willReturn(1);
        cache = new StatelessJWTCache(mockConfig, mockListeners);
        assertThat(actions).isNotEmpty();
    }

    @Test
    public void shouldRespondToServiceListenersNotification() {
        given(mockConfig.getJWTCacheSize()).willReturn(1);
        cache = new StatelessJWTCache(mockConfig, mockListeners);

        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        cache.cache(mockSessionInfo, "badger");

        // When
        for (Action action : actions) {
            action.performUpdate();
        }

        // Then
        assertThat(cache.contains(mockSessionInfo)).isFalse();
    }
}