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
package org.forgerock.openam.session.stateless.cache;

import com.iplanet.dpro.session.share.SessionInfo;
import org.forgerock.openam.session.stateless.StatelessConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

public class StatelessJWTCacheTest {

    private StatelessJWTCache cache;
    private StatelessConfig mockConfig;

    @BeforeMethod
    public void setUp() {
        mockConfig = mock(StatelessConfig.class);
    }

    @Test
    public void shouldNotContainNullJWT() {
        cache = new StatelessJWTCache(mockConfig);
        assertThat(cache.contains((String)null)).isFalse();
    }

    @Test
    public void shouldNotContainNullSessionInfo() {
        cache = new StatelessJWTCache(mockConfig);
        assertThat(cache.contains((SessionInfo)null)).isFalse();
    }

    @Test
    public void shouldCacheSessionInfoWithJWT() {
        // Given
        given(mockConfig.getJWTCacheSize()).willReturn(1);
        cache = new StatelessJWTCache(mockConfig);
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
        cache = new StatelessJWTCache(mockConfig);
        String key = "badger";
        // When
        cache.cache(mock(SessionInfo.class), key);
        // Then
        assertThat(cache.contains(key)).isFalse();
    }
}