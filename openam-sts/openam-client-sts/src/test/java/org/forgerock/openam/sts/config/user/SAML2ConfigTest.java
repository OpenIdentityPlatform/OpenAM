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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertEquals;

import org.apache.xml.security.encryption.XMLCipher;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class SAML2ConfigTest {
    private static final String NAME_ID_FORMAT = "nameidformat";
    private static final long TOKEN_LIFETIME = 60 * 10;
    private static final String CUSTOM_SUBJECT_PROVIDER = "org.foo.MyCustomSubjectProvider";
    private static final String CUSTOM_CONDITIONS_PROVIDER = "org.foo.MyCustomConditionsProvider";
    private static final String CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER = "org.foo.MyCustomAttributeStatementsProvider";
    private static final String CUSTOM_ATTRIBUTE_MAPPER = "org.foo.MyCustomAttributeMapper";
    private static final String CUSTOM_AUTHN_CONTEXT_MAPPER = "org.foo.MyCustomAuthNContextMapper";
    private static final String CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER = "org.foo.MyCustomAuthenticationStatementsProvider";
    private static final String CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER = "org.foo.MyCustomAuthzDecisionStatementsProvider";
    private static final String SP_ENTITY_ID = "http://host.com/saml2/fooentity";
    private static final String ENCRYPTION_KEY_ALIAS = "encrypt_key_alias";
    private static final String SIGNATURE_KEY_ALIAS = "sig_key_alias";
    private static final byte[] SIGNATURE_KEY_PASSWORD = "super_secret".getBytes();
    private static final boolean WITH_ATTR_MAP = true;
    private static final boolean WITH_CUSTOM_PROVIDERS = true;
    private static final boolean SIGN_ASSERTION = true;
    private static final boolean ENCRYPT_ASSERTION = true;
    private static final boolean ENCRYPT_NAME_ID = true;
    private static final boolean ENCRYPT_ATTRIBUTES = true;

    @Test
    public void testEquals() {
        SAML2Config config1 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        SAML2Config config2 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(config1, config2);

        config1 = buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        config2 = buildConfig(!WITH_ATTR_MAP,  !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        config2 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertNotEquals(config1, config2);

        config1 = buildConfig(!WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        config2 = buildConfig(!WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        config2 = buildConfig(WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        config2 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        config2 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertNotEquals(config1, config2);

        config1 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        config2 = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertNotEquals(config1, config2);
    }

    @Test
    public void testJsonRoundTrip1() {
        SAML2Config config = buildConfig(WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testJsonRoundTrip2() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testJsonRoundTrip3() {
        SAML2Config config = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testJsonRoundTrip4() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertEquals(config, SAML2Config.fromJson(config.toJson()));
    }

    @Test
    public void testToString() {
        //build a bunch of instances and call toString to insure no NPE results
        buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID).toString();
        buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID).toString();
        buildConfig(WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID).toString();
        buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID).toString();
        buildConfig(!WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID).toString();
        buildConfig(WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID).toString();
        buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID).toString();
    }

    @Test
    public void testFieldPersistenceAfterRoundTrip() {
        SAML2Config config = buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        SAML2Config reconsitutedConfig = SAML2Config.fromJson(config.toJson());
        assertEquals(NAME_ID_FORMAT, reconsitutedConfig.getNameIdFormat());
        assertEquals(TOKEN_LIFETIME, reconsitutedConfig.getTokenLifetimeInSeconds());

        config = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        reconsitutedConfig = SAML2Config.fromJson(config.toJson());
        assertEquals(SIGN_ASSERTION, reconsitutedConfig.signAssertion());
        assertEquals(!ENCRYPT_ASSERTION, reconsitutedConfig.encryptAssertion());
        assertEquals(!ENCRYPT_ATTRIBUTES, reconsitutedConfig.encryptAttributes());
        assertEquals(ENCRYPT_NAME_ID, reconsitutedConfig.encryptNameID());
        assertEquals(NAME_ID_FORMAT, reconsitutedConfig.getNameIdFormat());
        assertEquals(TOKEN_LIFETIME, reconsitutedConfig.getTokenLifetimeInSeconds());
        assertEquals(CUSTOM_CONDITIONS_PROVIDER, reconsitutedConfig.getCustomConditionsProviderClassName());
        assertEquals(CUSTOM_SUBJECT_PROVIDER, reconsitutedConfig.getCustomSubjectProviderClassName());
        assertEquals(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER, reconsitutedConfig.getCustomAuthenticationStatementsProviderClassName());
        assertEquals(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER, reconsitutedConfig.getCustomAttributeStatementsProviderClassName());
        assertEquals(CUSTOM_ATTRIBUTE_MAPPER, reconsitutedConfig.getCustomAttributeMapperClassName());
        assertEquals(CUSTOM_AUTHN_CONTEXT_MAPPER, reconsitutedConfig.getCustomAuthNContextMapperClassName());
    }

    @Test
    public void testMapMarshalRoundTrip() {
        SAML2Config saml2Config = buildConfig(WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(WITH_ATTR_MAP, WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, !ENCRYPT_NAME_ID);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));

        saml2Config = buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                !ENCRYPT_ASSERTION, !ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
        assertEquals(saml2Config, SAML2Config.marshalFromAttributeMap(saml2Config.marshalToAttributeMap()));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIllegalEncryptionSpecification() {
        buildConfig(!WITH_ATTR_MAP, !WITH_CUSTOM_PROVIDERS, !SIGN_ASSERTION,
                ENCRYPT_ASSERTION, ENCRYPT_ATTRIBUTES, ENCRYPT_NAME_ID);
    }

    private SAML2Config buildConfig(boolean withAttributeMap, boolean withCustomProviders,
                                    boolean signAssertion, boolean encryptAssertion, boolean encryptAttributes, boolean encryptNameId) {
        SAML2Config.SAML2ConfigBuilder builder = SAML2Config.builder()
                .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                .signAssertion(signAssertion)
                .nameIdFormat(NAME_ID_FORMAT);
        if (withAttributeMap) {
            Map<String, String> attrMap = new LinkedHashMap<String, String>();
            attrMap.put("saml_attr", "ldap_attr");
            attrMap.put("saml_attr1", "ldap_attr1");
            builder.attributeMap(attrMap);
        }

        builder.encryptAssertion(encryptAssertion);
        builder.encryptAttributes(encryptAttributes);
        builder.encryptNameID(encryptNameId);

        builder.spEntityId(SP_ENTITY_ID);

        if (encryptAssertion || encryptAttributes || encryptNameId) {
            builder.encryptionAlgorithm(XMLCipher.AES_128);
            builder.encryptionAlgorithmStrength(128);
            builder.encryptionKeyAlias(ENCRYPTION_KEY_ALIAS);
        }

        if (encryptAssertion || encryptAttributes || encryptNameId || signAssertion) {
            builder.keystoreFile("/user/home/dillrod/keystore");
            builder.keystorePassword("da_secret".getBytes());
        }

        if (signAssertion) {
            builder.signatureKeyAlias(SIGNATURE_KEY_ALIAS);
            builder.signatureKeyPassword(SIGNATURE_KEY_PASSWORD);
        }

        if (withCustomProviders) {
            builder.customConditionsProviderClassName(CUSTOM_CONDITIONS_PROVIDER);
            builder.customSubjectProviderClassName(CUSTOM_SUBJECT_PROVIDER);
            builder.customAuthenticationStatementsProviderClassName(CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER);
            builder.customAttributeStatementsProviderClassName(CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER);
            builder.customAuthzDecisionStatementsProviderClassName(CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER);
            builder.customAttributeMapperClassName(CUSTOM_ATTRIBUTE_MAPPER);
            builder.customAuthNContextMapperClassName(CUSTOM_AUTHN_CONTEXT_MAPPER);
        }
        return builder.build();
    }
}
