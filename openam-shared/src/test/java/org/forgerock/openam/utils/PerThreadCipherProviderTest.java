/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.crypto.Cipher;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link PerThreadCipherProvider}.
 */
public class PerThreadCipherProviderTest {

    private PerThreadCipherProvider testProvider;

    @BeforeMethod
    public void setup() {
        testProvider = new PerThreadCipherProvider(new MockCipherProvider());
    }

    @Test
    public void shouldReturnSameCipherForSameThread() {
        // Given

        // When
        Cipher cipher1 = testProvider.getCipher();
        Cipher cipher2 = testProvider.getCipher();

        // Then
        assertThat(cipher1).isSameAs(cipher2);
    }

    @Test
    public void shouldReturnDifferentCiphersForDifferentThreads() throws Exception {
        // Given
        ExecutorService otherThread = Executors.newSingleThreadExecutor();

        // When
        Cipher cipher1 = testProvider.getCipher();
        Cipher cipher2 = otherThread.submit(getCipherInOtherThread()).get();

        // Then
        assertThat(cipher1).isNotSameAs(cipher2);
    }

    @Test
    public void shouldEvictLeastRecentlyAccessedElements() throws Exception {
        // Given
        int maxSize = 1;
        ExecutorService otherThread = Executors.newSingleThreadExecutor();
        testProvider = new PerThreadCipherProvider(new MockCipherProvider(), maxSize);

        // When
        Cipher cipher1 = testProvider.getCipher();
        // Get a cipher from another thread, causing maxSize (1) to be exceeded, evicting the least recently accessed entry
        otherThread.submit(getCipherInOtherThread()).get();
        // Accessing again from the main thread should cause a new cipher instance to be created
        Cipher cipher2 = testProvider.getCipher();

        // Then
        assertThat(cipher1).isNotSameAs(cipher2);
    }

    private Callable<Cipher> getCipherInOtherThread() {
        return new Callable<Cipher>() {
            public Cipher call() throws Exception {
                return testProvider.getCipher();
            }
        };
    }

    private static class MockCipherProvider implements CipherProvider {
        public Cipher getCipher() {
            return mock(Cipher.class);
        }
    }
}
