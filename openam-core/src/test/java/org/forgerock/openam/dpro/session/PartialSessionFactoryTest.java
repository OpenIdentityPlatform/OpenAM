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

package org.forgerock.openam.dpro.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.TimeUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class PartialSessionFactoryTest {

    private static final String UNIVERSAL_ID = "id=demo,ou=user,dc=openam,dc=openidentityplatform,dc=org";
    private static final String USERNAME = "demo";
    private static final String REALM = "/";
    private static final String SESSION_HANDLE = "shandle:foobar";
    private static final long UNIX_TIME = 1477471528l;
    private static final String UNIX_TIME_FORMATTED = "2016-10-26T08:45:28Z";
    @Mock
    private Debug debug;
    @Mock
    private IdentityUtils identityUtils;
    @Mock
    private DNWrapper dnWrapper;
    private PartialSessionFactory factory;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        factory = new PartialSessionFactory(debug, identityUtils, dnWrapper);
    }

    @Test
    public void shouldConstructPartialSessionWithUserIdOnly() {
        // Given
        PartialToken partialToken = new PartialToken(
                ImmutableMap.<CoreTokenField, Object>of(CoreTokenField.USER_ID, UNIVERSAL_ID));
        given(identityUtils.getIdentityName(eq(UNIVERSAL_ID))).willReturn(USERNAME);

        // When
        final PartialSession partialSession = factory.fromPartialToken(partialToken);

        // Then
        assertThat(partialSession.getUsername()).isEqualTo(USERNAME);
        assertThat(partialSession.getUniversalId()).isEqualTo(UNIVERSAL_ID);
    }

    @Test
    public void shouldConstructPartialSessionWithRealmOnly() {
        // Given
        PartialToken partialToken = new PartialToken(
                ImmutableMap.<CoreTokenField, Object>of(SessionTokenField.REALM.getField(), REALM));

        // When
        final PartialSession partialSession = factory.fromPartialToken(partialToken);

        // Then
        assertThat(partialSession.getRealm()).isEqualTo(REALM);
    }

    @Test
    public void shouldConstructPartialSessionWithSessionHandleOnly() {
        // Given
        PartialToken partialToken = new PartialToken(
                ImmutableMap.<CoreTokenField, Object>of(SessionTokenField.SESSION_HANDLE.getField(), SESSION_HANDLE));

        // When
        final PartialSession partialSession = factory.fromPartialToken(partialToken);

        // Then
        assertThat(partialSession.getSessionHandle()).isEqualTo(SESSION_HANDLE);
    }

    @Test
    public void shouldConstructPartialSessionWithLatestAccessTimeOnly() {
        // Given
        PartialToken partialToken = new PartialToken(ImmutableMap.<CoreTokenField, Object>of(
                SessionTokenField.LATEST_ACCESS_TIME.getField(), String.valueOf(UNIX_TIME)));

        // When
        final PartialSession partialSession = factory.fromPartialToken(partialToken);

        // Then
        assertThat(partialSession.getLatestAccessTime()).isEqualTo(UNIX_TIME_FORMATTED);
    }

    @Test
    public void shouldConstructPartialSessionWithMaxIdleExpirationTimeOnly() {
        // Given
        PartialToken partialToken = new PartialToken(ImmutableMap.<CoreTokenField, Object>of(
                SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField(), TimeUtils.fromUnixTime(UNIX_TIME)));

        // When
        final PartialSession partialSession = factory.fromPartialToken(partialToken);

        // Then
        assertThat(partialSession.getMaxIdleExpirationTime()).isEqualTo(UNIX_TIME_FORMATTED);
    }

    @Test
    public void shouldConstructPartialSessionWithMaxSessionExpirationTimeOnly() {
        // Given
        PartialToken partialToken = new PartialToken(ImmutableMap.<CoreTokenField, Object>of(
                SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField(), TimeUtils.fromUnixTime(UNIX_TIME)));

        // When
        final PartialSession partialSession = factory.fromPartialToken(partialToken);

        // Then
        assertThat(partialSession.getMaxSessionExpirationTime()).isEqualTo(UNIX_TIME_FORMATTED);
    }

    @Test
    public void shouldConstructPartialSessionWithAllInfoAvailable() {
        // Given
        PartialToken partialToken = new PartialToken(ImmutableMap.<CoreTokenField, Object>builder()
                .put(CoreTokenField.USER_ID, UNIVERSAL_ID)
                .put(SessionTokenField.REALM.getField(), REALM)
                .put(SessionTokenField.SESSION_HANDLE.getField(), SESSION_HANDLE)
                .put(SessionTokenField.LATEST_ACCESS_TIME.getField(), String.valueOf(UNIX_TIME))
                .put(SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField(), TimeUtils.fromUnixTime(UNIX_TIME))
                .put(SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField(), TimeUtils.fromUnixTime(UNIX_TIME))
                .build());
        given(identityUtils.getIdentityName(eq(UNIVERSAL_ID))).willReturn(USERNAME);

        // When
        final PartialSession partialSession = factory.fromPartialToken(partialToken);

        // Then
        assertThat(partialSession.getUsername()).isEqualTo(USERNAME);
        assertThat(partialSession.getUniversalId()).isEqualTo(UNIVERSAL_ID);
        assertThat(partialSession.getRealm()).isEqualTo(REALM);
        assertThat(partialSession.getSessionHandle()).isEqualTo(SESSION_HANDLE);
        assertThat(partialSession.getLatestAccessTime()).isEqualTo(UNIX_TIME_FORMATTED);
        assertThat(partialSession.getMaxIdleExpirationTime()).isEqualTo(UNIX_TIME_FORMATTED);
        assertThat(partialSession.getMaxSessionExpirationTime()).isEqualTo(UNIX_TIME_FORMATTED);
    }
}
