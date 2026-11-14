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
package com.sun.identity.idm;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.HashMap;
import java.util.Map;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.operations.ServerSessionOperationStrategy;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.session.SessionCuller;
import org.forgerock.openam.session.SessionPollerPool;
import org.forgerock.openam.session.SessionServiceURLService;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.session.service.access.SessionQueryManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@PrepareForTest({SystemProperties.class, InjectorHolder.class, IdServicesFactory.class,
        AdminTokenAction.class, AMSetupServlet.class, SessionCache.class})
@PowerMockIgnore({"jdk.internal.reflect.*", "jakarta.servlet.*"})
public class AMIdentityPasswordChangeTest extends PowerMockTestCase {
    private static final String USER_ID = "id=amadmin,ou=user,dc=config";

    private final Map<SessionID, InternalSession> storedSessions = new HashMap<>();
    private SessionCache cache;
    private SessionAccessManager access;
    private SSOToken caller;
    private SSOToken appToken;
    private IdServices idServices;
    private SessionQueryManager query;
    private AMIdentity identity;

    @BeforeMethod
    public void setup() throws Exception {
        storedSessions.clear();
        PowerMockito.mockStatic(SystemProperties.class, InjectorHolder.class, IdServicesFactory.class,
                AdminTokenAction.class, AMSetupServlet.class);
        when(SystemProperties.isServerMode()).thenReturn(true);
        when(SystemProperties.getAsBoolean(Session.CACHED_BASE_POLLING_PROPERTY, false)).thenReturn(true);

        SessionPollerPool poller = mock(SessionPollerPool.class);
        when(poller.isPollingEnabled()).thenReturn(true);
        when(poller.getCacheBasedPolling()).thenReturn(true);
        cache = Whitebox.invokeConstructor(SessionCache.class, poller, mock(Debug.class));
        PowerMockito.mockStatic(SessionCache.class);
        when(SessionCache.getInstance()).thenReturn(cache);
        // Culling is independent of revocation; avoid starting timer jobs in this test.
        PowerMockito.whenNew(SessionCuller.class).withAnyArguments().thenReturn(mock(SessionCuller.class));

        access = mock(SessionAccessManager.class);
        when(access.getInternalSession(any(SessionID.class)))
                .thenAnswer(invocation -> storedSessions.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            InternalSession session = invocation.getArgument(0);
            storedSessions.remove(session.getID());
            return null;
        }).when(access).removeInternalSession(any(InternalSession.class));

        SessionChangeAuthorizer authorizer = spy(Whitebox.invokeConstructor(SessionChangeAuthorizer.class,
                mock(Debug.class), mock(SSOTokenManager.class)));
        // Keep the real VALID-state check; delegation itself is outside this fixture.
        doReturn(true).when(authorizer).hasTopLevelAdminRole(any(SessionID.class));
        query = mock(SessionQueryManager.class);
        LocalOperations local = Whitebox.invokeConstructor(LocalOperations.class, mock(Debug.class), access,
                query, new SessionInfoFactory(), mock(SessionServerConfig.class),
                mock(InternalSessionEventBroker.class), authorizer);
        ServerSessionOperationStrategy strategy = mock(ServerSessionOperationStrategy.class);
        when(strategy.getOperation(any(SessionID.class))).thenReturn(local);
        when(InjectorHolder.getInstance(ServerSessionOperationStrategy.class)).thenReturn(strategy);
        when(InjectorHolder.getInstance(SessionCache.class)).thenReturn(cache);
        when(InjectorHolder.getInstance(SessionCookies.class)).thenReturn(mock(SessionCookies.class));
        when(InjectorHolder.getInstance(SessionServiceURLService.class))
                .thenReturn(mock(SessionServiceURLService.class));
        when(InjectorHolder.getInstance(SessionQueryManager.class)).thenReturn(query);

        appToken = mock(SSOToken.class);
        AdminTokenAction adminAction = mock(AdminTokenAction.class);
        when(AdminTokenAction.getInstance()).thenReturn(adminAction);
        when(adminAction.run()).thenReturn(appToken);

