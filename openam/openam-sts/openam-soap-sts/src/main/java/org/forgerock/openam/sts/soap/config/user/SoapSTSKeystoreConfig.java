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

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.util.Reject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openam.shared.sts.SharedSTSConstants.*;

/**
 * The classes in this package define the objects which must be populated in order to create a fully-configured
 * STS instance. UI state soliciting user input in the context of creating STS instances will emit instances of classes
 * in this package upon UI-harvest.
 *
 * This class defines the keystore state necessary for a soap-sts instance to sign and encrypt messages to/from the
 * sts, as specified by the ws-security-policy bindings. Note that this keystore state does not pertain to the keystore
 * state related to generating SAML2 assertions, which is defined in SAML2Config.
 *
 */
public class SoapSTSKeystoreConfig {
    public static class SoapSTSKeystoreConfigBuilder {
        private String keystoreFileName;
        private byte[] keystorePassword;
        private String signatureKeyAlias;
        private String encryptionKeyAlias;
        private byte[] signatureKeyPassword;
        private byte[] encryptionKeyPassword;

        public SoapSTSKeystoreConfigBuilder fileName(String keystoreFileName) {
            this.keystoreFileName = keystoreFileName;
            return this;
        }

        public SoapSTSKeystoreConfigBuilder password(byte[] keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public SoapSTSKeystoreConfigBuilder signatureKeyAlias(String signatureKeyAlias) {
            this.signatureKeyAlias = signatureKeyAlias;
            return this;
        }

        public SoapSTSKeystoreConfigBuilder encryptionKeyAlias(String encryptionKeyAlias) {
            this.encryptionKeyAlias = encryptionKeyAlias;
            return this;
        }

        public SoapSTSKeystoreConfigBuilder signatureKeyPassword(byte[] signatureKeyPassword) {
            this.signatureKeyPassword = signatureKeyPassword;
            return this;
        }

        public SoapSTSKeystoreConfigBuilder encryptionKeyPassword(byte[] encryptionKeyPassword) {
            this.encryptionKeyPassword = encryptionKeyPassword;
            return this;
        }

        public SoapSTSKeystoreConfig build() {
            return new SoapSTSKeystoreConfig(this);
        }
    }

    public static final String KEYSTORE_FILE_NAME = "keystore-filename";
    public static final String KEYSTORE_PASSWORD = "keystore-password";
    public static final String SIGNATURE_KEY_ALIAS = "keystore-signature-key-alias";
    public static final String ENCRYPTION_KEY_ALIAS = "keystore-encryption-key-alias";
    public static final String SIGNATURE_KEY_PASSWORD = "keystore-signature-key-password";
    public static final String ENCRYPTION_KEY_PASSWORD = "keystore-encryption-key-password";

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

    private SoapSTSKeystoreConfig(SoapSTSKeystoreConfigBuilder builder) {
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
    }

    public static SoapSTSKeystoreConfigBuilder builder() {
        return new SoapSTSKeystoreConfigBuilder();
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
        if (other instanceof SoapSTSKeystoreConfig) {
            SoapSTSKeystoreConfig otherConfig = (SoapSTSKeystoreConfig)other;
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

    public JsonValue toJson() {
        try {
            return json(object(field(KEYSTORE_FILE_NAME, keystoreFileName),
                    field(KEYSTORE_PASSWORD, new String(keystorePassword, AMSTSConstants.UTF_8_CHARSET_ID)),
                    field(SIGNATURE_KEY_ALIAS, signatureKeyAlias), field(ENCRYPTION_KEY_ALIAS, encryptionKeyAlias),
                    field(SIGNATURE_KEY_PASSWORD, new String(signatureKeyPassword, AMSTSConstants.UTF_8_CHARSET_ID)),
                    field(ENCRYPTION_KEY_PASSWORD, new String(encryptionKeyPassword, AMSTSConstants.UTF_8_CHARSET_ID))));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding when marshalling from byte[] to String: " + e, e);
        }
    }

    public static SoapSTSKeystoreConfig fromJson(JsonValue json) throws IllegalStateException {
        try {
            return SoapSTSKeystoreConfig.builder()
                    .fileName(json.get(KEYSTORE_FILE_NAME).asString())
                    .password(json.get(KEYSTORE_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .signatureKeyAlias(json.get(SIGNATURE_KEY_ALIAS).asString())
                    .encryptionKeyAlias(json.get(ENCRYPTION_KEY_ALIAS).asString())
                    .signatureKeyPassword(json.get(SIGNATURE_KEY_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .encryptionKeyPassword(json.get(ENCRYPTION_KEY_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding when marshalling from String to to byte[]: " + e, e);
        }
    }

    public Map<String, Set<String>> marshalToAttributeMap() {
        return MapMarshallUtils.toSmsMap(toJson().asMap());
    }

    public static SoapSTSKeystoreConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        return fromJson(new JsonValue(MapMarshallUtils.toJsonValueMap(attributeMap)));
    }
}