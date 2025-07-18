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

package org.forgerock.openam.session.service.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceStore;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.dpro.session.PartialSessionFactory;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

@GuiceModules({SessionPersistenceStoreTest.TestSessionGuiceModule.class})
public class SessionPersistenceStoreTest extends GuiceTestCase {
    @Mock private Debug mockDebug;
    @Mock private SessionID mockSessionID;
    @Mock private InternalSession mockSession;

    @Mock private CTSPersistentStore mockCoreTokenService;
    @Mock private SessionAdapter mockTokenAdapter;
    @Mock private TokenIdFactory mockTokenIdFactory;
    @Mock private Token mockToken;
    @Mock private SessionServiceConfig mockSessionServiceConfig;
    @Mock private PartialSessionFactory mockPartialSessionFactory;
    @Mock private IdentityUtils mockIdentityUtils;

    private final String TOKEN = "TOKEN";
    private final String HANDLE = "HANDLE";

    private SessionPersistenceStore sessionPersistenceStore;

    @BeforeClass
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        sessionPersistenceStore = new SessionPersistenceStore(mockDebug, mockCoreTokenService, mockTokenAdapter,
                mockTokenIdFactory, mockSessionServiceConfig, mockPartialSessionFactory, mockIdentityUtils);

        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockTokenIdFactory.toSessionTokenId(mockSessionID)).willReturn(TOKEN);
        given(mockTokenAdapter.toToken(mockSession)).willReturn(mockToken);
        given(mockTokenAdapter.fromToken(mockToken)).willReturn(mockSession);
        given(mockCoreTokenService.read(TOKEN)).willReturn(mockToken);
    }

    @Test
    public void savesToken() throws Exception {
        sessionPersistenceStore.create(mockSession);

        verify(mockCoreTokenService).create(mockToken);
    }

    @Test
    public void deletesToken() throws Exception {
        sessionPersistenceStore.delete(mockSessionID);

        verify(mockCoreTokenService).delete(TOKEN);
    }

    @Test
    public void recoversSession() throws Exception {
        assertThat(sessionPersistenceStore.recoverSession(mockSessionID)).isEqualTo(mockSession);
    }


    @Test
    public void recoversSessionByHandle() throws Exception {
        given(mockCoreTokenService.query(any(TokenFilter.class))).willReturn(CollectionUtils.asList(mockToken));

        assertThat(sessionPersistenceStore.recoverSessionByHandle(HANDLE)).isEqualTo(mockSession);
    }

    @Test
    public void failsRecoverSessionByHandleWithNonUniqueMatch() throws Exception {
        given(mockCoreTokenService.query(any(TokenFilter.class))).willReturn(CollectionUtils.asList(mock(Token.class), mockToken));

        assertThat(sessionPersistenceStore.recoverSessionByHandle(HANDLE)).isNull();
    }

    public static class TestSessionGuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(SessionService.class).toInstance(mock(SessionService.class));
            bind(SessionServiceConfig.class).toInstance(mock(SessionServiceConfig.class));
            bind(SessionLogging.class).toInstance(mock(SessionLogging.class));
            bind(SessionAuditor.class).toInstance(mock(SessionAuditor.class));
            bind(InternalSessionEventBroker.class).toInstance(mock(InternalSessionEventBroker.class));
        }
    }
}
