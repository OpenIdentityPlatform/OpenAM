/**
 * Copyright 2013 ForgeRock, Inc.
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
import com.sun.identity.sm.ldap.CoreTokenConfig;
import com.sun.identity.sm.ldap.TokenTestUtils;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import org.testng.annotations.Test;

import java.util.Calendar;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SessionAdapterTest {
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
        TokenIdFactory tokenIdFactory = mock(TokenIdFactory.class);
        given(tokenIdFactory.toSessionTokenId(any(InternalSession.class))).willReturn(sessionId);
        // Ensure Session User is ferret
        CoreTokenConfig config = mock(CoreTokenConfig.class);
        given(config.getUserId(any(InternalSession.class))).willReturn(userId);
        // Ensure expiry date is now basd on the epoched time in seconds format.
        given(session.getExpirationTime()).willReturn(dataConversion.toEpochedSeconds(now));

        // Avoid serialisation when using mock InternalSessions
        JSONSerialisation serialisation = mock(JSONSerialisation.class);
        given(serialisation.deserialise(anyString(), eq(InternalSession.class))).willReturn(session);
        given(serialisation.serialise(any())).willReturn(new String(mockByteData));

        SessionAdapter adapter = new SessionAdapter(
                tokenIdFactory,
                config,
                serialisation,
                dataConversion);

        Token token = new Token(sessionId, TokenType.SESSION);
        token.setUserId(userId);
        token.setExpiryTimestamp(now);
        token.setBlob(mockByteData);

        // When
        Token result = adapter.toToken(adapter.fromToken(token));

        // Then
        TokenTestUtils.compareTokens(result, token);
    }
}
