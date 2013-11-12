/**
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package org.forgerock.openam.cts.adapters;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.*;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SessionAdapterTest {
    private SessionAdapter adapter;
    private TokenIdFactory tokenIdFactory;
    private CoreTokenConfig coreTokenConfig;
    private JSONSerialisation jsonSerialisation;
    private LDAPDataConversion ldapDataConversion;
    private TokenBlobUtils blobUtils;

    @BeforeMethod
    public void setup() {
        tokenIdFactory = mock(TokenIdFactory.class);
        coreTokenConfig = mock(CoreTokenConfig.class);
        jsonSerialisation = mock(JSONSerialisation.class);
        ldapDataConversion = mock(LDAPDataConversion.class);
        blobUtils = new TokenBlobUtils();
        adapter = new SessionAdapter(tokenIdFactory, coreTokenConfig, jsonSerialisation, ldapDataConversion, blobUtils);
    }

    @Test
    public void shouldSerialiseAndDeserialiseToken() {
        // Given

        // Sessions can only measure time to the closest second.
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);

        String userId = "ferret";
        String sessionId = "badger";
        byte[] mockByteData = {};
        LDAPDataConversion dataConversion = new LDAPDataConversion();

        InternalSession session = mock(InternalSession.class);
        // Ensure Session ID is badger
        given(tokenIdFactory.toSessionTokenId(any(InternalSession.class))).willReturn(sessionId);
        // Ensure Session User is ferret
        given(coreTokenConfig .getUserId(any(InternalSession.class))).willReturn(userId);
        // Ensure expiry date is now based on the epoched time in seconds format.
        given(session.getExpirationTime()).willReturn(dataConversion.toEpochedSeconds(now));

        SessionID mockSessionID = mock(SessionID.class);
        given(mockSessionID.toString()).willReturn(sessionId);
        given(session.getID()).willReturn(mockSessionID);

        // Avoid serialisation when using mock InternalSessions
        given(jsonSerialisation.deserialise(anyString(), eq(InternalSession.class))).willReturn(session);
        given(jsonSerialisation.serialise(any())).willReturn(new String(mockByteData));

        adapter = new SessionAdapter(
                tokenIdFactory,
                coreTokenConfig ,
                jsonSerialisation,
                dataConversion, blobUtils);

        Token token = new Token(sessionId, TokenType.SESSION);
        token.setUserId(userId);
        token.setExpiryTimestamp(now);
        token.setBlob(mockByteData);
        token.setAttribute(SessionTokenField.SESSION_ID.getField(), "badger");

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
        token.setBlob("{\"clientDomain\":null,\"creationTime\":1376307674,\"isISStored\":true,\"maxCachingTime\":3}".getBytes());

        // need a real JSONSerialisation for this test
        JSONSerialisation serialisation = new JSONSerialisation();
        adapter = new SessionAdapter(tokenIdFactory, coreTokenConfig, serialisation, ldapDataConversion, blobUtils);

        // When
        InternalSession session = adapter.fromToken(token);

        // Then
        // if latestAccessTime was zero, this would fail
        long epochedSeconds = System.currentTimeMillis() / 1000;
        long idleTime = session.getIdleTime();
        assertTrue(idleTime < epochedSeconds);

    }

    @Test
    public void shouldAssignAttributeFromSessionLatestAccessTime() {
        // Given
        long timestamp = 12345l;

        InternalSession mockSession = mock(InternalSession.class);
        SessionID mockSessionID = mock(SessionID.class);

        given(mockSessionID.toString()).willReturn("badger");
        given(jsonSerialisation.deserialise(anyString(), any(Class.class))).willReturn(mockSession);
        given(mockSession.getExpirationTime()).willReturn(timestamp);
        given(mockSession.getID()).willReturn(mockSessionID);

        // some additional required mocking
        given(tokenIdFactory.toSessionTokenId(eq(mockSession))).willReturn("badger");
        given(jsonSerialisation.serialise(any())).willReturn("");

        // When
        adapter.toToken(mockSession);

        // Then
        verify(ldapDataConversion).fromEpochedSeconds(eq(timestamp));
    }

    @Test
    public void shouldAssignSessionID() {
        // Given
        long timestamp = 12345l;

        InternalSession mockSession = mock(InternalSession.class);
        SessionID mockSessionID = mock(SessionID.class);

        String sessionId = "badger";
        given(mockSessionID.toString()).willReturn(sessionId);
        given(jsonSerialisation.deserialise(anyString(), any(Class.class))).willReturn(mockSession);
        given(mockSession.getExpirationTime()).willReturn(timestamp);
        given(mockSession.getID()).willReturn(mockSessionID);

        // some additional required mocking
        given(tokenIdFactory.toSessionTokenId(eq(mockSession))).willReturn(sessionId);
        given(jsonSerialisation.serialise(any())).willReturn("");

        // When
        Token token = adapter.toToken(mockSession);

        // Then
        assertThat(token.getValue(SessionTokenField.SESSION_ID.getField())).isEqualTo(sessionId);
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
}
