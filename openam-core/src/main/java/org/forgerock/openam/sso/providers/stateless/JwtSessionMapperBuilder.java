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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.charset.Charset;
import java.security.KeyPair;

/**
 * Responsible for creating instances of {@link JwtSessionMapper}.
 *
 * Encryption and signing are optional and can be combined. However, only one means of signing can be applied.
 *
 * @since 13.0.0
 */
@NotThreadSafe
class JwtSessionMapperBuilder {

    private JwsAlgorithm jwsAlgorithm = JwsAlgorithm.NONE;
    private SigningHandler signingHandler = new SigningManager().newNopSigningHandler();
    private SigningHandler verificationHandler = new SigningManager().newNopSigningHandler();
    private KeyPair encryptionKeyPair = null;

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can sign and verify
     * the JWT using RSA using SHA-256 hash algorithm.
     *
     * @param signingKeyPair Non-null, public-private key-pair to use to sign and verify the JWT.
     *
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see org.forgerock.json.jose.jws.JwsAlgorithm
     */
    public JwtSessionMapperBuilder signedUsingRS256(@Nonnull final KeyPair signingKeyPair) {

        Reject.ifNull(signingKeyPair, "signingKeyPair must not be null.");

        jwsAlgorithm = JwsAlgorithm.RS256;
        signingHandler = new SigningManager().newRsaSigningHandler(signingKeyPair.getPrivate());
        verificationHandler = new SigningManager().newRsaSigningHandler(signingKeyPair.getPublic());
        return this;
    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can sign and verify
     * the JWT using HMAC with the SHA-256 hash algorithm.
     *
     * @param sharedSecret Non-null, shared secret to use to sign and verify the JWT.
     *
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see org.forgerock.json.jose.jws.JwsAlgorithm
     */
    public JwtSessionMapperBuilder signedUsingHS256(@Nonnull final String sharedSecret) {

        signedUsingHSxxx(JwsAlgorithm.HS256, sharedSecret);
        return this;
    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can sign and verify
     * the JWT using using SHA-384 hash algorithm.
     *
     * @param sharedSecret Non-null, shared secret to use to sign and verify the JWT.
     *
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see org.forgerock.json.jose.jws.JwsAlgorithm
     */
    public JwtSessionMapperBuilder signedUsingHS384(@Nonnull final String sharedSecret) {

        signedUsingHSxxx(JwsAlgorithm.HS384, sharedSecret);
        return this;
    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can sign and verify
     * the JWT using using SHA-512 hash algorithm.
     *
     * @param sharedSecret Non-null, shared secret to use to sign and verify the JWT.
     *
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see org.forgerock.json.jose.jws.JwsAlgorithm
     */
    public JwtSessionMapperBuilder signedUsingHS512(@Nonnull final String sharedSecret) {

        signedUsingHSxxx(JwsAlgorithm.HS512, sharedSecret);
        return this;
    }

    private void signedUsingHSxxx(@Nonnull final JwsAlgorithm jwsAlgorithm, @Nonnull final String sharedSecret) {

        Reject.ifNull(jwsAlgorithm, "jwsAlgorithm must not be null.");
        Reject.ifTrue(StringUtils.isEmpty(sharedSecret), "sharedSecret must not be null or empty string.");

        final byte[] sharedSecretBytes = sharedSecret.getBytes(Charset.forName("UTF-8"));

        this.jwsAlgorithm = jwsAlgorithm;
        this.signingHandler = new SigningManager().newHmacSigningHandler(sharedSecretBytes);
        this.verificationHandler = new SigningManager().newHmacSigningHandler(sharedSecretBytes);

    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can encrypt and decrypt
     * the JWT.
     *
     * @param encryptionKeyPair Non-null, public-private key-pair to use to encrypt and decrypt the JWT.
     *
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see org.forgerock.json.jose.jwe.JweAlgorithm
     * @see org.forgerock.json.jose.jwe.EncryptionMethod
     */
    public JwtSessionMapperBuilder encryptedUsingKeyPair(@Nonnull final KeyPair encryptionKeyPair) {

        Reject.ifNull(encryptionKeyPair, "encryptionKeyPair must not be null.");

        this.encryptionKeyPair = encryptionKeyPair;
        return this;

    }

    /**
     * @return JwtSessionMapper configured to perform signing and encryption as specified.
     */
    public JwtSessionMapper build() {

        return new JwtSessionMapper(jwsAlgorithm, signingHandler, verificationHandler, encryptionKeyPair);
    }

}
