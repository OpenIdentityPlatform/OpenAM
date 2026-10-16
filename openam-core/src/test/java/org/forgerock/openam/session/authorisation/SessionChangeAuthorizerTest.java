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
 * Copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.session.authorisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Collections;

import org.forgerock.openam.utils.CollectionUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

/**
 * Tests that the realm restriction on session destruction is evaluated against the session being
 * destroyed rather than against the realm the requester happens to be in.
 */
public class SessionChangeAuthorizerTest {

    private static final String DESTROY_SESSIONS_ATTRIBUTE = "iplanet-am-session-destroy-sessions";
    private static final String REALM_A = "o=realma,ou=services,dc=openam,dc=example,dc=org";
    private static final String REALM_B = "o=realmb,ou=services,dc=openam,dc=example,dc=org";

    @Mock
    private Debug debug;
    @Mock
    private SSOTokenManager ssoTokenManager;
    @Mock
    private Session requester;
    @Mock
    private AMIdentity requesterIdentity;

    private SessionID requesterSessionId;
    private SessionID targetSessionId;

    private SessionChangeAuthorizer authorizer;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        requesterSessionId = new SessionID("requester-session");
        targetSessionId = new SessionID("target-session");

        given(requester.getState(false)).willReturn(SessionState.VALID);
        given(requester.getSessionID()).willReturn(requesterSessionId);
        given(requester.getID()).willReturn(requesterSessionId);
        // The requester is authenticated to realm A.
        given(requester.getClientDomain()).willReturn(REALM_A);

        authorizer = spy(new SessionChangeAuthorizer(debug, ssoTokenManager));
        willReturn(false).given(authorizer).hasTopLevelAdminRole(any(SessionID.class));
        willReturn(requesterIdentity).given(authorizer).getUser(any(SessionID.class));
    }

    @Test
    public void shouldDenyDestroyingSessionOfRealmTheRequesterIsNotDelegatedAdminOf() throws Exception {
        // Given a delegated administrator of realm A only...
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE))
                .willReturn(CollectionUtils.asSet(REALM_A));

        // When destroying a session that belongs to realm B...
        boolean allowed = authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_B);

        // Then the realm restriction applies to the target session, not to the requester's own realm.
        assertThat(allowed).isFalse();
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldThrowWhenDestroyingSessionOfAnotherRealm() throws Exception {
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE))
                .willReturn(CollectionUtils.asSet(REALM_A));

        authorizer.checkPermissionToDestroySession(requester, targetSessionId, REALM_B);
    }

    @Test
    public void shouldAllowDestroyingSessionOfDelegatedRealm() throws Exception {
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE))
                .willReturn(CollectionUtils.asSet(REALM_A));

        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_A)).isTrue();
    }

    @Test
    public void shouldEvaluateTargetRealmRatherThanRequesterRealm() throws Exception {
        // Given a requester whose account lives in realm A, but who is delegated realm B.
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE))
                .willReturn(CollectionUtils.asSet(REALM_B));

        // Then the realm the requester was delegated is the one that counts...
        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_B)).isTrue();
        // ...and the requester's own realm grants nothing on its own.
        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_A)).isFalse();
    }

    @Test
    public void shouldAllowSessionToDestroyItselfRegardlessOfRealm() throws Exception {
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE)).willReturn(Collections.emptySet());

        assertThat(authorizer.hasPermissionToDestroySession(requester, requesterSessionId, REALM_B)).isTrue();
    }

    @Test
    public void shouldAllowTopLevelAdminToDestroyAnySession() throws Exception {
        willReturn(true).given(authorizer).hasTopLevelAdminRole(any(SessionID.class));
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE)).willReturn(Collections.emptySet());

        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_B)).isTrue();
    }

    @Test
    public void shouldDenyWhenTargetRealmCannotBeDetermined() throws Exception {
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE))
                .willReturn(CollectionUtils.asSet(REALM_A, REALM_B));

        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, null)).isFalse();
        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, "")).isFalse();
    }

    @Test
    public void shouldDenyWhenRequesterHasNoDestroySessionsAttribute() throws Exception {
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE)).willReturn(null);

        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_B)).isFalse();
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldRejectRequesterWhoseSessionIsNotValid() throws Exception {
        given(requester.getState(false)).willReturn(SessionState.INVALID);

        authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_A);
    }

    @Test
    public void shouldDenyWhenTheRequesterIdentityCannotBeResolved() throws Exception {
        // getUser swallows an IdRepoException and returns null.
        willReturn(null).given(authorizer).getUser(any(SessionID.class));

        assertThat(authorizer.hasPermissionToDestroySession(requester, targetSessionId, REALM_A)).isFalse();
    }

    @Test
    public void shouldReturnNoOrganisationsWhenTheSubjectIdentityCannotBeResolved() throws Exception {
        willReturn(null).given(authorizer).getUser(any(SessionID.class));

        assertThat(authorizer.getSessionSubjectOrganisations(targetSessionId)).isEmpty();
    }

    @Test
    public void shouldReturnNoOrganisationsWhenTheAttributeIsNotSet() throws Exception {
        given(requesterIdentity.getAttribute("iplanet-am-session-get-valid-sessions")).willReturn(null);

        assertThat(authorizer.getSessionSubjectOrganisations(targetSessionId)).isEmpty();
    }

    @Test
    public void shouldTakeTheTargetRealmFromTheSessionBeingDestroyed() throws Exception {
        // Given a delegated administrator of realm A only...
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE))
                .willReturn(CollectionUtils.asSet(REALM_A));
        Session sessionToDestroy = mock(Session.class);
        given(sessionToDestroy.getID()).willReturn(targetSessionId);
        given(sessionToDestroy.getClientDomain()).willReturn(REALM_B);

        // Then the realm is read off the session being destroyed, not off the requester.
        assertThat(authorizer.hasPermissionToDestroySession(requester, sessionToDestroy)).isFalse();

        given(sessionToDestroy.getClientDomain()).willReturn(REALM_A);
        assertThat(authorizer.hasPermissionToDestroySession(requester, sessionToDestroy)).isTrue();
    }

    @Test
    public void shouldTakeTheTargetRealmFromTheInternalSessionBeingDestroyed() throws Exception {
        given(requesterIdentity.getAttribute(DESTROY_SESSIONS_ATTRIBUTE))
                .willReturn(CollectionUtils.asSet(REALM_A));
        InternalSession sessionToDestroy = mock(InternalSession.class);
        given(sessionToDestroy.getID()).willReturn(targetSessionId);
        given(sessionToDestroy.getClientDomain()).willReturn(REALM_B);

        assertThat(authorizer.hasPermissionToDestroySession(requester, sessionToDestroy)).isFalse();

        given(sessionToDestroy.getClientDomain()).willReturn(REALM_A);
        assertThat(authorizer.hasPermissionToDestroySession(requester, sessionToDestroy)).isTrue();
    }
}
