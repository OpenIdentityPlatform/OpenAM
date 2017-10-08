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

package org.forgerock.openam.selfservice;

import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;

import java.util.Objects;

/**
 * JWT token config that makes use of key store.
 *
 * @since 13.5.0
 */
public final class KeyStoreJwtTokenConfig implements SnapshotTokenConfig {

    /**
     * Snapshot token config type.
     */
    public static final String TYPE = "KEY_STORE_JWT";

    private String encryptionKeyPairAlias;
    private String signingSecretKeyAlias;
    private long tokenLifeTimeInSeconds;

    /**
     * Get the encryption key pair alias.
     *
     * @return the encryption key pair alias
     */
    public String getEncryptionKeyPairAlias() {
        return encryptionKeyPairAlias;
    }

    /**
     * Set the encryption key pair alias.
     *
     * @param encryptionKeyPairAlias
     *         the encryption key pair alias
     *
     * @return this instance
     */
    public KeyStoreJwtTokenConfig withEncryptionKeyPairAlias(String encryptionKeyPairAlias) {
        this.encryptionKeyPairAlias = encryptionKeyPairAlias;
        return this;
    }

    /**
     * Get the signing secret key alias.
     *
     * @return the signing secret key alias
     */
    public String getSigningSecretKeyAlias() {
        return signingSecretKeyAlias;
    }

    /**
     * Set the signing secret key alias.
     *
     * @param signingSecretKeyAlias
     *         the signing secret key alias
     *
     * @return this instance
     */
    public KeyStoreJwtTokenConfig withSigningSecretKeyAlias(String signingSecretKeyAlias) {
        this.signingSecretKeyAlias = signingSecretKeyAlias;
        return this;
    }

    /**
     * Get the token lift time in seconds.
     *
     * @return the token life time
     */
    public long getTokenLifeTimeInSeconds() {
        return tokenLifeTimeInSeconds;
    }

    /**
     * Set the token life time in seconds.
     *
     * @param tokenLifeTimeInSeconds
     *         the token life time
     *
     * @return this instance
     */
    public KeyStoreJwtTokenConfig withTokenLifeTimeInSeconds(long tokenLifeTimeInSeconds) {
        this.tokenLifeTimeInSeconds = tokenLifeTimeInSeconds;
        return this;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(encryptionKeyPairAlias, signingSecretKeyAlias, tokenLifeTimeInSeconds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof KeyStoreJwtTokenConfig)) {
            return false;
        }

        KeyStoreJwtTokenConfig other = (KeyStoreJwtTokenConfig) obj;
        return Objects.equals(encryptionKeyPairAlias, other.encryptionKeyPairAlias)
                && Objects.equals(signingSecretKeyAlias, other.signingSecretKeyAlias)
                && Objects.equals(tokenLifeTimeInSeconds, other.tokenLifeTimeInSeconds);
    }

}
