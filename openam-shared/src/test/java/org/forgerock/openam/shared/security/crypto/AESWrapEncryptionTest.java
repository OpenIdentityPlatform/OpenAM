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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.shared.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;

import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AESWrapEncryptionTest {

    private AESWrapEncryption aesWrapEncryption;

    @BeforeMethod
    public void initialise() throws Exception {
        MockitoAnnotations.initMocks(this);
        aesWrapEncryption = new AESWrapEncryption(new PBKDF2KeyDerivation("SHA1", 10000));
        aesWrapEncryption.setPassword("password");
    }

    @Test(dataProvider = "paddingValues")
    public void shouldApplyPkcs5PaddingCorrectly(byte[] input, byte[] expectedPadding) throws Exception {
        byte[] padded = AESWrapEncryption.pkcs5pad(input);
        assertThat(padded).isEqualTo(expectedPadding);
    }

    @Test(dataProvider = "paddingValues")
    public void shouldRemovePkcs5PaddingCorrectly(byte[] expectedOutput, byte[] input) throws Exception {
        byte[] unpadded = AESWrapEncryption.pkcs5unpad(input);
        assertThat(unpadded).isEqualTo(expectedOutput);
    }

    @Test(dataProvider = "paddingValues")
    public void shouldRoundTripThroughPadding(byte[] input, byte[] expectedPadding) throws Exception {
        assertThat(AESWrapEncryption.pkcs5unpad(AESWrapEncryption.pkcs5pad(input.clone()))).isEqualTo(input);
    }

    @Test(dataProvider = "paddingValues", expectedExceptions = BadPaddingException.class)
    public void shouldRejectInvalidPadding(byte[] dummy, byte[] paddedInput) throws Exception {
        paddedInput[paddedInput.length - 1] ^= 0x01;
        AESWrapEncryption.pkcs5unpad(paddedInput);
    }

    @Test
    public void shouldRoundtripCorrectly() throws Exception {
        // Given
        String content = "A test message to be encrypted";

        // When
        byte[] encrypted = aesWrapEncryption.encrypt(content.getBytes(StandardCharsets.UTF_8));
        byte[] decrypted = aesWrapEncryption.decrypt(encrypted);

        // Then
        assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo(content);
    }

    @Test
    public void shouldBeAbleToEncryptSmallInputs() {
        // Given
        String input = "A";

        // When
        byte[] encrypted = aesWrapEncryption.encrypt(input.getBytes(StandardCharsets.UTF_8));

        // Then
        assertThat(encrypted).isNotNull();
        assertThat(new String(aesWrapEncryption.decrypt(encrypted), StandardCharsets.UTF_8)).isEqualTo(input);
    }

    @Test
    public void shouldReturnNullIfDecryptionFails() {
        // Given
        byte[] data = new byte[16];
        ThreadLocalRandom.current().nextBytes(data);

        // When
        byte[] decrypted = aesWrapEncryption.decrypt(data);

        // Then
        assertThat(decrypted).isNull();
    }

    @Test
    public void shouldResistTampering() {
        // Given
        String content = "A test message to be encrypted";
        byte[] encrypted = aesWrapEncryption.encrypt(content.getBytes(StandardCharsets.UTF_8));

        // When
        for (int i = 0; i < encrypted.length; ++i) {
            byte[] altered = encrypted.clone();
            altered[i] ^= 0x01; // Flip a bit in this byte position
            byte[] decrypted = aesWrapEncryption.decrypt(altered);
            assertThat(decrypted).isNull();
        }

        // Then - success
    }

    @DataProvider
    public Object[][] paddingValues() {
        return new Object[][] {
                { new byte[0],                          new byte[] { 8, 8, 8, 8, 8, 8, 8, 8 } },
                { new byte[] { 1 },                     new byte[] { 1, 7, 7, 7, 7, 7, 7, 7 } },
                { new byte[] { 1, 2 },                  new byte[] { 1, 2, 6, 6, 6, 6, 6, 6 } },
                { new byte[] { 1, 2, 3},                new byte[] { 1, 2, 3, 5, 5, 5, 5, 5 } },
                { new byte[] { 1, 2, 3, 4 },            new byte[] { 1, 2, 3, 4, 4, 4, 4, 4 } },
                { new byte[] { 1, 2, 3, 4, 5},          new byte[] { 1, 2, 3, 4, 5, 3, 3, 3 } },
                { new byte[] { 1, 2, 3, 4, 5, 6},       new byte[] { 1, 2, 3, 4, 5, 6, 2, 2 } },
                { new byte[] { 1, 2, 3, 4, 5, 6, 7},    new byte[] { 1, 2, 3, 4, 5, 6, 7, 1 } },
                { new byte[] { 1, 2, 3, 4, 5, 6, 7, 8}, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8,
                                                                     8, 8, 8, 8, 8, 8, 8, 8 } }
        };
    }
}