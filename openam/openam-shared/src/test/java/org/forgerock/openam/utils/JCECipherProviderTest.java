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

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.crypto.Cipher;
import java.security.Provider;
import java.security.Security;

import static org.fest.assertions.Assertions.assertThat;


/**
 * Unit tests for the standard cipher provider. It's pretty much impossible to mock the underlying Cipher/Provider
 * implementation, as the JVM requires all providers to be in signed jars. Instead we rely on well-known cipher
 * algorithms to test that this is working correctly. JDK 7 {@link Cipher} javadoc lists a number of algorithms that
 * are guaranteed to exist, but Java 6 and before do not guarantee this. These tests may therefore fail on environments
 * that lack basic AES cipher implementations.
 */
public class JCECipherProviderTest {
    /** An arbitrarily selected Cipher transformation that is guaranteed to exist on all JVMs. @see {@link Cipher}. */
    private static final String CIPHER_ALGORITHM = "AES/CBC/NoPadding";
    private static final String CIPHER_FILTER = "Cipher.AES";
    private static String preferredProvider;

    private JCECipherProvider cipherProvider;

    @BeforeClass
    public static void pickAProvider() {
        // Pick an available provider of our cipher algorithm.
        Provider[] providers = Security.getProviders(CIPHER_FILTER);
        if (providers == null || providers.length == 0) {
            throw new SkipException("No security provider available!");
        }
        preferredProvider = providers[0].getName();
    }


    @Test
    public void shouldUsePreferredProviderWhenAvailable() throws Exception {
        // Given
        cipherProvider = new JCECipherProvider(CIPHER_ALGORITHM, preferredProvider);

        // When
        Cipher result = cipherProvider.getCipher();

        // Then
        assertThat(result.getProvider().getName()).isEqualTo(preferredProvider);
    }

    @Test
    public void shouldFallbackOnAnyProviderWhenPreferredNotAvailable() throws Exception {
        // Given
        String unknownProvider = "notARealProvider";
        cipherProvider = new JCECipherProvider(CIPHER_ALGORITHM, unknownProvider);

        // When
        Cipher result = cipherProvider.getCipher();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProvider().getName()).isNotEqualTo(unknownProvider);
    }

    /**
     * Cipher providers should return null (rather than throwing an exception) if a matching cipher cannot be created.
     */
    @Test
    public void shouldReturnNullForUnknownAlgorithm() throws Exception {
        // Given
        cipherProvider = new JCECipherProvider("UnknownAlgorithm", preferredProvider);

        // When
        Cipher result = cipherProvider.getCipher();

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldReturnNullForUnknownPadding() throws Exception {
        // Given
        String transformation = CIPHER_ALGORITHM.replace("NoPadding", "UnknownWeirdPadding");
        cipherProvider = new JCECipherProvider(transformation, preferredProvider);

        // When
        Cipher result = cipherProvider.getCipher();

        // Then
        assertThat(result).isNull();
    }
}
