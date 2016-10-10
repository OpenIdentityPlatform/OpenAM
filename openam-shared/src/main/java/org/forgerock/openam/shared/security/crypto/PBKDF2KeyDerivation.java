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

import static org.forgerock.util.Reject.rejectStateIfTrue;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;

import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.services.util.ConfigurableKey;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * A {@link ConfigurableKey} implementation that derives a secret key from the input password using the PBKDF2 key
 * deriviation algorithm. The following system properties can be used to configure how the key is derived:
 * <ul>
 *     <li>{@code org.forgerock.openam.encryption.key.digest} - the message digest (hash) algorithm to use with
 *     PBKDF2. Defaults to "SHA1".</li>
 *     <li>{@code org.forgerock.openam.encryption.key.iterations} - the number of iterations of PBKDF2 to apply
 *     when generating keys. Must be at least 10,000, but should typically be as large as you can compute in a
 *     reasonable time.</li>
 * </ul>
 * The <a href="https://pages.nist.gov/800-63-3/sp800-63b.html#memorized-secret-verifiers">NIST guidelines</a>
 * (section 5.1.1.2) recommend at least 10,000 iterations of PBKDF2 and a salt of at least 32 bits. The PBKDF2 spec
 * itself recommends a salt of at least 64 bits. We use a salt of 128 bits as per <a
 * href="http://security.stackexchange.com/a/27971">this advice</a>. The default hash algorithm we use is SHA-1, as
 * this is available on all supported platforms. If you are using Java 8 it is better to use a hash like SHA-512 as
 * that greatly increases the memory requirements and will slow down GPU-based brute-force attacks.
 */
public class PBKDF2KeyDerivation implements ConfigurableKey {
    private static final int SALT_BYTES = 16;
    private static final String DIGEST_ALGORITHM_PROPERTY = "org.forgerock.openam.encryption.key.digest";
    private static final String ITERATIONS_PROPERTY = "org.forgerock.openam.encryption.key.iterations";

    private final int iterations;
    private final SecretKeyFactory secretKeyFactory;
    private final SecureRandom secureRandom = new SecureRandom();

    private volatile char[] password;

    @VisibleForTesting
    PBKDF2KeyDerivation(final String messageDigest, final int iterations) {
        rejectStateIfTrue(iterations < 10_000, "Should use at least 10,000 iterations");
        this.iterations = iterations;
        try {
            this.secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmac" + messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid message digest: " + messageDigest, e);
        }
    }

    /**
     * Default private constructor.
     *
     * @throws IllegalStateException if the configured message digest or iteration count is invalid.
     */
    public PBKDF2KeyDerivation() {
        this(SystemPropertiesManager.get(DIGEST_ALGORITHM_PROPERTY, "SHA1"),
                SystemPropertiesManager.getAsInt(ITERATIONS_PROPERTY, 10_000));
    }

    @Override
    public void setPassword(final String password) throws Exception {
        Reject.ifTrue(StringUtils.isBlank(password));
        char[] oldPassword = this.password;
        if (oldPassword != null) {
            Arrays.fill(oldPassword, ' ');
        }
        this.password = password.toCharArray();
    }

    /**
     * Derives a secret key of the requested size using a fresh random salt and the configured password and iteration
     * count. The parameters can be read from the generated key.
     *
     * @param keySize the size of the key (in bits) to generate.
     * @return the generated key.
     */
    public PBEKey deriveSecretKey(int keySize) {
        final byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return deriveSecretKey(keySize, salt);
    }

    /**
     * Derives a secret key of the requested using using the given salt and the configured password and iteration count.
     *
     * @param keySize the size of the key (in bits) to generate.
     * @param salt the salt to use to generate the key.
     * @return the derived key.
     */
    public PBEKey deriveSecretKey(int keySize, byte[] salt) {
        Reject.ifNull(salt);
        Reject.ifTrue(salt.length < 16, "Salt should be at least 16 bytes");
        rejectStateIfTrue(password == null, "No password configured");

        try {
            return (PBEKey) secretKeyFactory.generateSecret(new PBEKeySpec(password, salt, iterations, keySize));
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid key size", e);
        }
    }

    /**
     * Clears the configured password for this key derivation function.
     */
    public void clear() {
        Arrays.fill(this.password, ' ');
        this.password = null;
    }
}
