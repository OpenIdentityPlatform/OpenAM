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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.openam.utils.CipherProvider;
import org.forgerock.openam.utils.Providers;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.services.util.ConfigurableKey;
import com.sun.identity.shared.debug.Debug;

/**
 * Encryption using AES KeyWrap deterministic authenticated key-wrapping encryption scheme described in <a
 * href="https://tools.ietf.org/html/rfc3394">RFC 3394</a>, with PBKDF2-HMAC-SHA1 to derive unique encryption keys
 * for each encrypted key from the AM encryption password. The AES KeyWrap algorithm provides strong integrity-protected
 * <em>deterministic</em> encryption that is space efficient (encrypted keys are only slightly larger than the
 * original data), but relatively inefficient performance-wise (still within the microsecond range for reasonable
 * inputs). Key wrapping modes are generally robust in the face of misuse or low-quality random number generators.
 * For instance, AES KeyWrap requires no nonce or random IV, and so the security cannot be compromised by problems in
 * the generation of these components.
 * <p>
 * As the password provided by the {@link ConfigurableKey} interface cannot be guaranteed to be of high quality, this
 * implementation uses {@link PBKDF2KeyDerivation} to derive unique keys for each item that is encrypted, using
 * unique 128-bit random salt. This provides significantly improved security compared to just using the encryption
 * password directly or deriving a single master encryption key from it, and as a side-effect also regains
 * unpredictable output from AES KeyWrap as a unique key is used to encrypt each plaintext.
 * <p>
 * AES KeyWrap (AESKW) is a NIST/FIPS-approved key-wrap algorithm. PBKDF2 with HMAC-SHA1/SHA256/SHA384/SHA512 is a
 * NIST-approved password-based key deriviation function. We use or exceed all current NIST recommendations for
 * parameters to these algorithms. The only non-standard aspect is our use of PKCS#5 padding with AESKW rather than
 * the separate <a href="https://tools.ietf.org/html/rfc5649">padded variant of AESKW</a>. This is for implementation
 * simplicity given that our supported JREs only provide the non-padded AESKW algorithm and PKCS#5 padding is easier
 * to implement correctly.
 */
public class AESWrapEncryption implements AMEncryption, ConfigurableKey {
    private static final Debug DEBUG = Debug.getInstance("amSDK");

    private static final byte VERSION = 2; // v1 is JCEEncryption
    private static final int AESWRAP_BLOCK_SIZE = 8;

    private static final int CACHE_SIZE = Integer.getInteger("amCryptoCacheSize", 1024);

    private static final String CIPHER_PROVIDER_ALGORITHM
            = Double.parseDouble(System.getProperty("java.specification.version")) >= 17 ? "AESWrapPad" : "AESWrap";

    private static final CipherProvider CIPHER_PROVIDER = Providers.cipherProvider(CIPHER_PROVIDER_ALGORITHM, null, CACHE_SIZE);

    private static final int KEY_SIZE = Integer.getInteger("org.forgerock.openam.encryption.key.size", 128);

    private final PBKDF2KeyDerivation keyDerivation;

    @VisibleForTesting
    AESWrapEncryption(PBKDF2KeyDerivation keyDerivation) {
        this.keyDerivation = keyDerivation;
    }

    /**
     * Public default constructor.
     */
    public AESWrapEncryption() {
        this(new PBKDF2KeyDerivation());
    }

    /**
     * Sets the encryption key from a password. The actual encryption key will be derived from the password using
     * the <a href="">PBKDF2</a> key derivation algorithm. Use the system properties
     * {@code org.forgerock.openam.encryption.key.salt}, {@code org.forgerock.openam.encryption.key.iterations} and
     * {@code org.forgerock.openam.encryption.key.size} to set the salt (no default, must be set), number of
     * iterations (defaults to 250,000) and output key size (defaults to 128-bits, can be 128, 192 or 256).
     *
     * @param password the password to use to derive the encryption key. Must be ASCII.
     * @throws Exception if an error occurs when deriving a key from the password.
     */
    @Override
    public void setPassword(final String password) throws Exception {
        keyDerivation.setPassword(password);
    }

    /**
     * Encrypts the data using AESWrap and the configured key.
     *
     * @param rawData the data to encrypt.
     * @return the encrypted data.
     * @throws IllegalStateException if a key has not been configured.
     * @throws RuntimeException if the data cannot be encrypted for any reason.
     */
    @Override
    public byte[] encrypt(final byte[] rawData) {
        final PBEKey encryptionKey = keyDerivation.deriveSecretKey(KEY_SIZE);

        try {
            Cipher cipher = CIPHER_PROVIDER.getCipher();
            cipher.init(Cipher.WRAP_MODE, new SecretKeySpec(encryptionKey.getEncoded(), "AES"));
            final byte[] encrypted = cipher.wrap(new SecretKeySpec(pkcs5pad(rawData), "RAW"));
            return formatEncryptedMessage(encryptionKey, encrypted);
        } catch (GeneralSecurityException e) {
            DEBUG.error("AESWrapEncryption: Failed to encrypt data", e);
            return null;
        }
    }

