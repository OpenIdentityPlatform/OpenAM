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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.config.user;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.util.Reject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;


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
    /*
    A set of constants used to marshal to an from json and to and from the Map<String, Set<String>> required by the
    SMS. Note that these values must match those defined in soapSTS.xml.
     */
    static final String KEYSTORE_FILENAME = "soap-keystore-filename";
    static final String KEYSTORE_PASSWORD = "soap-keystore-password";
    static final String SIGNATURE_KEY_ALIAS = "soap-signature-key-alias";
    static final String ENCRYPTION_KEY_ALIAS = "soap-encryption-key-alias";
    static final String SIGNATURE_KEY_PASSWORD = "soap-signature-key-password";
    static final String ENCRYPTION_KEY_PASSWORD = "soap-encryption-key-password";

    public static class SoapSTSKeystoreConfigBuilder {
        private String keystoreFileName;
        private byte[] keystorePassword;
        private String signatureKeyAlias;
        private String encryptionKeyAlias;
        private byte[] signatureKeyPassword;
        private byte[] encryptionKeyPassword;

        public SoapSTSKeystoreConfigBuilder keystoreFileName(String keystoreFileName) {
            this.keystoreFileName = keystoreFileName;
            return this;
        }

        public SoapSTSKeystoreConfigBuilder keystorePassword(byte[] keystorePassword) {
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
        //minimum specification is the keystore filename password, necessary for SecurityPolicy Transport binding validation
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
                    Objects.equal(signatureKeyAlias, otherConfig.getSignatureKeyAlias()) &&
                    Objects.equal(encryptionKeyAlias, otherConfig.getEncryptionKeyAlias()) &&
                    Arrays.equals(signatureKeyPassword, otherConfig.getSignatureKeyPassword()) &&
                    Arrays.equals(encryptionKeyPassword, otherConfig.getEncryptionKeyPassword());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (keystoreFileName + new String(keystorePassword) + signatureKeyAlias + encryptionKeyAlias).hashCode();
    }

    public JsonValue toJson() {
        try {
            return json(object(field(KEYSTORE_FILENAME, keystoreFileName),
                    field(KEYSTORE_PASSWORD, new String(keystorePassword, AMSTSConstants.UTF_8_CHARSET_ID)),
                    field(SIGNATURE_KEY_ALIAS, signatureKeyAlias), field(ENCRYPTION_KEY_ALIAS, encryptionKeyAlias),
                    field(SIGNATURE_KEY_PASSWORD, signatureKeyPassword != null ? new String(signatureKeyPassword, AMSTSConstants.UTF_8_CHARSET_ID) : null),
                    field(ENCRYPTION_KEY_PASSWORD, encryptionKeyPassword!= null ? new String(encryptionKeyPassword, AMSTSConstants.UTF_8_CHARSET_ID) : null)));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding when marshalling from byte[] to String: " + e, e);
        }
    }

    /**
     * Marshal out a SoapSTSKeystoreConfig instance if it was specified in the encapsulating SoapSTSInstanceConfig.
     * Note that if a Soap STS has no SecurityPolicy bindings, or only a transport SecurityPolicy binding, the
     * SoapSTSKeystoreConfig might be null
     * @param json the json representation of state which possibly includes SoapSTSKeystoreConfig state.
     * @return A SoapSTSKeystoreConfig or null if the JsonValue parameter is null.
     * @throws IllegalStateException If an encoding exception is encountered
     */
    public static SoapSTSKeystoreConfig fromJson(JsonValue json) throws IllegalStateException {
        //first check to see if mandatory values are present (keystore name and passsword). If not, return null, as
        //this means that a SoapSTSKeystoreConfig instance was not configured
        if (json.get(KEYSTORE_FILENAME).isNull()) {
            return null;
        }
        try {
            return SoapSTSKeystoreConfig.builder()
                    .keystoreFileName(json.get(KEYSTORE_FILENAME).asString())
                    .keystorePassword(json.get(KEYSTORE_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .signatureKeyAlias(!json.get(SIGNATURE_KEY_ALIAS).isNull() ? json.get(SIGNATURE_KEY_ALIAS).asString() : null)
                    .encryptionKeyAlias(!json.get(ENCRYPTION_KEY_ALIAS).isNull() ? json.get(ENCRYPTION_KEY_ALIAS).asString() : null)
                    .signatureKeyPassword(!json.get(SIGNATURE_KEY_PASSWORD).isNull() ? json.get(SIGNATURE_KEY_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID) : null)
                    .encryptionKeyPassword(!json.get(ENCRYPTION_KEY_PASSWORD).isNull() ? json.get(ENCRYPTION_KEY_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID) : null)
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