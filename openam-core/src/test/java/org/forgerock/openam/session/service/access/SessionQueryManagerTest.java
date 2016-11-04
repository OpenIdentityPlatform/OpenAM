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

import static com.iplanet.dpro.session.service.SessionState.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.SearchResults;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceStore;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionQueryManagerTest {

    @Mock
    private Debug debug;
    @Mock
    private SessionPersistenceStore sessionPersistenceStore;
    @Mock
    private SessionChangeAuthorizer sessionChangeAuthorizer;
    @Mock
    private SessionServiceConfig serviceConfig;
    @Mock
    private Session actingSession;
    @Mock
    private InternalSession internalSession;
    @Mock
    private SessionID sessionID;


    private SessionQueryManager sessionQueryManager;

    @BeforeMethod
    public void setup() throws SessionException {
        MockitoAnnotations.initMocks(this);
        given(actingSession.getState(false)).willReturn(VALID);
        sessionQueryManager = new SessionQueryManager(debug, sessionPersistenceStore,
                sessionChangeAuthorizer, serviceConfig);
    }

    @Test
    public void shouldReturnAllSessionsWhenNullPattern() throws SessionException, CoreTokenException, SSOException {
        //given
        String pattern = null;
        given(actingSession.getSessionID()).willReturn(sessionID);
        given(sessionChangeAuthorizer.hasTopLevelAdminRole(sessionID)).willReturn(true);
        given(sessionPersistenceStore.getValidSessions()).willReturn(Arrays.asList(internalSession));
        given(internalSession.isUserSession()).willReturn(true);
        given(internalSession.toSessionInfo()).willReturn(new SessionInfo());

        //when
        SearchResults<SessionInfo> results = sessionQueryManager.getValidSessions(actingSession, pattern);

        //then
        assertThat(results.getTotalResultCount()).isEqualTo(1);
    }
}