        idServices = mock(IdServices.class);
        when(IdServicesFactory.getDataStoreServices()).thenReturn(idServices);
        caller = mock(SSOToken.class);
        when(caller.getProperty(Constants.UNIVERSAL_IDENTIFIER)).thenReturn(USER_ID);
        SSOTokenID callerId = mock(SSOTokenID.class);
        when(callerId.toString()).thenReturn("current");
        when(caller.getTokenID()).thenReturn(callerId);
        identity = new AMIdentity(caller);

        addStoredSession("current");
        addStoredSession("other");
        Map<String, Long> sessions = new HashMap<>();
        sessions.put("current", 1L);
        sessions.put("other", 1L);
        when(query.getAllSessionsByUUID(USER_ID)).thenReturn(sessions);
    }

    @Test
    public void revokesUncachedSessionWithoutResettingIdleTime() throws Exception {
        assertFalse(cache.hasSession(new SessionID("other")));
        InternalSession other = storedSessions.get(new SessionID("other"));

        identity.changePassword("old", "new");

        assertFalse(storedSessions.containsKey(new SessionID("other")));
        assertTrue(storedSessions.containsKey(new SessionID("current")));
        verify(idServices).changePassword(caller, IdType.USER, "amadmin", "old", "new", "dc=config", null);
        verify(other, never()).setLatestAccessTime();
    }

    @Test
    public void keepsMasterSessionWhenCallerUsesRestrictedToken() throws Exception {
        when(caller.getTokenID().toString()).thenReturn("restricted");
        when(caller.isTokenRestricted()).thenReturn(true);
        when(caller.dereferenceRestrictedTokenID(appToken, "restricted")).thenReturn("current");
        // Isolate the ID mismatch from the separate cache-miss bug.
        cache.getSession(new SessionID("current"), false, false);
        cache.getSession(new SessionID("other"), false, false);

        identity.changePassword("old", "new");

        assertTrue(storedSessions.containsKey(new SessionID("current")));
        assertFalse(storedSessions.containsKey(new SessionID("other")));
    }

    @Test
    public void doesNotRevokeSessionsWhenRestrictedTokenCannotBeResolved() throws Exception {
        when(caller.isTokenRestricted()).thenReturn(true);
        when(caller.dereferenceRestrictedTokenID(appToken, "current"))
                .thenThrow(new SSOException("unavailable"));
        cache.getSession(new SessionID("current"), false, false);
        cache.getSession(new SessionID("other"), false, false);

        identity.changePassword("old", "new");

        assertEquals(storedSessions.size(), 2);
        verify(idServices).changePassword(caller, IdType.USER, "amadmin", "old", "new", "dc=config", null);
        verifyZeroInteractions(query);
    }

    @Test
    public void doesNotRevokeSessionsWhenChangingAnotherUsersPassword() throws Exception {
        AMIdentity otherIdentity = new AMIdentity(caller, "id=another,ou=user,dc=config");

        otherIdentity.changePassword("old", "new");

        assertEquals(storedSessions.size(), 2);
        verify(idServices).changePassword(caller, IdType.USER, "another", "old", "new", "dc=config", null);
        verifyZeroInteractions(query);
    }

    @Test(expectedExceptions = SSOException.class)
    public void doesNotRevokeSessionsWhenPasswordChangeFails() throws Exception {
        doThrow(new SSOException("password change failed")).when(idServices)
                .changePassword(caller, IdType.USER, "amadmin", "old", "new", "dc=config", null);

        try {
            identity.changePassword("old", "new");
        } finally {
            assertEquals(storedSessions.size(), 2);
            verifyZeroInteractions(query);
        }
    }

    private void addStoredSession(String id) throws Exception {
        SessionID sessionId = new SessionID(id);
        InternalSession internal = mock(InternalSession.class);
        when(internal.getID()).thenReturn(sessionId);
        when(internal.getSessionID()).thenReturn(sessionId);
        when(internal.getState()).thenReturn(SessionState.VALID);
        SessionInfo info = new SessionInfo();
        info.setSessionType("user");
        info.setState("valid");
        info.setClientID(USER_ID);
        info.setClientDomain("dc=config");
        info.setMaxTime(60);
        info.setMaxIdle(30);
        info.setMaxCaching(3);
        when(internal.toSessionInfo()).thenReturn(info);
        storedSessions.put(sessionId, internal);
    }
}
