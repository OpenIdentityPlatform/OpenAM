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
package org.forgerock.openam.keystore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreType;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Holds a Keystore Configuration. This gets serialized to/from JSON as part of boot.json
 * <p>
 * todo: This functionality should be moved into the commons project
 * There is just enough functionality here to support OpenAM boot.json
 *
 *
 * @since 14.0
 */
public class KeyStoreConfig {
    private String keyStorePasswordFile;
    private String keyPasswordFile;
    private String keyStoreType;
    private String keyStoreFile;
    private String providerClass;
    private String providerArg;

    /**
     * Get the path to the file used to unlock the keystore.
     *
     * @return Path to file that holds the password to unlock the keystore
     */
    public String getKeyStorePasswordFile() {
        return keyStorePasswordFile;
    }

    /**
     * Set path the file that contains the password used to unlock the keystore.
     *
     * @param keyStorePasswordFile path to keystore password file
     */
    public void setKeyStorePasswordFile(String keyStorePasswordFile) {
        this.keyStorePasswordFile = keyStorePasswordFile;
    }

    /**
     *
     * Get the path the key password file.
     *
     * @return path to file that contains password to unlock individual key entries
     */
    public String getKeyPasswordFile() {
        return keyPasswordFile;
    }


    /**
     * Set the path the file that holds the per entry password.
     *
     * @param keyPasswordFile set the path to file that holds the per key password
     */
    public void setKeyPasswordFile(String keyPasswordFile) {
        this.keyPasswordFile = keyPasswordFile;
    }

    /**
     * Get the keystore type.
     * @return the keystore type (JKS, JCEKS , etc. )
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }

    /**
     * Set the keystore type (e.g. JKS, JCEKS, ).
     *
     * @param keyStoreType set the keystore type
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    /**
     * Get the path to the keystore.
     * @return The keystore file (example: /tmp/keystore.jceks )
     */
    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    /**
     * Set the keystore file path.
     *
     * @param keyStoreFile the keystore file path
     */
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    /**
     * Get the provider class name string. This is optional for providers.
     * @return The provider class
     */
    public String getProviderClass() {
        return providerClass;
    }

    /**
     * set the provider class name string.
     * @param providerClass - provider class. This is optional for most providers
     *
     */
    public void setProviderClass(String providerClass) {
        this.providerClass = providerClass;
    }

    /**
     * Get the provider class name as a string.
     * @return optional provider argument
     * */
    public String getProviderArg() {
        return providerArg;
    }

    /**
     * Set provider arg.
     * @param providerArg  optional provider arument used to create provider instances
     * */
    public void setProviderArg(String providerArg) {
        this.providerArg = providerArg;
    }


    /**
     * Get the keystore password.
     * @return The keystore password as a char array
     * @throws IOException IF the keystore password file can not be opened
     */
    @JsonIgnore
    public char[] getKeyStorePassword() throws IOException {
        return new String(Files.readAllBytes(Paths.get(getKeyStorePasswordFile())), UTF_8).toCharArray();
    }

    /**
     * Get the key password used to unlock key entries.
     * @return The key password as a char array
     * @throws IOException If the key password file can not be opened
     */

    @JsonIgnore
    public char[] getKeyPassword() throws IOException {
        return new String(Files.readAllBytes(Paths.get(getKeyPasswordFile())), UTF_8).toCharArray();
    }

    /**
     * Initialize and load the keystore described by this configuration
     *
     * todo: This is just enough to get the current keystore.jceks opened
     *  When this is moved to commons the functionality should be expanded to open all keystore types
     *
     *  There are a number of possible exceptions that can be generated - they are consolidated
     *  to a single type and the underlying exception is wrapped.
     *
     * @throws KeyStoreException if the keystore can not be opened or initialized.
     * @return the opened KeyStore
     */
    public KeyStore loadKeyStore() throws KeyStoreException {
        KeyStore ks = null;
        try {
            KeyStoreBuilder builder = new KeyStoreBuilder();
            ks = builder.withKeyStoreFile(getKeyStoreFile())
                    .withKeyStoreType(KeyStoreType.JCEKS)
                    .withPassword(getKeyPassword())
                    .build();
        } catch (IOException e) {
            throw new KeyStoreException("Could not initialize the Keystore", e);
        }
        return ks;
    }
}
