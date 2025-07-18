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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.adapters;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.getCalendarInstance;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.TimeUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionState;

public class SessionAdapterTest {

    private SessionAdapter adapter;
    private TokenIdFactory mockTokenIdFactory;
    private CoreTokenConfig mockCoreTokenConfig;
    private JSONSerialisation mockJsonSerialisation;
    private TokenBlobUtils blobUtils;
    private DNWrapper dnWrapper;

    @BeforeMethod
    public void setup() {
        mockTokenIdFactory = mock(TokenIdFactory.class);
        mockCoreTokenConfig = mock(CoreTokenConfig.class);
        mockJsonSerialisation = mock(JSONSerialisation.class);
        blobUtils = new TokenBlobUtils();
        dnWrapper = mock(DNWrapper.class);
        adapter = new SessionAdapter(mockTokenIdFactory, mockCoreTokenConfig, mockJsonSerialisation, blobUtils,
                dnWrapper);
    }

    @Test
    public void shouldDeserializeTokenAttributes() {
        // Given

        // Sessions can only measure time to the closest second.
        Calendar now = getCalendarInstance();
        now.set(Calendar.MILLISECOND, 0);
        long mockTimestamp = TimeUtils.toUnixTime(now);

        String mockUserId = "ferret";
        String mockSessionId = "badger";
        String mockSessionHandle = SessionID.SHANDLE_SCHEME_PREFIX + "weasel";
        byte[] mockByteData = {};

        InternalSession mockSession = mock(InternalSession.class);
        givenMockSessionID(mockSession, mockSessionId);
        given(mockCoreTokenConfig.getUserId(eq(mockSession))).willReturn(mockUserId);
        given(mockSession.getExpirationTime(MILLISECONDS)).willReturn(SECONDS.toMillis(mockTimestamp));
        given(mockSession.getSessionHandle()).willReturn(mockSessionHandle);
        given(mockSession.getState()).willReturn(SessionState.VALID);
        given(mockSession.getClientDomain()).willReturn("REALM");
        given(dnWrapper.orgNameToRealmName(anyString())).willReturn("PRETTY_REALM");

        // Avoid serialisation when using mock InternalSessions
        given(mockJsonSerialisation.deserialise(anyString(), eq(InternalSession.class))).willReturn(mockSession);
        given(mockJsonSerialisation.serialise(any(InternalSession.class))).willReturn(new String(mockByteData));

        Token token = new Token(mockSessionId, TokenType.SESSION);
        token.setUserId(mockUserId);
        token.setExpiryTimestamp(now);
        token.setBlob(mockByteData);
        token.setAttribute(SessionTokenField.SESSION_ID.getField(), mockSessionId);
        token.setAttribute(SessionTokenField.SESSION_HANDLE.getField(), mockSessionHandle);
        token.setAttribute(SessionTokenField.SESSION_STATE.getField(), "VALID");
        token.setAttribute(SessionTokenField.REALM.getField(), "PRETTY_REALM");
        SessionAdapter.setDateAttributeFromMillis(token, SessionTokenField.MAX_SESSION_EXPIRATION_TIME, 0);
        SessionAdapter.setDateAttributeFromMillis(token, SessionTokenField.MAX_IDLE_EXPIRATION_TIME, 0);

        // When
        Token result = adapter.toToken(adapter.fromToken(token));

        // Then
        TokenTestUtils.assertTokenEquals(result, token);
    }

    @Test
    public void shouldRestoreLatestAccessTimeFromAttribute() {
        // Given
        String latestAccessTime = "12345";

        Token token = new Token("badger", TokenType.SESSION);
        token.setAttribute(SessionTokenField.LATEST_ACCESS_TIME.getField(), latestAccessTime);

        // blob contents are missing the latestAccessTime value
        token.setBlob("{\"clientDomain\":null,\"creationTime\":1376307674,\"maxCachingTime\":3}".getBytes());

        // need a real JSONSerialisation for this test
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        JSONSerialisation serialisation = new JSONSerialisation(mapper);
        adapter = new SessionAdapter(mockTokenIdFactory, mockCoreTokenConfig, serialisation, blobUtils, dnWrapper);

        // When
        InternalSession session = adapter.fromToken(token);

        // Then
        // if latestAccessTime was zero, this would fail
        long epochedSeconds = currentTimeMillis() / 1000;
        long idleTime = session.getIdleTime();
        assertTrue(idleTime < epochedSeconds);

    }

