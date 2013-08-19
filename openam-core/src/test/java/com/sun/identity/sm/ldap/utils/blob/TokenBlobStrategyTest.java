/**
 * Copyright 2013 ForgeRock AS.
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
package com.sun.identity.sm.ldap.utils.blob;

import com.sun.identity.sm.ldap.CoreTokenConfig;
import com.sun.identity.sm.ldap.api.tokens.Token;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenBlobStrategyTest {
    private CoreTokenConfig config;
    private TokenStrategyFactory factory;

    @BeforeMethod
    public void setup() {
        config = mock(CoreTokenConfig.class);
        factory = mock(TokenStrategyFactory.class);
    }

    @Test
    public void shouldPerformAllStrategy() throws TokenStrategyFailedException {
        // Given
        BlobStrategy first = mock(BlobStrategy.class);
        BlobStrategy second = mock(BlobStrategy.class);
        BlobStrategy third = mock(BlobStrategy.class);

        given(factory.getStrategies(any(CoreTokenConfig.class)))
                .willReturn(Arrays.asList(first, second, third));

        Token mockToken = mock(Token.class);

        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        // When
        strategy.perfom(mockToken);

        // Then
        verify(first).perform(mockToken);
        verify(second).perform(mockToken);
        verify(third).perform(mockToken);
    }

    @Test
    public void shouldReverseAllStrategy() throws TokenStrategyFailedException {
        // Given
        BlobStrategy first = mock(BlobStrategy.class);
        BlobStrategy second = mock(BlobStrategy.class);
        BlobStrategy third = mock(BlobStrategy.class);

        given(factory.getStrategies(any(CoreTokenConfig.class)))
                .willReturn(Arrays.asList(first, second, third));

        Token mockToken = mock(Token.class);

        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        // When
        strategy.reverse(mockToken);

        // Then
        verify(first).reverse(mockToken);
        verify(second).reverse(mockToken);
        verify(third).reverse(mockToken);
    }

    @Test
    public void shouldDoNothingWithNoStrategy() throws TokenStrategyFailedException {
        // Given
        Token mockToken = mock(Token.class);
        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        // When
        strategy.perfom(mockToken);

        // Then
        verify(mockToken, times(0)).getBlob();
    }
}
