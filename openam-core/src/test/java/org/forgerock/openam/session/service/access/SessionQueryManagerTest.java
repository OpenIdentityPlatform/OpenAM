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
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.session.service.access;

import static com.iplanet.dpro.session.service.SessionState.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.SearchResults;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonPointer;
import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceStore;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.util.query.QueryFilter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionQueryManagerTest {

    private static final JsonPointer REALM = new JsonPointer("realm");
    private static final JsonPointer USERNAME = new JsonPointer("username");

    private static final String REALM_A_PATH = "/realmA";
    private static final String REALM_A_DN = "o=realma,ou=services,dc=openam,dc=example,dc=com";
    private static final String REALM_B_PATH = "/realmB";
    private static final String REALM_B_DN = "o=realmb,ou=services,dc=openam,dc=example,dc=com";
    private static final String ROOT_REALM_DN = "dc=openam,dc=example,dc=com";

    @Mock
    private Debug debug;
    @Mock
    private SessionPersistenceStore sessionPersistenceStore;
    @Mock
    private SessionChangeAuthorizer sessionChangeAuthorizer;
    @Mock
    private SessionServiceConfig serviceConfig;
    @Mock
    private DNWrapper dnWrapper;
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
        given(actingSession.getSessionID()).willReturn(sessionID);
        given(dnWrapper.orgNameToRealmName(REALM_A_DN)).willReturn(REALM_A_PATH);
        given(dnWrapper.orgNameToRealmName(REALM_B_DN)).willReturn(REALM_B_PATH);
        given(dnWrapper.orgNameToRealmName(ROOT_REALM_DN)).willReturn("/");
        sessionQueryManager = new SessionQueryManager(debug, sessionPersistenceStore,
                sessionChangeAuthorizer, serviceConfig, dnWrapper);
    }

    @Test
    public void shouldReturnAllSessionsWhenNullPattern() throws SessionException, CoreTokenException, SSOException {
        //given
        String pattern = null;
        given(sessionChangeAuthorizer.hasTopLevelAdminRole(sessionID)).willReturn(true);
        given(sessionPersistenceStore.getValidSessions()).willReturn(Arrays.asList(internalSession));
        given(internalSession.isUserSession()).willReturn(true);
        given(internalSession.toSessionInfo()).willReturn(new SessionInfo());

        //when
        SearchResults<SessionInfo> results = sessionQueryManager.getValidSessions(actingSession, pattern);

        //then
        assertThat(results.getTotalResultCount()).isEqualTo(1);
    }

    @Test
    public void shouldLetTheTopLevelAdminQueryAnyRealm() throws Exception {
        //given
        given(sessionChangeAuthorizer.hasTopLevelAdminRole(sessionID)).willReturn(true);
        given(actingSession.getClientDomain()).willReturn(REALM_A_DN);
        givenTheStoreReturnsASession();

        //when
        Collection<PartialSession> sessions =
                sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_B_PATH));

        //then
        assertThat(sessions).hasSize(1);
    }

    @Test
    public void shouldLetARealmAdminQueryItsOwnRealm() throws Exception {
        //given
        givenARealmAdminOf(REALM_A_DN);
        givenTheStoreReturnsASession();

        //when
        Collection<PartialSession> sessions =
                sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_A_PATH));

        //then
        assertThat(sessions).hasSize(1);
    }

    @Test
    public void shouldLetARealmAdminQueryASubRealmOfItsOwnRealm() throws Exception {
        //given
        givenARealmAdminOf(REALM_A_DN);
        givenTheStoreReturnsASession();

        //when
        Collection<PartialSession> sessions =
                sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_A_PATH + "/sub"));

        //then
        assertThat(sessions).hasSize(1);
    }

    @Test
    public void shouldLetARealmAdminQueryARealmListedInItsProfile() throws Exception {
        //given
        givenARealmAdminOf(REALM_A_DN);
        given(sessionChangeAuthorizer.getSessionSubjectOrganisations(sessionID))
                .willReturn(Collections.singleton(REALM_B_DN));
        givenTheStoreReturnsASession();

        //when
        Collection<PartialSession> sessions =
                sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_B_PATH));

        //then
        assertThat(sessions).hasSize(1);
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldRefuseARealmAdminQueryingAnotherRealm() throws Exception {
        //given
        givenARealmAdminOf(REALM_A_DN);

        //when
        sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_B_PATH));
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldRefuseARealmAdminQueryingTheTopLevelRealm() throws Exception {
        //given
        givenARealmAdminOf(REALM_A_DN);

        //when
        sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery("/"));
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldRefuseARealmAdminQueryingARealmWithASimilarName() throws Exception {
        //given
        givenARealmAdminOf(REALM_A_DN);

        //when
        sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_A_PATH + "-other"));
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldRefuseARealmAdminQueryingASubRealmOfARealmListedInItsProfile() throws Exception {
        //given the profile attribute grants the realm itself, as it does for the older listing path
        givenARealmAdminOf(REALM_A_DN);
        given(sessionChangeAuthorizer.getSessionSubjectOrganisations(sessionID))
                .willReturn(Collections.singleton(REALM_B_DN));

        //when
        sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_B_PATH + "/sub"));
    }

    @Test
    public void shouldNotQueryTheStoreWhenTheRealmIsRefused() throws Exception {
        //given
        givenARealmAdminOf(REALM_A_DN);

        //when
        try {
            sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_B_PATH));
        } catch (SessionException expected) {
            // The realm may not be queried by this caller.
        }

        //then
        verify(sessionPersistenceStore, never()).searchPartialSessions(any(CrestQuery.class));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRefuseARealmAdminQueryingAnotherRealmThroughAParentPathSegment() throws Exception {
        //when the realm would name another realm once the '..' segment is resolved
        sessionQueryManager.getMatchingValidSessions(actingSession, realmQuery(REALM_A_PATH + "/.." + REALM_B_PATH));
    }

    @Test
    public void shouldNotQueryTheStoreWhenTheRealmHasAParentPathSegment() throws Exception {
        //when
        try {
            sessionQueryManager.getMatchingValidSessions(actingSession,
                    realmQuery(REALM_A_PATH + "/.." + REALM_B_PATH));
        } catch (IllegalArgumentException expected) {
            // The realm of the filter has to name a realm without being resolved first.
        }

        //then
        verify(sessionPersistenceStore, never()).searchPartialSessions(any(CrestQuery.class));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRefuseAQueryWithoutARealm() throws Exception {
        //when the realm is missing, before any privilege of the caller is looked at
        sessionQueryManager.getMatchingValidSessions(actingSession,
                new CrestQuery(QueryFilter.equalTo(USERNAME, "demo")));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRefuseAQueryWithoutAFilter() throws Exception {
        //when the filter is missing, before any privilege of the caller is looked at
        sessionQueryManager.getMatchingValidSessions(actingSession, new CrestQuery("all"));
    }

    private void givenARealmAdminOf(String realmDn) throws Exception {
        given(sessionChangeAuthorizer.hasTopLevelAdminRole(sessionID)).willReturn(false);
        given(actingSession.getClientDomain()).willReturn(realmDn);
    }

    private void givenTheStoreReturnsASession() throws CoreTokenException {
        given(sessionPersistenceStore.searchPartialSessions(any(CrestQuery.class)))
                .willReturn(Arrays.asList(new PartialSession.Builder().username("demo").build()));
    }

    private CrestQuery realmQuery(String realm) {
        return new CrestQuery(QueryFilter.equalTo(REALM, realm));
    }
}