    @Test
    public void shouldAssignUserIDToTokenAttribute() {
        // Given
        InternalSession mockSession = prototypeMockInternalSession();
        String mockUserID = "badger";
        given(mockCoreTokenConfig.getUserId(eq(mockSession))).willReturn(mockUserID);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.<String>getAttribute(CoreTokenField.USER_ID)).isEqualTo(mockUserID);
    }

    @Test
    public void shouldAssignExpiryTimestampFromSessionLatestAccessTime() {
        // Given
        InternalSession mockSession = prototypeMockInternalSession();

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.getExpiryTimestamp()).isNotNull();
    }

    @Test
    public void shouldAssignSessionIDToTokenAttribute() {
        // Given
        InternalSession mockSession = prototypeMockInternalSession();
        String mockSessionID = "badger";
        givenMockSessionID(mockSession, mockSessionID);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.<String>getAttribute(SessionTokenField.SESSION_ID.getField())).isEqualTo(mockSessionID);
    }

    @Test
    public void shouldAssignSessionHandleToTokenAttribute() {
        // Given
        InternalSession mockSession = prototypeMockInternalSession();
        String mockSessionHandle = SessionID.SHANDLE_SCHEME_PREFIX + "ferret";
        given(mockSession.getSessionHandle()).willReturn(mockSessionHandle);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.<String>getAttribute(SessionTokenField.SESSION_HANDLE.getField())).isEqualTo(mockSessionHandle);
    }

    @Test
    public void shouldAssignSessionStateInvalidToTokenAttribute() {
        // Given
        InternalSession mockSession = prototypeMockInternalSession();
        given(mockSession.getState()).willReturn(SessionState.INVALID);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.<String>getAttribute(SessionTokenField.SESSION_STATE.getField())).isEqualTo("INVALID");
    }

    @Test
    public void shouldAssignSessionStateValidToTokenAttribute() {
        // Given
        InternalSession mockSession = prototypeMockInternalSession();
        given(mockSession.getState()).willReturn(SessionState.VALID);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.<String>getAttribute(SessionTokenField.SESSION_STATE.getField())).isEqualTo("VALID");
    }

    @Test
    public void shouldAssignSessionStateDestroyedToTokenAttribute() {
        // Given
        InternalSession mockSession = prototypeMockInternalSession();
        given(mockSession.getState()).willReturn(SessionState.DESTROYED);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.<String>getAttribute(SessionTokenField.SESSION_STATE.getField())).isEqualTo("DESTROYED");
    }

    @Test
    public void shouldAssignMaxSessionExpirationTimeToTokenAttribute() {
        // Given
        long mockTimestampMillis = 1_376_308_558_000L;
        InternalSession mockSession = prototypeMockInternalSession();
        given(mockSession.getMaxSessionExpirationTime(MILLISECONDS)).willReturn(mockTimestampMillis);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        Calendar maxSessionExpirationTime = token.getAttribute(SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField());
        assertThat(maxSessionExpirationTime.getTimeInMillis()).isEqualTo(mockTimestampMillis);
    }

    @Test
    public void shouldAssignMaxIdleExpirationTimeToTokenAttribute() {
        // Given
        long mockTimestampMillis = 1_376_308_558_000L;
        InternalSession mockSession = prototypeMockInternalSession();
        given(mockSession.getMaxIdleExpirationTime(MILLISECONDS)).willReturn(mockTimestampMillis);

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        Calendar maxIdleExpirationTime = token.getAttribute(SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField());
        assertThat(maxIdleExpirationTime.getTimeInMillis()).isEqualTo(mockTimestampMillis);
    }

    @Test
    public void shouldAssignRealmToTokenAttribute() {
        //Given
        InternalSession mockSession = prototypeMockInternalSession();

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        String realm = token.getAttribute(SessionTokenField.REALM.getField());
        assertThat(realm).isEqualTo("PRETTY_REALM");
    }

    @Test
    public void shouldFilterLatestAccessTime() throws CoreTokenException {
        // Given
        Token token = new Token("badger", TokenType.SESSION);
        String latestAccessTime = "\"latestAccessTime\":1376308558,";
        String someJSONLikeText = "{\"clientDomain\":null,\"creationTime\":1376307674,\"isISStored\":true,"
                + latestAccessTime + "\"maxCachingTime\":3}";
        token.setBlob(someJSONLikeText.getBytes());
        TokenBlobUtils utils = new TokenBlobUtils();

        // When
        adapter.filterLatestAccessTime(token);

        // Then
        String contents = utils.getBlobAsString(token);
        // Present in the original json text.
        assertTrue(someJSONLikeText.contains(latestAccessTime));
        // Removed in the treated json text.
        assertFalse(contents.contains(latestAccessTime));
    }

    @Test
    public void shouldHandleMissingCommaInBlob() {
        // Given
        String latestAccessTime = "1376308558";
        Token token = new Token("badger", TokenType.SESSION);
        String someJSONLikeText = "{\"latestAccessTime\":" + latestAccessTime + "}";
        token.setBlob(someJSONLikeText.getBytes());

        // When
        String result = adapter.filterLatestAccessTime(token);

        // Then
        assertEquals(result, latestAccessTime);
    }

    @Test
    public void shouldDoNothingIfLatestAccessTimeNotFound() throws UnsupportedEncodingException {
        // Given
        Token mockToken = mock(Token.class);
        given(mockToken.getBlob()).willReturn("badger".getBytes(TokenBlobUtils.ENCODING));

        // When
        adapter.filterLatestAccessTime(mockToken);

        // Then
        verify(mockToken, times(0)).setBlob(any(byte[].class));
    }

    @Test
    public void shouldLocateValidFieldInJSON() {
        String json = "{\"clientDomain\":null,\"creationTime\":1376307674,\"isISStored\":true,\"latestAccessTime\":1376308558,\"maxCachingTime\":3}";
        assertEquals(1, adapter.findIndexOfValidField(json));
    }

    @Test
    public void shouldIndicateNoValidFieldsInJSON() {
        assertEquals(-1, adapter.findIndexOfValidField(""));
    }

    private InternalSession prototypeMockInternalSession() {
        long mockTimestamp = 12345l;
        InternalSession mockSession = mock(InternalSession.class);
        String sessionHandle = SessionID.SHANDLE_SCHEME_PREFIX + "ferret";

        givenMockSessionID(mockSession, "badger");
        given(mockSession.getExpirationTime(SECONDS)).willReturn(mockTimestamp);
        given(mockSession.getSessionHandle()).willReturn(sessionHandle);
        given(mockSession.getState()).willReturn(SessionState.INVALID);
        given(mockSession.getClientDomain()).willReturn("REALM");
        given(dnWrapper.orgNameToRealmName(anyString())).willReturn("PRETTY_REALM");
        given(mockJsonSerialisation.serialise(any())).willReturn("");
        given(mockJsonSerialisation.deserialise(anyString(), eq(InternalSession.class))).willReturn(mockSession);

        return mockSession;
    }

    private void givenMockSessionID(InternalSession mockSession, String mockSessionIDString) {
        SessionID mockSessionID = mock(SessionID.class);
        given(mockSessionID.toString()).willReturn(mockSessionIDString);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockTokenIdFactory.toSessionTokenId(eq(mockSession))).willReturn(mockSessionIDString);
    }

}
