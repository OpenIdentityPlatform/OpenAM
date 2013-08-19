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
package com.sun.identity.sm.ldap.adapters;

import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.CoreTokenConfig;
import com.sun.identity.sm.ldap.TokenTestUtils;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.fields.SessionTokenField;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import com.sun.identity.sm.ldap.utils.blob.TokenBlobUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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

        // When
        Token result = adapter.toToken(adapter.fromToken(token));

        // Then
        TokenTestUtils.compareTokens(result, token);
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
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
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
        String latestAccessTime = "12345";

        // start by making an IntenalSession that has latestAccessTime set.
        JSONSerialisation serialisation = new JSONSerialisation(mock(Debug.class));
        String serialisedSession = "{\"latestAccessTime\":" + latestAccessTime + "}";
        InternalSession session = serialisation.deserialise(serialisedSession, InternalSession.class);

        // some additional required mocking
        given(tokenIdFactory.toSessionTokenId(eq(session))).willReturn("badger");
        given(jsonSerialisation.serialise(any())).willReturn(serialisedSession);

        // When
        Token token = adapter.toToken(session);

        // Then
        String value = token.getValue(SessionTokenField.LATEST_ACCESS_TIME.getField());
        assertEquals(value, latestAccessTime);
    }

    @Test
    public void shouldFilterLatestAccessTime() throws CoreTokenException {
        // Given
        Token token = new Token("badger", TokenType.SESSION);
        String someJSONLikeText = "{\"clientDomain\":null,\"creationTime\":1376307674,\"isISStored\":true,\"latestAccessTime\":1376308558,\"maxCachingTime\":3}";
        token.setBlob(someJSONLikeText.getBytes());
        TokenBlobUtils utils = new TokenBlobUtils();

        // When
        adapter.filterLatestAccessTime(token);

        // Then
        String contents = utils.getBlobAsString(token);
        assertFalse(contents.contains(SessionTokenField.LATEST_ACCESS_TIME.getInternalSessionFieldName()));
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
