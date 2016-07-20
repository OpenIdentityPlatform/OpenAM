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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import static org.forgerock.util.Reject.checkNotNull;

import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweAlgorithmType;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

import com.sun.identity.configuration.SystemProperties;
import com.sun.identity.shared.configuration.ISystemProperties;

/**
 * Responsible for creating instances of {@link JwtSessionMapper}.
 *
 * Encryption and signing are optional and can be combined. However, only one means of signing can be applied.
 *
 * @since 13.0.0
 */
@NotThreadSafe
class JwtSessionMapperBuilder {
    private static final String ENCRYPTION_METHOD =
            "org.forgerock.openam.session.stateless.encryption.method";
    private static final String DEFAULT_ENCRYPTION_METHOD = "A128CBC-HS256";
    private static final String RSA_PADDING_METHOD = "org.forgerock.openam.session.stateless.rsa.padding";
    private static final String DEFAULT_RSA_PADDING_METHOD = "RSA-OAEP-256";

    private final SigningManager signingManager;
    private final ISystemProperties systemProperties;

    JwsAlgorithm jwsAlgorithm = JwsAlgorithm.NONE;
    SigningHandler signingHandler = new SigningManager().newNopSigningHandler();
    SigningHandler verificationHandler = new SigningManager().newNopSigningHandler();

    JweAlgorithm jweAlgorithm = null;
    Key encryptionKey = null;
    Key decryptionKey = null;
    EncryptionMethod encryptionMethod = null;

    CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.NONE;

    @VisibleForTesting
    JwtSessionMapperBuilder(final SigningManager signingManager, final ISystemProperties systemProperties) {
        this.signingManager = signingManager;
        this.systemProperties = systemProperties;
    }

