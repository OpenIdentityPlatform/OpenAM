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
* Copyright 2014 ForgeRock AS. All rights reserved.
*/

package org.forgerock.openam.sts.config.user;

import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.apache.xml.security.encryption.XMLCipher;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshallUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * TODO: Ambiguity in the context of setting the customAttributeStatementsProviderClassName
 * and the customAttributeMapperClassName. As it currently stands, the customAttributeStatementsProvider will be passed
 * an instance of the customAttributeMapper if both are specified. The usual case will simply to set the customAttributeMapper,
 * as this allows custom attributes to be set in the AttributeStatement.
 *
 * TODO: do I want a name-qualifier in addition to a nameIdFormat?
 *
 * Currently, each published rest-sts instance will encapsulate state to allow it to issue saml2 assertions for a single
 * SP. Thus the spEntityId, and spAcsUrl (the url of the SP's assertion consumer service) are specified in this class.
 * The signatureAlias corresponds to the IDP's signing
 * key, and the encryptionKeyAlias could correspond to the SP's public key corresponding to the key used to encrypt the
 * symmetric key used to encrypt assertion elements.
 */
public class SAML2Config {
    private static final String EQUALS = "=";
    public static class SAML2ConfigBuilder {
        /*
        use the ws-security constant, instead of the SAML2Constants defined in openam-federation, as this dependency
        introduces a dependency on openam-core, which pulls the ws-* dependencies into the soap-sts, which I don't want.
         */
        private String nameIdFormat = SAML2Constants.NAMEID_FORMAT_UNSPECIFIED;
        private Map<String, String> attributeMap;
        private long tokenLifetimeInSeconds = 60 * 10; //default token lifetime is 10 minutes
        private String customConditionsProviderClassName;
        private String customSubjectProviderClassName;
        private String customAuthenticationStatementsProviderClassName;
        private String customAttributeStatementsProviderClassName;
        private String customAuthzDecisionStatementsProviderClassName;
        private String customAttributeMapperClassName;
        private String customAuthNContextMapperClassName;
        private String spEntityId;
        private String spAcsUrl;
        private boolean signAssertion;
        private boolean encryptNameID;
        private boolean encryptAttributes;
        private boolean encryptAssertion;
        private String encryptionAlgorithm;
        private int encryptionAlgorithmStrength;
        private String keystoreFileName;
        private byte[] keystorePassword;
        /*
        Corresponds to the key used to sign the assertion.
         */
        private String signatureKeyAlias;
        private byte[] signatureKeyPassword;
        /*
        Corresponds to the SP's x509 cert -  the corresponding public key is used to encrypt the symmetric key used to
        encrypt assertion elements
         */
        private String encryptionKeyAlias;


        public SAML2ConfigBuilder nameIdFormat(String nameIdFormat) {
            //TODO - test to see if it matches one of the allowed values?
            this.nameIdFormat = nameIdFormat;
            return this;
        }

        public SAML2ConfigBuilder attributeMap(Map<String, String> attributeMap) {
            this.attributeMap = Collections.unmodifiableMap(attributeMap);
            return this;
        }

        public SAML2ConfigBuilder tokenLifetimeInSeconds(long lifetimeInSeconds) {
            this.tokenLifetimeInSeconds = lifetimeInSeconds;
            return this;
        }

        public SAML2ConfigBuilder customConditionsProviderClassName(String customConditionsProviderClassName) {
            this.customConditionsProviderClassName = customConditionsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customSubjectProviderClassName(String customSubjectProviderClassName) {
            this.customSubjectProviderClassName = customSubjectProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAuthenticationStatementsProviderClassName(String customAuthenticationStatementsProviderClassName) {
            this.customAuthenticationStatementsProviderClassName = customAuthenticationStatementsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAttributeStatementsProviderClassName(String customAttributeStatementsProviderClassName) {
            this.customAttributeStatementsProviderClassName = customAttributeStatementsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAuthzDecisionStatementsProviderClassName(String customAuthzDecisionStatementsProviderClassName) {
            this.customAuthzDecisionStatementsProviderClassName = customAuthzDecisionStatementsProviderClassName;
            return this;
        }

        public SAML2ConfigBuilder customAttributeMapperClassName(String customAttributeMapperClassName) {
            this.customAttributeMapperClassName = customAttributeMapperClassName;
            return this;
        }

        public SAML2ConfigBuilder customAuthNContextMapperClassName(String customAuthNContextMapperClassName) {
            this.customAuthNContextMapperClassName = customAuthNContextMapperClassName;
            return this;
        }

        public SAML2ConfigBuilder spEntityId(String spEntityId) {
            this.spEntityId = spEntityId;
            return this;
        }

        public SAML2ConfigBuilder spAcsUrl(String spAcsUrl) {
            this.spAcsUrl = spAcsUrl;
            return this;
        }

        public SAML2ConfigBuilder signatureKeyAlias(String signatureKeyAlias) {
            this.signatureKeyAlias = signatureKeyAlias;
            return this;
        }

        public SAML2ConfigBuilder signatureKeyPassword(byte[] signatureKeyPassword) {
            this.signatureKeyPassword = signatureKeyPassword;
            return this;
        }

        public SAML2ConfigBuilder encryptionKeyAlias(String encryptionKeyAlias) {
            this.encryptionKeyAlias = encryptionKeyAlias;
            return this;
        }

        public SAML2ConfigBuilder signAssertion(boolean signAssertion) {
            this.signAssertion = signAssertion;
            return this;
        }

        public SAML2ConfigBuilder encryptNameID(boolean encryptNameID) {
            this.encryptNameID = encryptNameID;
            return this;
        }

        public SAML2ConfigBuilder encryptAttributes(boolean encryptAttributes) {
            this.encryptAttributes = encryptAttributes;
            return this;
        }

        public SAML2ConfigBuilder encryptAssertion(boolean encryptAssertion) {
            this.encryptAssertion = encryptAssertion;
            return this;
        }

        /*
        Note that the encryption of SAML2 assertions, is, by default, delegated to the FMEncProvider class, which supports
        only http://www.w3.org/2001/04/xmlenc#aes128-cbc, http://www.w3.org/2001/04/xmlenc#aes192-cbc,
        http://www.w3.org/2001/04/xmlenc#aes256-cbc, or http://www.w3.org/2001/04/xmlenc#tripledes-cbc. However, because
        this EncProvider implementation can be over-ridden by setting the com.sun.identity.saml2.xmlenc.EncryptionProvider
        property, I can't reject the specification of an encryption algorithm not supported by the FMEncProvider, as
        I don't know whether this property has been over-ridden.

        Note also that I will remove http://www.w3.org/2001/04/xmlenc#tripledes-cbc from the set of choices exposed
        in the UI. There seems to be a bug in the FMEncProvider - when the tripledes-cbc is chosen, note on line 294 that
        this string http://www.w3.org/2001/04/xmlenc#tripledes-cbc is passed to XMLCipher.getInstance resulting in the
        error below:
        org.apache.xml.security.encryption.XMLEncryptionException: Wrong algorithm: DESede or TripleDES required
        The correct thing is done in FMEncProvider#generateSecretKey, where the http://www.w3.org/2001/04/xmlenc#tripledes-cbc
        is translated to 'TripleDES' before being passed to the XMLCipher - and this actually works.
         */
        public SAML2ConfigBuilder encryptionAlgorithm(String encryptionAlgorithm) {
            this.encryptionAlgorithm = encryptionAlgorithm;
            return this;
        }

        /*
        Note that the encryption of SAML2 assertions, is, by default, delegated to the FMEncProvider class, which supports
        only encryption algorithm strength values of 128, 192, and 256 for the encryption types XMLCipher.AES_128,
        XMLCipher.AES_192, and XMLCipher.AES_256, respectively. It does not look like the XMLCipher.TRIPLEDES supports a
        key encryption strength (see FMEncProvider for details). Given that the encryption strength is directly related
        to the cipher, it seems a bit silly to set these values. However, because
        this EncProvider implementation can be over-ridden by setting the com.sun.identity.saml2.xmlenc.EncryptionProvider
        property, and because the EncProvider specifies an encryption strength parameter, it would seem that I would have
        to support the setting of this seemingly superfluous parameter, just to support the plug-in interface. For now,
        I will not expose this value in the UI, as it adds unnecessary complexity, and the encryption algorithms are
        pre-defined as well. I will simply set this value in the UI context based upon the encryption algorithm. If
        a customer wants to specify a custom value because they have implemented their own EncryptionProvider, then they
        can publish a rest-sts instance programmatically.
         */
        public SAML2ConfigBuilder encryptionAlgorithmStrength(int encryptionAlgorithmStrength) {
            this.encryptionAlgorithmStrength = encryptionAlgorithmStrength;
            return this;
        }

        public SAML2ConfigBuilder keystoreFile(String keystoreFileName) {
            this.keystoreFileName = keystoreFileName;
            return this;
        }

        public SAML2ConfigBuilder keystorePassword(byte[] keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public SAML2Config build() {
            return new SAML2Config(this);
        }
    }

    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    private static final String NAME_ID_FORMAT = "saml2-name-id-format";
    private static final String ATTRIBUTE_MAP = "saml2-attribute-map";
    private static final String TOKEN_LIFETIME = SharedSTSConstants.SAML2_TOKEN_LIFETIME;
    private static final String CUSTOM_CONDITIONS_PROVIDER_CLASS = "saml2-custom-conditions-provider-class-name";
    private static final String CUSTOM_SUBJECT_PROVIDER_CLASS = "saml2-custom-subject-provider-class-name";
    private static final String CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS = "saml2-custom-attribute-statements-provider-class-name";
    private static final String CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS = "saml2-custom-authentication-statements-provider-class-name";
    private static final String CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS = "saml2-custom-authz-decision-statements-provider-class-name";
    private static final String CUSTOM_ATTRIBUTE_MAPPER_CLASS = "saml2-custom-attribute-mapper-class-name";
    private static final String CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS = "saml2-custom-authn-context-mapper-class-name";
    private static final String SIGN_ASSERTION = SharedSTSConstants.SAML2_SIGN_ASSERTION;
    private static final String ENCRYPT_ATTRIBUTES = SharedSTSConstants.SAML2_ENCRYPT_ATTRIBUTES;
    private static final String ENCRYPT_NAME_ID = SharedSTSConstants.SAML2_ENCRYPT_NAME_ID;
    private static final String ENCRYPT_ASSERTION = SharedSTSConstants.SAML2_ENCRYPT_ASSERTION;
    private static final String ENCRYPTION_ALGORITHM = SharedSTSConstants.SAML2_ENCRYPTION_ALGORITHM;
    private static final String ENCRYPTION_ALGORITHM_STRENGTH = SharedSTSConstants.SAML2_ENCRYPTION_ALGORITHM_STRENGTH;
    private static final String KEYSTORE_FILE_NAME = SharedSTSConstants.SAML2_KEYSTORE_FILE_NAME;
    private static final String KEYSTORE_PASSWORD = SharedSTSConstants.SAML2_KEYSTORE_PASSWORD;
    private static final String SP_ENTITY_ID = SharedSTSConstants.SAML2_SP_ENTITY_ID;
    private static final String SP_ACS_URL = SharedSTSConstants.SAML2_SP_ACS_URL;
    private static final String ENCRYPTION_KEY_ALIAS = SharedSTSConstants.SAML2_ENCRYPTION_KEY_ALIAS;
    private static final String SIGNATURE_KEY_ALIAS = SharedSTSConstants.SAML2_SIGNATURE_KEY_ALIAS;
    private static final String SIGNATURE_KEY_PASSWORD = SharedSTSConstants.SAML2_SIGNATURE_KEY_PASSWORD;

    private final String nameIdFormat;
    private final Map<String, String> attributeMap;
    private final long tokenLifetimeInSeconds;
    private final String customConditionsProviderClassName;
    private final String customSubjectProviderClassName;
    private final String customAuthenticationStatementsProviderClassName;
    private final String customAttributeStatementsProviderClassName;
    private final String customAuthzDecisionStatementsProviderClassName;
    private final String customAttributeMapperClassName;
    private final String customAuthNContextMapperClassName;
    private final String spEntityId;
    private final String spAcsUrl;
    private final boolean signAssertion;
    private final boolean encryptNameID;
    private final boolean encryptAttributes;
    private final boolean encryptAssertion;
    private final String encryptionAlgorithm;
    private final int encryptionAlgorithmStrength;
    private final String keystoreFileName;
    private final byte[] keystorePassword;
    private final String signatureKeyAlias;
    private final byte[] signatureKeyPassword;
    private final String encryptionKeyAlias;

    private SAML2Config(SAML2ConfigBuilder builder) {
        this.nameIdFormat = builder.nameIdFormat; //not required so don't reject if null
        if (builder.attributeMap != null) {
            this.attributeMap = Collections.unmodifiableMap(builder.attributeMap);
        } else {
            attributeMap = Collections.emptyMap();
        }
        tokenLifetimeInSeconds = builder.tokenLifetimeInSeconds; //will be set to default if not explicitly set
        customConditionsProviderClassName = builder.customConditionsProviderClassName;
        customSubjectProviderClassName = builder.customSubjectProviderClassName;
        customAuthenticationStatementsProviderClassName = builder.customAuthenticationStatementsProviderClassName;
        customAuthzDecisionStatementsProviderClassName = builder.customAuthzDecisionStatementsProviderClassName;
        customAttributeStatementsProviderClassName = builder.customAttributeStatementsProviderClassName;
        customAttributeMapperClassName = builder.customAttributeMapperClassName;
        customAuthNContextMapperClassName = builder.customAuthNContextMapperClassName;
        this.signAssertion = builder.signAssertion;
        this.encryptNameID = builder.encryptNameID;
        this.encryptAttributes = builder.encryptAttributes;
        this.encryptAssertion = builder.encryptAssertion;
        this.encryptionAlgorithm = builder.encryptionAlgorithm;
        this.encryptionAlgorithmStrength = builder.encryptionAlgorithmStrength;
        this.keystoreFileName = builder.keystoreFileName;
        this.keystorePassword = builder.keystorePassword;
        this.spEntityId = builder.spEntityId;
        this.spAcsUrl = builder.spAcsUrl;
        this.signatureKeyAlias = builder.signatureKeyAlias;
        this.signatureKeyPassword = builder.signatureKeyPassword;
        this.encryptionKeyAlias = builder.encryptionKeyAlias;

        if (spEntityId ==  null) {
            throw new IllegalArgumentException("The entity id of the consumer (SP) for issued assertions must be specified.");
        }
        if (encryptAssertion || encryptNameID || encryptAttributes) {
            if (encryptionAlgorithm == null) {
                throw new IllegalArgumentException("If elements of the assertion are to be encrypted, an encryption " +
                        "algorithm must be specified.");
            }
            if (encryptionAlgorithmStrength == 0 && !XMLCipher.TRIPLEDES.equals(encryptionAlgorithm)) {
                throw new IllegalArgumentException("If elements of the assertion are to be encrypted, an encryption " +
                        "algorithm strength must be specified.");
            }
            if (encryptionKeyAlias ==  null) {
                throw new IllegalArgumentException("If elements of the assertion are to be encrypted, an encryption key" +
                        "alias  must be specified.");
            }
        }
        if (encryptAssertion || encryptNameID || encryptAttributes || signAssertion) {
            if (keystorePassword == null || keystoreFileName == null) {
                throw new IllegalArgumentException("If the assertions are to be signed or encrypted, then the keystore " +
                        "file and password must be specified.");
            }
        }
        if (signAssertion) {
            if ((signatureKeyPassword == null) || (signatureKeyAlias == null)) {
                throw new IllegalArgumentException("If the assertion is to be signed, then the signature key alias and" +
                        " signature key password must be specified.");
            }
        }

        if (encryptAssertion && (encryptNameID || encryptAttributes)) {
            throw new IllegalArgumentException("Either the entire assertion can be encrypted, or the Attributes and/or NameID.");
        }
    }

    public static SAML2ConfigBuilder builder() {
        return new SAML2ConfigBuilder();
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public long getTokenLifetimeInSeconds() {
        return tokenLifetimeInSeconds;
    }

    public Map<String, String> getAttributeMap() {
        return attributeMap;
    }

    public String getCustomConditionsProviderClassName() {
        return customConditionsProviderClassName;
    }

    public String getCustomSubjectProviderClassName() {
        return customSubjectProviderClassName;
    }

    public String getCustomAuthenticationStatementsProviderClassName() {
        return customAuthenticationStatementsProviderClassName;
    }

    public String getCustomAttributeMapperClassName() {
        return customAttributeMapperClassName;
    }

    public String getCustomAuthNContextMapperClassName() {
        return customAuthNContextMapperClassName;
    }

    public String getCustomAttributeStatementsProviderClassName() {
        return customAttributeStatementsProviderClassName;
    }

    public String getCustomAuthzDecisionStatementsProviderClassName() {
        return customAuthzDecisionStatementsProviderClassName;
    }

    public boolean signAssertion() {
        return signAssertion;
    }

    public boolean encryptNameID() {
        return encryptNameID;
    }

    public boolean encryptAttributes() {
        return encryptAttributes;
    }

    public boolean encryptAssertion() {
        return encryptAssertion;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public int getEncryptionAlgorithmStrength() {
        return encryptionAlgorithmStrength;
    }

    public String getKeystoreFileName() {
        return keystoreFileName;
    }

    public byte[] getKeystorePassword() {
        return keystorePassword;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public String getSpAcsUrl() {
        return spAcsUrl;
    }

    public String getEncryptionKeyAlias() {
        return encryptionKeyAlias;
    }

    public String getSignatureKeyAlias() {
        return signatureKeyAlias;
    }

    public byte[] getSignatureKeyPassword() {
        return signatureKeyPassword;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SAML2Config instance:").append('\n');
        sb.append('\t').append("nameIDFormat: ").append(nameIdFormat).append('\n');
        sb.append('\t').append("attributeMap: ").append(attributeMap).append('\n');
        sb.append('\t').append("tokenLifetimeInSeconds: ").append(tokenLifetimeInSeconds).append('\n');
        sb.append('\t').append("customConditionsProviderClassName: ").append(customConditionsProviderClassName).append('\n');
        sb.append('\t').append("customSubjectProviderClassName: ").append(customSubjectProviderClassName).append('\n');
        sb.append('\t').append("customAttributeStatementsProviderClassName: ").append(customAttributeStatementsProviderClassName).append('\n');
        sb.append('\t').append("customAttributeMapperClassName: ").append(customAttributeMapperClassName).append('\n');
        sb.append('\t').append("customAuthNContextMapperClassName: ").append(customAuthNContextMapperClassName).append('\n');
        sb.append('\t').append("customAuthenticationStatementsProviderClassName: ").append(customAuthenticationStatementsProviderClassName).append('\n');
        sb.append('\t').append("customAuthzDecisionStatementsProviderClassName: ").append(customAuthzDecisionStatementsProviderClassName).append('\n');
        sb.append('\t').append("Sign assertion ").append(signAssertion).append('\n');
        sb.append('\t').append("Encrypt NameID ").append(encryptNameID).append('\n');
        sb.append('\t').append("Encrypt Attributes ").append(encryptAttributes).append('\n');
        sb.append('\t').append("Encrypt Assertion ").append(encryptAssertion).append('\n');
        sb.append('\t').append("Encryption Algorithm ").append(encryptionAlgorithm).append('\n');
        sb.append('\t').append("Encryption Algorithm Strength ").append(encryptionAlgorithmStrength).append('\n');
        sb.append('\t').append("Keystore File ").append(keystoreFileName).append('\n');
        sb.append('\t').append("Keystore Password ").append("xxx").append('\n');
        sb.append('\t').append("SP Entity Id ").append(spEntityId).append('\n');
        sb.append('\t').append("SP ACS URL ").append(spAcsUrl).append('\n');
        sb.append('\t').append("Encryption key alias ").append(encryptionKeyAlias).append('\n');
        sb.append('\t').append("Signature key alias").append(signatureKeyAlias).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SAML2Config) {
            SAML2Config otherConfig = (SAML2Config)other;
            return nameIdFormat.equals(otherConfig.getNameIdFormat()) &&
                    tokenLifetimeInSeconds == otherConfig.tokenLifetimeInSeconds &&
                    attributeMap.equals(otherConfig.attributeMap) &&
                    signAssertion == otherConfig.signAssertion() &&
                    encryptAssertion == otherConfig.encryptAssertion() &&
                    encryptAttributes == otherConfig.encryptAttributes() &&
                    encryptNameID == otherConfig.encryptNameID() &&
                    encryptionAlgorithmStrength == otherConfig.encryptionAlgorithmStrength &&
                    spEntityId.equals(otherConfig.spEntityId) &&
                    (encryptionAlgorithm != null
                            ? encryptionAlgorithm.equals(otherConfig.getEncryptionAlgorithm())
                            : otherConfig.getEncryptionAlgorithm() == null) &&
                    (customConditionsProviderClassName != null
                            ? customConditionsProviderClassName.equals(otherConfig.getCustomConditionsProviderClassName())
                            : otherConfig.getCustomConditionsProviderClassName() == null) &&
                    (customSubjectProviderClassName != null
                            ? customSubjectProviderClassName.equals(otherConfig.getCustomSubjectProviderClassName())
                            : otherConfig.getCustomSubjectProviderClassName() == null) &&
                    (customAttributeStatementsProviderClassName != null
                        ? customAttributeStatementsProviderClassName.equals(otherConfig.getCustomAttributeStatementsProviderClassName())
                        : otherConfig.getCustomAttributeStatementsProviderClassName() == null) &&
                    (customAuthzDecisionStatementsProviderClassName != null
                            ? customAuthzDecisionStatementsProviderClassName.equals(otherConfig.getCustomAuthzDecisionStatementsProviderClassName())
                            : otherConfig.getCustomAuthzDecisionStatementsProviderClassName() == null) &&
                    (customAttributeMapperClassName != null
                            ? customAttributeMapperClassName.equals(otherConfig.getCustomAttributeMapperClassName())
                            : otherConfig.getCustomAttributeMapperClassName() == null) &&
                    (customAuthNContextMapperClassName != null
                            ? customAuthNContextMapperClassName.equals(otherConfig.getCustomAuthNContextMapperClassName())
                            : otherConfig.getCustomAuthNContextMapperClassName() == null) &&
                    (customAuthenticationStatementsProviderClassName != null
                        ? customAuthenticationStatementsProviderClassName.equals(otherConfig.getCustomAuthenticationStatementsProviderClassName())
                        : otherConfig.getCustomAuthenticationStatementsProviderClassName() == null) &&
                    (keystoreFileName != null
                            ? keystoreFileName.equals(otherConfig.getKeystoreFileName())
                            : otherConfig.getKeystoreFileName() == null) &&
                    (keystorePassword != null
                            ? Arrays.equals(keystorePassword, otherConfig.getKeystorePassword())
                            : otherConfig.getKeystorePassword() == null) &&
                    (spAcsUrl != null
                            ? spAcsUrl.equals(otherConfig.getSpAcsUrl())
                            : otherConfig.getSpAcsUrl() == null) &&
                    (signatureKeyAlias != null
                            ? signatureKeyAlias.equals(otherConfig.getSignatureKeyAlias())
                            : otherConfig.getSignatureKeyAlias() == null) &&
                    (encryptionKeyAlias != null
                            ? encryptionKeyAlias.equals(otherConfig.getEncryptionKeyAlias())
                            : otherConfig.getEncryptionKeyAlias() == null) &&
                    (signatureKeyPassword != null
                            ? Arrays.equals(signatureKeyPassword, otherConfig.getSignatureKeyPassword())
                            : otherConfig.getSignatureKeyPassword() == null);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (nameIdFormat + attributeMap + spEntityId + Long.toString(tokenLifetimeInSeconds)).hashCode();
    }

    /*
    Because toJson will be used to produce the map that will also be used to marshal to the SMS attribute map format, and
    because the SMS attribute map format represents all values as Set<String>, I need to represent all of the json values
    as strings as well.
     */
    public JsonValue toJson() {
        try {
            return json(object(
                    field(NAME_ID_FORMAT, nameIdFormat),
                    field(TOKEN_LIFETIME, String.valueOf(tokenLifetimeInSeconds)),
                    field(CUSTOM_CONDITIONS_PROVIDER_CLASS, customConditionsProviderClassName),
                    field(CUSTOM_SUBJECT_PROVIDER_CLASS, customSubjectProviderClassName),
                    field(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS, customAttributeStatementsProviderClassName),
                    field(CUSTOM_ATTRIBUTE_MAPPER_CLASS, customAttributeMapperClassName),
                    field(CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS, customAuthNContextMapperClassName),
                    field(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS, customAuthenticationStatementsProviderClassName),
                    field(CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS, customAuthzDecisionStatementsProviderClassName),
                    field(SIGN_ASSERTION, String.valueOf(signAssertion)),
                    field(ENCRYPT_ASSERTION, String.valueOf(encryptAssertion)),
                    field(ENCRYPT_ATTRIBUTES, String.valueOf(encryptAttributes)),
                    field(ENCRYPT_NAME_ID, String.valueOf(encryptNameID)),
                    field(ENCRYPTION_ALGORITHM, encryptionAlgorithm),
                    field(ENCRYPTION_ALGORITHM_STRENGTH, String.valueOf(encryptionAlgorithmStrength)),
                    field(ATTRIBUTE_MAP, attributeMap),
                    field(KEYSTORE_FILE_NAME, keystoreFileName),
                    field(KEYSTORE_PASSWORD,
                            keystorePassword != null ? new String(keystorePassword, AMSTSConstants.UTF_8_CHARSET_ID) : null),
                    field(SP_ACS_URL, spAcsUrl),
                    field(SP_ENTITY_ID, spEntityId),
                    field(SIGNATURE_KEY_ALIAS, signatureKeyAlias),
                    field(SIGNATURE_KEY_PASSWORD,
                            signatureKeyPassword != null ? new String(signatureKeyPassword, AMSTSConstants.UTF_8_CHARSET_ID) : null),
                    field(ENCRYPTION_KEY_ALIAS, encryptionKeyAlias)));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding when marshalling from String to to byte[]: " + e, e);
        }
    }

    public static SAML2Config fromJson(JsonValue json) throws IllegalStateException {
        try {
            return SAML2Config.builder()
                    .nameIdFormat(json.get(NAME_ID_FORMAT).asString())
                    //because we have to go to the SMS Map representation, where all values are Set<String>, I need to
                    // pull the value from Json as a string, and then parse out a Long.
                    .tokenLifetimeInSeconds(Long.valueOf(json.get(TOKEN_LIFETIME).asString()))
                    .customConditionsProviderClassName(json.get(CUSTOM_CONDITIONS_PROVIDER_CLASS).asString())
                    .customSubjectProviderClassName(json.get(CUSTOM_SUBJECT_PROVIDER_CLASS).asString())
                    .customAttributeStatementsProviderClassName(json.get(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS).asString())
                    .customAttributeMapperClassName(json.get(CUSTOM_ATTRIBUTE_MAPPER_CLASS).asString())
                    .customAuthNContextMapperClassName(json.get(CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS).asString())
                    .customAuthenticationStatementsProviderClassName(json.get(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS).asString())
                    .customAuthzDecisionStatementsProviderClassName(json.get(CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS).asString())
                    .signAssertion(Boolean.valueOf(json.get(SIGN_ASSERTION).asString()))
                    .encryptAssertion(Boolean.valueOf(json.get(ENCRYPT_ASSERTION).asString()))
                    .encryptNameID(Boolean.valueOf(json.get(ENCRYPT_NAME_ID).asString()))
                    .encryptAttributes(Boolean.valueOf(json.get(ENCRYPT_ATTRIBUTES).asString()))
                    .encryptionAlgorithm(json.get(ENCRYPTION_ALGORITHM).asString())
                    .encryptionAlgorithmStrength(Integer.valueOf(json.get(ENCRYPTION_ALGORITHM_STRENGTH).asString()))
                    .attributeMap(json.get(ATTRIBUTE_MAP).asMap(String.class))
                    .keystoreFile(json.get(KEYSTORE_FILE_NAME).asString())
                    .keystorePassword(json.get(KEYSTORE_PASSWORD).isString()
                            ? json.get(KEYSTORE_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID) : null)
                    .signatureKeyPassword(json.get(SIGNATURE_KEY_PASSWORD).isString()
                            ? json.get(SIGNATURE_KEY_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID) : null)
                    .signatureKeyAlias(json.get(SIGNATURE_KEY_ALIAS).asString())
                    .spAcsUrl(json.get(SP_ACS_URL).asString())
                    .spEntityId(json.get(SP_ENTITY_ID).asString())
                    .encryptionKeyAlias(json.get(ENCRYPTION_KEY_ALIAS).asString())
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding when marshalling from String to to byte[]: " + e, e);
        }
    }

    /*
    We need to marshal the SAML2Config instance to a Map<String, Object>. The JsonValue of toJson gets us there,
    except for the complex types for the audiences and attribute map. These need to be marshaled into a Set<String>, and
    these entries included in the top-level map, replacing the existing complex entries.
     */
    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Object> preMap = toJson().asMap();
        Map<String, Set<String>> finalMap = MapMarshallUtils.toSmsMap(preMap);
        Object attributesObject = preMap.get(ATTRIBUTE_MAP);
        if (attributesObject instanceof Map) {
            finalMap.remove(ATTRIBUTE_MAP);
            Set<String> attributeValues = new LinkedHashSet<String>();
            finalMap.put(ATTRIBUTE_MAP, attributeValues);
            for (Map.Entry<String, String> entry : ((Map<String, String>)attributesObject).entrySet()) {
                attributeValues.add(entry.getKey() + EQUALS + entry.getValue());
            }
        } else {
            throw new IllegalStateException("Type corresponding to " + ATTRIBUTE_MAP + " key unexpected. Type: "
                    + (attributesObject != null ? attributesObject.getClass().getName() :" null"));
        }
        return finalMap;
    }

    /*
    Here we have to modify the ATTRIBUTE_MAP and AUDIENCES entries to match the JsonValue format expected by
    fromJson, and then call the static fromJson. This method must marshal between the Json representation of a complex
    object, and the representation expected by the SMS
     */
    public static SAML2Config marshalFromAttributeMap(Map<String, Set<String>> smsAttributeMap) {
        Set<String> attributes = smsAttributeMap.get(ATTRIBUTE_MAP);
        /*
        The STSInstanceConfig may not have SAML2Config, if there are no defined token transformations that result
        in a SAML2 assertion. So if we have null attributes, this means that STSInstanceConfig.marshalFromAttributeMap
        was called. Note that we cannot check for isEmpty, as this will be the case if SAML2Config has been defined, but
        simply without any attributes.
         */
        if (attributes == null) {
            return null;
        }
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(smsAttributeMap);
        jsonAttributes.remove(ATTRIBUTE_MAP);
        Map<String, Object> jsonAttributeMap = new LinkedHashMap<String, Object>();
        for (String entry : attributes) {
            StringTokenizer st = new StringTokenizer(entry, EQUALS);
            jsonAttributeMap.put(st.nextToken(), st.nextToken());
        }
        jsonAttributes.put(ATTRIBUTE_MAP, new JsonValue(jsonAttributeMap));

        return fromJson(new JsonValue(jsonAttributes));
    }
}
