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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import org.forgerock.openam.session.stateless.cache.StatelessJWTCache;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * Limited amount of testing present here due to static code usage.
 */
public class StatelessSessionFactoryTest {

    private StatelessSessionFactory factory;
    private StatelessJWTCache mockCache;

    @BeforeMethod
    public void setup() {
        mockCache = mock(StatelessJWTCache.class);
        SessionServerConfig mockServerConfig = mock(SessionServerConfig.class);
        SessionServiceConfig mockServiceConfig = mock(SessionServiceConfig.class);
        factory = new StatelessSessionFactory(mockCache, mockServerConfig, mockServiceConfig);
    }

    @Test
    public void shouldContainJwtInSessionID() {
        SessionID mockSession = mock(SessionID.class);
        given(mockSession.getTail()).willReturn("badger");
        assertThat(factory.containsJwt(mockSession)).isTrue();
    }

    @Test
    public void shouldExtractJWTFromSessionID() {
        SessionID id = mock(SessionID.class);
        given(id.getTail()).willReturn("badger=");
        assertThat(StatelessSessionFactory.getJWTFromSessionID(id)).isEqualTo("badger.");
    }

    @Test
    public void shouldReturnNullIfNoJWTInSessionID() {
        SessionID id = mock(SessionID.class);
        given(id.getTail()).willReturn(null);
        assertThat(StatelessSessionFactory.getJWTFromSessionID(id)).isNull();
    }

    @Test
    public void shouldReturnNullIfNoSessionIDProvided() {
        assertThat(StatelessSessionFactory.getJWTFromSessionID(null)).isNull();
    }
}