    /**
     * Creates a blank session mapping builder with no signing or encryption modes configured.
     */
    JwtSessionMapperBuilder() {
        this(new SigningManager(), new SystemProperties());
    }

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
    JwtSessionMapperBuilder signedUsingRS256(@Nonnull final KeyPair signingKeyPair) {

        Reject.ifNull(signingKeyPair, "signingKeyPair must not be null.");

        jwsAlgorithm = JwsAlgorithm.RS256;
        signingHandler = signingManager.newRsaSigningHandler(signingKeyPair.getPrivate());
        verificationHandler = signingManager.newRsaSigningHandler(signingKeyPair.getPublic());
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
    JwtSessionMapperBuilder signedUsingHS256(@Nonnull final String sharedSecret) {

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
    JwtSessionMapperBuilder signedUsingHS384(@Nonnull final String sharedSecret) {

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
    JwtSessionMapperBuilder signedUsingHS512(@Nonnull final String sharedSecret) {

        signedUsingHSxxx(JwsAlgorithm.HS512, sharedSecret);
        return this;
    }

    private void signedUsingHSxxx(@Nonnull final JwsAlgorithm jwsAlgorithm, @Nonnull final String sharedSecret) {

        Reject.ifNull(jwsAlgorithm, "jwsAlgorithm must not be null.");
        Reject.ifTrue(StringUtils.isEmpty(sharedSecret), "sharedSecret must not be null or empty string.");

        final byte[] sharedSecretBytes = sharedSecret.getBytes(Charset.forName("UTF-8"));

        this.jwsAlgorithm = jwsAlgorithm;
        this.signingHandler = signingManager.newHmacSigningHandler(sharedSecretBytes);
        this.verificationHandler = signingManager.newHmacSigningHandler(sharedSecretBytes);

    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can sign and verify the JWT using
     * the ES256 signature algorithm (ECDSA using P-256 curve over SHA-256 hashes).
     *
     * @param signingKeyPair the signing key pair, not null.
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see JwsAlgorithm
     */
    JwtSessionMapperBuilder signedUsingES256(@Nonnull final KeyPair signingKeyPair) {
        signedUsingESxxx(JwsAlgorithm.ES256, signingKeyPair);
        return this;
    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can sign and verify the JWT using
     * the ES384 signature algorithm (ECDSA using P-384 curve over SHA-384 hashes).
     *
     * @param signingKeyPair the signing key pair, not null.
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see JwsAlgorithm
     */
    JwtSessionMapperBuilder signedUsingES384(@Nonnull final KeyPair signingKeyPair) {
        signedUsingESxxx(JwsAlgorithm.ES384, signingKeyPair);
        return this;
    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can sign and verify the JWT using
     * the ES512 signature algorithm (ECDSA using P-521 (not a typo!) curve over SHA-512 hashes).
     *
     * @param signingKeyPair the signing key pair, not null.
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see JwsAlgorithm
     */
    JwtSessionMapperBuilder signedUsingES512(@Nonnull final KeyPair signingKeyPair) {
        signedUsingESxxx(JwsAlgorithm.ES512, signingKeyPair);
        return this;
    }

    private void signedUsingESxxx(@Nonnull final JwsAlgorithm jwsAlgorithm, @Nonnull final KeyPair signingKeyPair) {
        Reject.ifNull(signingKeyPair, "signingKeyPair must not be null.");
        Reject.ifFalse(signingKeyPair.getPrivate() instanceof ECPrivateKey, "private key is not suitable for " +
                jwsAlgorithm);
        Reject.ifFalse(signingKeyPair.getPublic() instanceof ECPublicKey, "public key is not suitable for " +
                jwsAlgorithm);

        this.jwsAlgorithm = jwsAlgorithm;
        this.signingHandler = signingManager.newEcdsaSigningHandler((ECPrivateKey) signingKeyPair.getPrivate());
        this.verificationHandler = signingManager.newEcdsaVerificationHandler((ECPublicKey) signingKeyPair.getPublic());
    }

    /**
     * Instructs the builder to create instances of {@link JwtSessionMapper} that can encrypt and decrypt
     * the JWT using RSA encryption. The system property {@literal org.forgerock.openam.session.stateless.rsa .padding}
     * can be used to control the RSA padding method (defaults to {@literal RSA-OAEP-256}). The system property
     * {@literal org.forgerock.openam.session.stateless.encryption.method} can be used to control the underlying
     * content encryption method (defaults to {@literal A128CBC-HS256}).
     *
     * @param encryptionKeyPair Non-null, public-private key-pair to use to encrypt and decrypt the JWT.
     *
     * @return this {@link JwtSessionMapperBuilder}
     *
     * @see JweAlgorithm#RSA_OAEP
     * @see JweAlgorithm#RSA_OAEP_256
     * @see JweAlgorithm#RSAES_PKCS1_V1_5
     * @see org.forgerock.json.jose.jwe.EncryptionMethod
     */
    JwtSessionMapperBuilder encryptedUsingKeyPair(@Nonnull final KeyPair encryptionKeyPair) {

        Reject.ifNull(encryptionKeyPair, "encryptionKeyPair must not be null.");

        this.encryptionKey = encryptionKeyPair.getPublic();
        this.decryptionKey = encryptionKeyPair.getPrivate();
        this.jweAlgorithm = JweAlgorithm.parseAlgorithm(
                systemProperties.getOrDefault(RSA_PADDING_METHOD, DEFAULT_RSA_PADDING_METHOD));

        return this;
    }

    /**
     * Instructs the builder to constuct {@link JwtSessionMapper} instances that encrypt the JWT contents using <a
     * href="https://tools.ietf.org/html/rfc3394">AES KeyWrap</a>. A unique key is generated for each JWT and then
     * wrapped using the provided symmetric key. The strength is determined by the size of the given key: 128-bit key
     * corresponds to {@link JweAlgorithm#A128KW} etc. Note that key sizes greater than 128 bits will require
     * installation of the JCE Unlimited Strength policy files in the Java Runtime Environment. The system property
     * {@literal org.forgerock.openam.session.stateless.encryption.method} can be used to control the underlying
     * content encryption method (defaults to {@literal A128CBC-HS256}).
     *
     * @param symmetricEncryptionKey the symmetric key. Must be 128, 192 or 256 bits in size.
     * @return this {@link JwtSessionMapperBuilder}.
     *
     * @see JweAlgorithm#A128KW
     * @see JweAlgorithm#A192KW
     * @see JweAlgorithm#A256KW
     * @see EncryptionMethod
     */
    JwtSessionMapperBuilder encryptedUsingKeyWrap(@Nonnull final Key symmetricEncryptionKey) {
        Reject.ifNull(symmetricEncryptionKey, "symmetricEncryptionKey must not be null.");

        this.encryptionKey = symmetricEncryptionKey;
        this.decryptionKey = symmetricEncryptionKey;
        switch (symmetricEncryptionKey.getEncoded().length) {
            case 16:
                this.jweAlgorithm = JweAlgorithm.A128KW;
                break;
            case 24:
                this.jweAlgorithm = JweAlgorithm.A192KW;
                break;
            case 32:
                this.jweAlgorithm = JweAlgorithm.A256KW;
                break;
            default:
                throw new IllegalArgumentException("Invalid key size for AES KeyWrap: must be 128, 192 or 256 bits");
        }

        return this;
    }

    /**
     * Instructs the builder to construct {@link JwtSessionMapper} instances that encrypt the JWT contents directly
     * using the given AES key. The system property
     * {@literal org.forgerock.openam.session.stateless.encryption.method} can be used to control the underlying
     * content encryption method (defaults to {@literal A128CBC-HS256}).
     *
     * @param symmetricEncryptionKey the AES encryption key to use. Must be either 128, 192 or 256 bits.
     * @return this {@link JwtSessionMapperBuilder}.
     *
     * @see JweAlgorithm#DIRECT
     * @see EncryptionMethod
     */
    JwtSessionMapperBuilder encryptedUsingDirectKey(@Nonnull final Key symmetricEncryptionKey) {
        Reject.ifNull(symmetricEncryptionKey, "symmetricEncryptionKey must not be null.");

        this.encryptionKey = symmetricEncryptionKey;
        this.decryptionKey = symmetricEncryptionKey;
        this.jweAlgorithm = JweAlgorithm.DIRECT;

        return this;
    }

    /**
     * Indicates that the content should be compressed before encryption using the given algorithm.
     *
     * @param compressionAlgorithm the compression algorithm to use.
     * @return this builder object.
     */
    JwtSessionMapperBuilder compressedUsing(CompressionAlgorithm compressionAlgorithm) {
        this.compressionAlgorithm = checkNotNull(compressionAlgorithm);
        return this;
    }

    /**
     * @return JwtSessionMapper configured to perform signing and encryption as specified.
     */
    JwtSessionMapper build() {

        Reject.ifNull(jwsAlgorithm, "jwsAlgorithm must not be null.");
        Reject.ifNull(signingHandler, "signingHandler must not be null.");
        Reject.ifNull(verificationHandler, "verificationHandler must not be null.");
        if (jweAlgorithm != null) {
            encryptionMethod = EncryptionMethod.parseMethod(
                    systemProperties.getOrDefault(ENCRYPTION_METHOD, DEFAULT_ENCRYPTION_METHOD));

            Reject.ifNull(encryptionMethod, "Encryption enabled but no EncryptionMethod specified");
            Reject.ifNull(encryptionKey, "Encryption enabled but no encryption key specified");
            Reject.ifNull(decryptionKey, "Encryption enabled but no decryption key specified");

            Reject.ifTrue(jweAlgorithm.getAlgorithmType() == JweAlgorithmType.RSA && jwsAlgorithm == JwsAlgorithm.NONE,
                    "RSA encryption should not be used without a signature");
        } else {
            Reject.ifTrue(jwsAlgorithm == JwsAlgorithm.NONE, "No encryption or signature scheme specified!");
        }

        return new JwtSessionMapper(this);
    }

    @VisibleForTesting
    JwsAlgorithm getJwsAlgorithm() {
        return jwsAlgorithm;
    }
}
