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

package org.forgerock.openam.shared.security.crypto;

import static org.forgerock.util.Reject.checkNotNull;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.Reject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Arrays;

/**
 * Builder class for loading keystores.
 *
 * @since 13.0.0
 */
public final class KeyStoreBuilder {
    private static final Debug DEBUG = Debug.getInstance("amSecurity");

    private KeyStoreType type = KeyStoreType.JKS;
    private InputStream inputStream = null;
    private Provider provider = null;
    private char[] password = null;

    /**
     * Specifies the input stream to load the keystore from. Defaults to {@code null} to create a fresh keystore.
     *
     * @param inputStream the input stream to load the keystore from.
     * @return the same builder instance.
     */
    public KeyStoreBuilder withInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /**
     * Specifies the file to load the keystore from.
     *
     * @param keyStoreFile the keystore file to load.
     * @return the same builder instance.
     * @throws FileNotFoundException if the file does not exist, is not a file, or cannot be read.
     */
    public KeyStoreBuilder withKeyStoreFile(final File keyStoreFile) throws FileNotFoundException {
        return withInputStream(new FileInputStream(keyStoreFile));
    }

    /**
     * Specifies the type of keystore to load. Defaults to JKS.
     *
     * @param type the type of keystore to load. May not be null.
     * @return the same builder instance.
     */
    public KeyStoreBuilder withKeyStoreType(final KeyStoreType type) {
        this.type = checkNotNull(type);
        return this;
    }

    /**
     * Specifies the password to unlock the keystore. Defaults to no password. The password will be cleared after the
     * keystore has been loaded.
     *
     * @param password the password to unlock the keystore.
     * @return the same builder instance.
     */
    public KeyStoreBuilder withPassword(final char[] password) {
        this.password = password;
        return this;
    }

    /**
     * Specifies the password to unlock the keystore.
     *
     * @param password the password to use. May not be null.
     * @return the same builder instance.
     * @see #withPassword(char[])
     */
    public KeyStoreBuilder withPassword(final String password) {
        return withPassword(password.toCharArray());
    }

    /**
     * Specifies the security provider to use for the keystore.
     *
     * @param provider the security provider. May not be null.
     * @return the same builder instance.
     */
    public KeyStoreBuilder withProvider(final Provider provider) {
        Reject.ifNull(provider);
        this.provider = provider;
        return this;
    }

    /**
     * Specifies the security provider to use for the keystore.
     *
     * @param providerName the name of the provider to use.
     * @return the same builder instance.
     * @throws IllegalArgumentException if no such provider exists.
     */
    public KeyStoreBuilder withProvider(final String providerName) {
        Provider provider = Security.getProvider(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("No such provider: " + providerName);
        }
        return withProvider(provider);
    }

    /**
     * Builds and loads the keystore using the provided parameters. If a password was provided, then it is blanked
     * after the keystore has been loaded.
     *
     * @return the configured keystore.
     */
    public KeyStore build() {
        try {
            KeyStore keyStore = provider != null ? KeyStore.getInstance(type.toString(), provider)
                                                 : KeyStore.getInstance(type.toString());
            keyStore.load(inputStream, password);
            return keyStore;
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            DEBUG.error("KeyStoreBuilder.build(): Error loading keystore", e);
            throw new IllegalStateException("Unable to load keystore");
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
