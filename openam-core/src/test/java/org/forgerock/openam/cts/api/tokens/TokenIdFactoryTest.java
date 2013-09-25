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
package org.forgerock.openam.cts.api.tokens;

import com.iplanet.dpro.session.SessionID;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.utils.KeyConversion;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenIdFactoryTest {

    private TokenIdFactory factory;

    private KeyConversion conversion;

    @BeforeClass
    public void setUp() {
        conversion = mock(KeyConversion.class);

        factory = new TokenIdFactory(conversion);
    }

    @Test
    public void shouldUseKeyConversionForSAMLTokens() {

        // Given
        String key = "badger";

        // When
        factory.toSAMLPrimaryTokenId(key);
        factory.toSAMLSecondaryTokenId(key);

        // Then
        verify(conversion, times(2)).encodeKey(key);
    }

    @Test
    public void shouldUseKeyConversionForSessionTokens() {

        // Given
        SessionID session = new SessionID("badger");

        // When
        factory.toSessionTokenId(session);

        // Then
        verify(conversion).encryptKey(session);
    }

    @Test
    public void shouldGetOAuthTokenIdWhenSet() {

        // Given
        String key = "badger";

        // When
        String result = factory.getOAuthTokenId(key);

        // Then
        assertEquals(result, key);
    }

    @Test
    public void shouldGenerateRandomIdWhenOAuthTokenIdNotSet() {

        // Given

        // When
        String result = factory.getOAuthTokenId(null);

        // Then
        assertNotNull(result);
    }
}
