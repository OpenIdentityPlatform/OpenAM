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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils.blob;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

        byte[] data = new byte[0];

        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        // When
        strategy.perform(data);

        // Then
        verify(first).perform(any(byte[].class));
        verify(second).perform(any(byte[].class));
        verify(third).perform(any(byte[].class));
    }

    @Test
    public void shouldReverseAllStrategy() throws TokenStrategyFailedException {
        // Given
        BlobStrategy first = mock(BlobStrategy.class);
        BlobStrategy second = mock(BlobStrategy.class);
        BlobStrategy third = mock(BlobStrategy.class);

        given(factory.getStrategies(any(CoreTokenConfig.class)))
                .willReturn(Arrays.asList(first, second, third));

        byte[] data = new byte[0];

        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        // When
        strategy.reverse(data);

        // Then
        verify(first).reverse(any(byte[].class));
        verify(second).reverse(any(byte[].class));
        verify(third).reverse(any(byte[].class));
    }

    @Test
    public void shouldDoNothingWithNoStrategy() throws TokenStrategyFailedException {
        // Given
        byte[] data = new byte[0];
        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        // When
        byte[] result = strategy.perform(data);

        // Then
        assertThat(result).isEqualTo(data);
    }

    @Test
    public void shouldNotModifyProvidedByteArray() throws TokenStrategyFailedException {
        // Given
        byte[] data = "badger".getBytes();

        BlobStrategy first = mock(BlobStrategy.class);
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        given(first.perform(captor.capture())).willReturn(data);

        given(factory.getStrategies(any(CoreTokenConfig.class))).willReturn(Arrays.asList(first));
        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        // When
        strategy.perform(data);

        // Then
        Object dataRef = data;
        Object captorRef = captor.getValue();
        if (dataRef == captorRef) { // Verify the references are different (the contents will be the same)
            fail();
        }
    }

    @Test
    public void shouldReturnNullForNullInputDuringPerform() throws Exception {
        byte[] data = null;

        BlobStrategy first = mock(BlobStrategy.class);

        given(factory.getStrategies(any(CoreTokenConfig.class))).willReturn(Arrays.asList(first));
        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        assertThat(strategy.perform(data)).isNull();
        verify(first, times(0)).perform(any(byte[].class));
    }

    @Test
    public void shouldReturnNullForNullInputDuringReverse() throws Exception {
        byte[] data = null;

        BlobStrategy first = mock(BlobStrategy.class);

        given(factory.getStrategies(any(CoreTokenConfig.class))).willReturn(Arrays.asList(first));
        TokenBlobStrategy strategy = new TokenBlobStrategy(factory, config);

        assertThat(strategy.perform(data)).isNull();
        verify(first, times(0)).reverse(any(byte[].class));
    }
}
