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

import java.util.Arrays;

import javax.crypto.interfaces.PBEKey;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.encode.Base64;

public class PBKDF2KeyDerivationTest {

    private PBKDF2KeyDerivation keyDerivation;

    @BeforeMethod
    public void createKDF() throws Exception {
        keyDerivation = new PBKDF2KeyDerivation("SHA1", 10000);
        keyDerivation.setPassword("password");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldRejectLessThan10000Iterations() {
        new PBKDF2KeyDerivation("SHA1", 9999);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldRejectInvalidMessageDigests() {
        new PBKDF2KeyDerivation("Flibble", 10000);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorIfNoPasswordConfigured() {
        keyDerivation.clear();
        keyDerivation.deriveSecretKey(128);
    }

    @Test
    public void shouldUseRandomSaltIfNotSupplied() throws Exception {
        final PBEKey key = keyDerivation.deriveSecretKey(128);
        assertThat(key.getSalt()).hasSize(16);
    }

    @Test
    public void shouldUseSuppliedSalt() throws Exception {
        byte[] salt = new byte[16];
        Arrays.fill(salt, (byte) 42);

        final PBEKey key = keyDerivation.deriveSecretKey(128, salt.clone());

        assertThat(key.getSalt()).isEqualTo(salt);
    }

    @Test
    public void shouldDeriveKeyOfRequestedSize() throws Exception {
        int keySize = 224;
        final PBEKey key = keyDerivation.deriveSecretKey(keySize);
        assertThat(key.getEncoded()).hasSize(keySize / 8);
    }

    @Test
    public void shouldDeriveDeterminsticKeyWhenGivenFixedSalt() throws Exception {
        byte[] salt = new byte[16];
        Arrays.fill(salt, (byte) 42);

        final PBEKey key = keyDerivation.deriveSecretKey(128, salt.clone());

        // Key derived via PBKDF2-HMAC-SHA1 with 10,000 iterations and fixed salt of 16 bytes of the literal 42.
        assertThat(key.getEncoded()).isEqualTo(Base64.decode("D2kG4OYraGm4A8Htz9k3VA=="));
    }
}