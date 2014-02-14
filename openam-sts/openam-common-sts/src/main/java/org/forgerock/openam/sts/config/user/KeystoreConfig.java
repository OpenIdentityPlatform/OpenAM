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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.config.user;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.util.Reject;

import java.util.Arrays;

/**
 * The classes in this package define the objects which must be populated in order to create a fully-configured
 * STS instance. UI state soliciting user input in the context of creating STS instances will emit instances of classes
 * in this package upon UI-harvest.
 *
 * This class defines the keystore state necessary to sign and encrypt tokens.
 *
 */
public class KeystoreConfig {
    public static class KeystoreConfigBuilder {
        private String keystoreFileName;
        private byte[] keystorePassword;
        private String signatureKeyAlias;
        private String encryptionKeyAlias;
        private byte[] signatureKeyPassword;
        private byte[] encryptionKeyPassword;

        public KeystoreConfigBuilder fileName(String keystoreFileName) {
            this.keystoreFileName = keystoreFileName;
            return this;
        }

        public KeystoreConfigBuilder password(byte[] keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public KeystoreConfigBuilder signatureKeyAlias(String signatureKeyAlias) {
            this.signatureKeyAlias = signatureKeyAlias;
            return this;
        }

        public KeystoreConfigBuilder encryptionKeyAlias(String encryptionKeyAlias) {
            this.encryptionKeyAlias = encryptionKeyAlias;
            return this;
        }

        public KeystoreConfigBuilder signatureKeyPassword(byte[] signatureKeyPassword) {
            this.signatureKeyPassword = signatureKeyPassword;
            return this;
        }

        public KeystoreConfigBuilder encryptionKeyPassword(byte[] encryptionKeyPassword) {
            this.encryptionKeyPassword = encryptionKeyPassword;
            return this;
        }

        public KeystoreConfig build() {
            return new KeystoreConfig(this);
        }
    }

    private final String keystoreFileName;
    private final byte[] keystorePassword;
    private final String signatureKeyAlias;
    private final String encryptionKeyAlias;
    /*
    The alias passwords corresponding to the signatureKeyAlias and the encryptionKeyAlias are obtained from
    a callback handler. The callback handler will use an instance of this class to return the appropriate values.
     */
    private final byte[] signatureKeyPassword;
    private final byte[] encryptionKeyPassword;

    private KeystoreConfig(KeystoreConfigBuilder builder) {
        this.keystoreFileName = builder.keystoreFileName;
        this.keystorePassword = builder.keystorePassword;
        this.signatureKeyAlias = builder.signatureKeyAlias;
        this.encryptionKeyAlias = builder.encryptionKeyAlias;
        this.signatureKeyPassword = builder.signatureKeyPassword;
        this.encryptionKeyPassword = builder.encryptionKeyPassword;
        Reject.ifNull(keystoreFileName, "Keystore file name cannot be null");
        Reject.ifNull(keystorePassword, "Keystore password cannot be null");
        Reject.ifNull(signatureKeyAlias, "Signature key alias cannot be null");
        Reject.ifNull(encryptionKeyAlias, "Encryption key alias cannot be null");
        Reject.ifNull(signatureKeyPassword, "Signature key password cannot be null");
        Reject.ifNull(encryptionKeyPassword, "Encryption key password cannot be null");
        /*
        TODO: it may be that a deployed STS might not need to sign keys. If this is the case(currently TBD),
        the Reject can be removed, and equals, toString, and hashCode updated to handle the null fields.
         */
    }

    public static KeystoreConfigBuilder builder() {
        return new KeystoreConfigBuilder();
    }

    public String getKeystoreFileName() {
        return keystoreFileName;
    }

    public byte[] getKeystorePassword() {
        return keystorePassword;
    }

    public String getSignatureKeyAlias() {
        return signatureKeyAlias;
    }

    public String getEncryptionKeyAlias() {
        return encryptionKeyAlias;
    }

    public byte[] getSignatureKeyPassword() {
        return signatureKeyPassword;
    }

    public byte[] getEncryptionKeyPassword() {
        return encryptionKeyPassword;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KeystoreConfig instance:").append('\n');
        sb.append('\t').append("keyStoreFileName: ").append(keystoreFileName);
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof KeystoreConfig) {
            KeystoreConfig otherConfig = (KeystoreConfig)other;
            return keystoreFileName.equals(otherConfig.getKeystoreFileName()) &&
                    Arrays.equals(keystorePassword, otherConfig.getKeystorePassword()) &&
                    signatureKeyAlias.equals(otherConfig.getSignatureKeyAlias()) &&
                    encryptionKeyAlias.equals(otherConfig.getEncryptionKeyAlias()) &&
                    Arrays.equals(signatureKeyPassword, otherConfig.getSignatureKeyPassword()) &&
                    Arrays.equals(encryptionKeyPassword, otherConfig.getEncryptionKeyPassword());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (keystoreFileName + new String(keystorePassword) + signatureKeyAlias + encryptionKeyAlias +
                new String(signatureKeyPassword) + new String(encryptionKeyPassword)).hashCode();
    }
}