    /**
     * Decrypts the data using AESWrap and the configured key.
     *
     * @param encData the data to decrypt.
     * @return the decrypted data.
     * @throws IllegalStateException if a key has not been configured.
     * @throws RuntimeException if the data cannot be decrypted for any reason.
     */
    @Override
    public byte[] decrypt(final byte[] encData) {
        if (encData == null || encData.length < 2 || encData[0] != VERSION) {
            DEBUG.error("AESWrapEncryption: malformed input");
            return null;
        }

        final int saltLen = encData[1] & 0xFF;
        if (saltLen < 0 || saltLen > encData.length - 2) {
            DEBUG.error("AESWrapEncryption: invalid salt length {}", saltLen);
            return null;
        }
        final byte[] salt = Arrays.copyOfRange(encData, 2, saltLen + 2);
        final PBEKey encryptionKey = keyDerivation.deriveSecretKey(KEY_SIZE, salt);
        final byte[] data = Arrays.copyOfRange(encData, saltLen + 2, encData.length);

        try {
            Cipher cipher = CIPHER_PROVIDER.getCipher();
            cipher.init(Cipher.UNWRAP_MODE, new SecretKeySpec(encryptionKey.getEncoded(), "AES"));
            return pkcs5unpad(cipher.unwrap(data, "RAW", Cipher.SECRET_KEY).getEncoded());
        } catch (GeneralSecurityException e) {
            DEBUG.error("AESWrapEncryption: Failed to decrypt data", e);
            return null;
        }
    }

    /**
     * Formats an encrypted payload into the following form (in Big Endian byte order):
     *
     * <ol>
     *     <li>A 1 byte version field, always the constant 2</li>
     *     <li>A 1-byte salt length field.</li>
     *     <li>An n-byte salt value.</li>
     *     <li>The encrypted ciphertext bytes.</li>
     * </ol>
     *
     * @param key the derived key used to encrypt the data.
     * @param data the encrypted ciphertext.
     * @return the formatted
     */
    @VisibleForTesting
    static byte[] formatEncryptedMessage(final PBEKey key, final byte[] data) {
        final byte[] salt = key.getSalt();
        if (salt.length > 255) {
            throw new IllegalStateException("Salt too large to be encoded");
        }
        final ByteBuffer buffer = ByteBuffer.allocate(2 + salt.length + data.length).order(ByteOrder.BIG_ENDIAN);
        buffer.put(VERSION);
        buffer.put((byte) salt.length);
        buffer.put(salt);
        buffer.put(data);
        return buffer.array();
    }


    /**
     * Applies PKCS#5 padding to the given data. This is identical to
     * <a href="https://tools.ietf.org/html/rfc5652#section-6.3">PKCS#7 padding</a> except specialised to 64-bit (8
     * byte) blocks. The result will be 1-8 bytes larger than the original and always an exact multiple of 8 bytes
     * in size.
     *
     * @param data the data to pad.
     * @return the padded data.
     */
    static byte[] pkcs5pad(byte[] data) {
        int len = data.length;
        int padding = AESWRAP_BLOCK_SIZE - (len % AESWRAP_BLOCK_SIZE);
        byte[] result = new byte[len + padding];
        System.arraycopy(data, 0, result, 0, data.length);
        Arrays.fill(result, len, len + padding, (byte) padding);
        return result;
    }

    /**
     * Checks and removes PKCS#5 padding from the given data, returning the unpadded content. This implementation
     * should be constant time to avoid padding oracle timing attacks. However, this is "belt and braces" as the
     * underlying AESWrap cipher is authenticated so attempts to tamper with the padding should already be rejected
     * before we reach this point.
     *
     * @param data the data to remove padding from.
     * @return the unpadded data.
     * @throws BadPaddingException if the data is padded incorrectly.
     */
    static byte[] pkcs5unpad(byte[] data) throws BadPaddingException {
        Reject.ifNull(data);
        int len = data.length;
        int padding = data[len - 1];
        if (padding <= 0 || padding > AESWRAP_BLOCK_SIZE) {
            throw new BadPaddingException("Invalid padding length: " + padding);
        }
        byte[] expectedPadding = new byte[padding];
        Arrays.fill(expectedPadding, (byte) padding);
        byte[] actualPadding = Arrays.copyOfRange(data, len - padding, len);
        if (!MessageDigest.isEqual(expectedPadding, actualPadding)) {
            throw new BadPaddingException("Invalid padding");
        }
        return Arrays.copyOfRange(data, 0, len - padding);
    }
}
