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

package org.forgerock.openam.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import org.fest.util.Collections;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

@GuiceModules({SessionPersistentStoreTest.TestSessionGuiceModule.class})
public class SessionPersistentStoreTest extends GuiceTestCase {
    @Mock private Debug mockDebug;
    @Mock private SessionID mockSessionID;
    @Mock private InternalSession mockSession;

    @Mock private CTSPersistentStore mockCoreTokenService;
    @Mock private SessionAdapter mockTokenAdapter;
    @Mock private TokenIdFactory mockTokenIdFactory;
    @Mock private Token mockToken;
    @Mock private SessionServiceConfig mockSessionServiceConfig;

    private final String TOKEN = "TOKEN";
    private final String HANDLE = "HANDLE";

    private SessionPersistentStore sessionPersistentStore;

    @BeforeClass
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        sessionPersistentStore = new SessionPersistentStore(mockDebug, mockCoreTokenService, mockTokenAdapter,
                mockTokenIdFactory, mockSessionServiceConfig);

        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockTokenIdFactory.toSessionTokenId(mockSessionID)).willReturn(TOKEN);
        given(mockTokenAdapter.toToken(mockSession)).willReturn(mockToken);
        given(mockTokenAdapter.fromToken(mockToken)).willReturn(mockSession);
        given(mockCoreTokenService.read(TOKEN)).willReturn(mockToken);
    }

    @Test
    public void savesToken() throws Exception {
        sessionPersistentStore.save(mockSession);

        verify(mockCoreTokenService).update(mockToken);
    }

    @Test
    public void deletesToken() throws Exception {
        sessionPersistentStore.delete(mockSession);

        verify(mockCoreTokenService).delete(TOKEN);
    }

    @Test
    public void recoversSession() throws Exception {
        assertThat(sessionPersistentStore.recoverSession(mockSessionID)).isEqualTo(mockSession);
    }


    @Test
    public void recoversSessionByHandle() throws Exception {
        given(mockCoreTokenService.query(any(TokenFilter.class))).willReturn(Collections.list(mockToken));

        assertThat(sessionPersistentStore.recoverSessionByHandle(HANDLE)).isEqualTo(mockSession);
    }

    @Test
    public void failsRecoverSessionByHandleWithNonUniqueMatch() throws Exception {
        given(mockCoreTokenService.query(any(TokenFilter.class))).willReturn(Collections.list(mock(Token.class), mockToken));

        assertThat(sessionPersistentStore.recoverSessionByHandle(HANDLE)).isNull();
    }

    public static class TestSessionGuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(SessionService.class).toInstance(mock(SessionService.class));
            bind(SessionServiceConfig.class).toInstance(mock(SessionServiceConfig.class));
            bind(SessionLogging.class).toInstance(mock(SessionLogging.class));
            bind(SessionAuditor.class).toInstance(mock(SessionAuditor.class));
        }
    }
}
