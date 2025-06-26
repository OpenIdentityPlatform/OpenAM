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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import org.forgerock.openam.session.stateless.cache.StatelessJWTCache;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * Limited amount of testing present here due to static code usage.
 */
public class StatelessSessionManagerTest {

    private StatelessSessionManager factory;
    private StatelessJWTCache mockCache;

    @BeforeMethod
    public void setup() {
        mockCache = mock(StatelessJWTCache.class);
        SessionServerConfig mockServerConfig = mock(SessionServerConfig.class);
        SessionServiceConfig mockServiceConfig = mock(SessionServiceConfig.class);
        factory = new StatelessSessionManager(mockCache, mockServerConfig, mockServiceConfig);
    }

    @Test
    public void shouldContainJwtInSessionID() {
        SessionID mockSession = mock(SessionID.class);
        given(mockSession.getTail()).willReturn("badger");
        assertThat(factory.containsJwt(mockSession)).isTrue();
    }

    @Test
    public void shouldExtractJWTFromSessionID() {
        SessionID id = mock(SessionID.class);
        given(id.getTail()).willReturn("badger=");
        given(id.isC66Encoded()).willReturn(true);
        assertThat(StatelessSessionManager.getJWTFromSessionID(id, true)).isEqualTo("badger.");
    }

    @Test
    public void shouldReturnNullIfNoJWTInSessionID() {
        SessionID id = mock(SessionID.class);
        given(id.getTail()).willReturn(null);
        assertThat(StatelessSessionManager.getJWTFromSessionID(id, true)).isNull();
    }

    @Test
    public void shouldReturnNullIfNoSessionIDProvided() {
        assertThat(StatelessSessionManager.getJWTFromSessionID(null, true)).isNull();
    }

    /**
     * OpenAM's C66 encoding/decoding in SessionID is lossy. In particular, c66 decoding changes base64url-encoded JWTs
     * into normal Base64 encoding. While this loses no information when decoding the JWT, it does cause any
     * signature to fail due to the input bytes changing. Rather than fixing the c66 encoding to be lossless (which
     * could break agents and other software that expects to be able to decode session ids), we instead work around
     * the problem by undoing the damage. We can always do this losslessly for JWTs.
     */
    @Test
    public void shouldUndoC66DecodingDamageToJwt() {
        // Given
        final String fullJwtAlphabet = ".-_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        // NB: '*' indicates that the sid is c66-encoded, '@' separates the encrypted id from the tail part (no exts).
        final SessionID sessionID = new SessionID("*@" + fullJwtAlphabet);

        // When
        final String result = StatelessSessionManager.getJWTFromSessionID(sessionID, true);

        // Then
        assertThat(result).isEqualTo(fullJwtAlphabet);
    }

    @Test
    public void shouldNotUndoC66DecodingIfNotAsked() {
        // Given
        final String fullJwtAlphabet = ".-_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        // NB: '*' indicates that the sid is c66-encoded, '@' separates the encrypted id from the tail part (no exts).
        final SessionID sessionID = new SessionID("*@" + fullJwtAlphabet);

        // When
        final String result = StatelessSessionManager.getJWTFromSessionID(sessionID, false);

        // Then
        assertThat(result).isEqualTo(sessionID.getTail());
    }

    @Test
    public void shouldNotUndoC66DecodingIfNotEncoded() {
        // Given
        final String fullJwtAlphabet = ".-_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final SessionID sessionID = new SessionID("notencoded@" + fullJwtAlphabet);

        // When
        final String result = StatelessSessionManager.getJWTFromSessionID(sessionID, true);

        // Then
        assertThat(result).isEqualTo(fullJwtAlphabet);
        assertThat(sessionID.isC66Encoded()).isFalse();
    }
}