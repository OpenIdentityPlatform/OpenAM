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

package org.forgerock.openam.sm;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.DefaultValues;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Set;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.forgerock.guice.core.InjectorHolder;

/**
 * SMS default value generator that generates unique and secure HMAC shared secrets.
 *
 * @since 13.0.0
 */
public final class HmacSharedSecretDefaultValues extends DefaultValues {
    private static final String KEYGEN_ALGORITHM = "HmacSHA256";
    private static final int KEYSIZE = 256;
    private static final SecureRandom RANDOM = InjectorHolder.getInstance(SecureRandom.class);

    private final KeyGenerator keyGenerator;

    /**
     * Constructs a HMAC-SHA-256 key generator using a SHA1PRNG SecureRandom instance.
     */
    public HmacSharedSecretDefaultValues() {
        try {
            this.keyGenerator = KeyGenerator.getInstance(KEYGEN_ALGORITHM);
            keyGenerator.init(KEYSIZE, RANDOM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set getDefaultValues() {
        final SecretKey key = keyGenerator.generateKey();
        return Collections.singleton(Base64.encode(key.getEncoded()));
    }

}
