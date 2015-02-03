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

import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class STSInstanceConfigTest {
    private static final String ISSUER = "cornholio";

    @Test
    public void testSettings() throws UnsupportedEncodingException {
        STSInstanceConfig instance = buildConfig();
        assertTrue(ISSUER.equals(instance.getIssuerName()));
    }

    @Test
    public void testJsonRoundTrip() throws UnsupportedEncodingException {
        STSInstanceConfig instance = buildConfig();
        STSInstanceConfig secondInstance = STSInstanceConfig.fromJson(instance.toJson());
        assertTrue(instance.equals(secondInstance));
    }

    @Test
    public void testJsonRoundTripWithSaml2Config() throws UnsupportedEncodingException {
        STSInstanceConfig instance = buildConfigWithSaml2Config();
        STSInstanceConfig secondInstance = STSInstanceConfig.fromJson(instance.toJson());
        assertTrue(instance.equals(secondInstance));
    }

    @Test
    public void testEquals() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfig();
        STSInstanceConfig instance2 = buildConfig();
        assertTrue(instance1.equals(instance2));
    }

    @Test
    public void testEqualsWithSamlConfig() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfigWithSaml2Config();
        STSInstanceConfig instance2 = buildConfigWithSaml2Config();
        assertTrue(instance1.equals(instance2));
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfig();
        STSInstanceConfig instance2 = buildConfigWithSaml2Config();
        assertFalse(instance2.equals(instance1));
        assertFalse(instance1.equals(instance2));
    }

    @Test
    public void testMapMarshalRoundTrip() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfig();
        assertEquals(instance1, STSInstanceConfig.marshalFromAttributeMap(instance1.marshalToAttributeMap()));

        instance1 = buildConfigWithSaml2Config();
        assertEquals(instance1, STSInstanceConfig.marshalFromAttributeMap(instance1.marshalToAttributeMap()));

    }

    /*
    Because SoapSTSInstanceConfig and RestSTSInstanceConfig and encapsulated instances must be marshaled to a
    Map<String, Set<String>> for SMS persistence,
    STSInstanceConfig and encapsulated classes define statics that define the keys which must correspond to the String
    keys in the SMS-persisted-map, values which must correspond to the entries defined in restSTS.xml and soapSTS.xml.
    This unit test tests this correspondence.
    */
    @Test
    public void testServicePropertyFileCorrespondence() throws IOException {
        String restSTSfileContent =
                IOUtils.getFileContent("../../openam-server-only/src/main/resources/services/restSTS.xml");
        String soapSTSfileContent =
                IOUtils.getFileContent("../../openam-server-only/src/main/resources/services/soapSTS.xml");
        assertTrue(restSTSfileContent.contains(STSInstanceConfig.ISSUER_NAME));
        assertTrue(soapSTSfileContent.contains(STSInstanceConfig.ISSUER_NAME));

        assertTrue(soapSTSfileContent.contains(SAML2Config.NAME_ID_FORMAT));
        assertTrue(restSTSfileContent.contains(SAML2Config.NAME_ID_FORMAT));

        assertTrue(soapSTSfileContent.contains(SAML2Config.ATTRIBUTE_MAP));
        assertTrue(restSTSfileContent.contains(SAML2Config.ATTRIBUTE_MAP));

        assertTrue(soapSTSfileContent.contains(SAML2Config.TOKEN_LIFETIME));
        assertTrue(restSTSfileContent.contains(SAML2Config.TOKEN_LIFETIME));

        assertTrue(soapSTSfileContent.contains(SAML2Config.CUSTOM_CONDITIONS_PROVIDER_CLASS));
        assertTrue(restSTSfileContent.contains(SAML2Config.CUSTOM_CONDITIONS_PROVIDER_CLASS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.CUSTOM_SUBJECT_PROVIDER_CLASS));
        assertTrue(restSTSfileContent.contains(SAML2Config.CUSTOM_SUBJECT_PROVIDER_CLASS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS));
        assertTrue(restSTSfileContent.contains(SAML2Config.CUSTOM_ATTRIBUTE_STATEMENTS_PROVIDER_CLASS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS));
        assertTrue(restSTSfileContent.contains(SAML2Config.CUSTOM_AUTHENTICATION_STATEMENTS_PROVIDER_CLASS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS));
        assertTrue(restSTSfileContent.contains(SAML2Config.CUSTOM_AUTHZ_DECISION_STATEMENTS_PROVIDER_CLASS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.CUSTOM_ATTRIBUTE_MAPPER_CLASS));
        assertTrue(restSTSfileContent.contains(SAML2Config.CUSTOM_ATTRIBUTE_MAPPER_CLASS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS));
        assertTrue(restSTSfileContent.contains(SAML2Config.CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.SIGN_ASSERTION));
        assertTrue(restSTSfileContent.contains(SAML2Config.SIGN_ASSERTION));

        assertTrue(soapSTSfileContent.contains(SAML2Config.ENCRYPT_ATTRIBUTES));
        assertTrue(restSTSfileContent.contains(SAML2Config.ENCRYPT_ATTRIBUTES));

        assertTrue(soapSTSfileContent.contains(SAML2Config.ENCRYPT_NAME_ID));
        assertTrue(restSTSfileContent.contains(SAML2Config.ENCRYPT_NAME_ID));

        assertTrue(soapSTSfileContent.contains(SAML2Config.ENCRYPT_ASSERTION));
        assertTrue(restSTSfileContent.contains(SAML2Config.ENCRYPT_ASSERTION));

        assertTrue(soapSTSfileContent.contains(SAML2Config.ENCRYPTION_ALGORITHM));
        assertTrue(restSTSfileContent.contains(SAML2Config.ENCRYPTION_ALGORITHM));

        assertTrue(soapSTSfileContent.contains(SAML2Config.ENCRYPTION_ALGORITHM_STRENGTH));
        assertTrue(restSTSfileContent.contains(SAML2Config.ENCRYPTION_ALGORITHM_STRENGTH));

        assertTrue(soapSTSfileContent.contains(SAML2Config.KEYSTORE_FILE_NAME));
        assertTrue(restSTSfileContent.contains(SAML2Config.KEYSTORE_FILE_NAME));

        assertTrue(soapSTSfileContent.contains(SAML2Config.KEYSTORE_PASSWORD));
        assertTrue(restSTSfileContent.contains(SAML2Config.KEYSTORE_PASSWORD));

        assertTrue(soapSTSfileContent.contains(SAML2Config.SP_ENTITY_ID));
        assertTrue(restSTSfileContent.contains(SAML2Config.SP_ENTITY_ID));

        assertTrue(soapSTSfileContent.contains(SAML2Config.SP_ACS_URL));
        assertTrue(restSTSfileContent.contains(SAML2Config.SP_ACS_URL));

        assertTrue(soapSTSfileContent.contains(SAML2Config.SP_ENTITY_ID));
        assertTrue(restSTSfileContent.contains(SAML2Config.SP_ENTITY_ID));

        assertTrue(soapSTSfileContent.contains(SAML2Config.ENCRYPTION_KEY_ALIAS));
        assertTrue(restSTSfileContent.contains(SAML2Config.ENCRYPTION_KEY_ALIAS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.SIGNATURE_KEY_ALIAS));
        assertTrue(restSTSfileContent.contains(SAML2Config.SIGNATURE_KEY_ALIAS));

        assertTrue(soapSTSfileContent.contains(SAML2Config.SIGNATURE_KEY_PASSWORD));
        assertTrue(restSTSfileContent.contains(SAML2Config.SIGNATURE_KEY_PASSWORD));

        assertTrue(soapSTSfileContent.contains(DeploymentConfig.REALM));
        assertTrue(restSTSfileContent.contains(DeploymentConfig.REALM));

        assertTrue(soapSTSfileContent.contains(DeploymentConfig.URI_ELEMENT));
        assertTrue(restSTSfileContent.contains(DeploymentConfig.URI_ELEMENT));

        assertTrue(soapSTSfileContent.contains(DeploymentConfig.AUTH_TARGET_MAPPINGS));
        assertTrue(restSTSfileContent.contains(DeploymentConfig.AUTH_TARGET_MAPPINGS));

        assertTrue(soapSTSfileContent.contains(DeploymentConfig.OFFLOADED_TWO_WAY_TLS_HEADER_KEY));
        assertTrue(restSTSfileContent.contains(DeploymentConfig.OFFLOADED_TWO_WAY_TLS_HEADER_KEY));

        assertTrue(soapSTSfileContent.contains(DeploymentConfig.TLS_OFFLOAD_ENGINE_HOSTS));
        assertTrue(restSTSfileContent.contains(DeploymentConfig.TLS_OFFLOAD_ENGINE_HOSTS));

        assertTrue(soapSTSfileContent.contains(AuthTargetMapping.AUTH_TARGET_MAPPINGS));
        assertTrue(restSTSfileContent.contains(AuthTargetMapping.AUTH_TARGET_MAPPINGS));
    }

    private STSInstanceConfig buildConfig() throws UnsupportedEncodingException {
        return STSInstanceConfig.builder()
                .issuerName(ISSUER)
                .build();
    }

    private STSInstanceConfig buildConfigWithSaml2Config() throws UnsupportedEncodingException {
        SAML2Config saml2Config =
                SAML2Config.builder()
                .nameIdFormat("transient")
                .tokenLifetimeInSeconds(500000)
                .keystoreFile("/usr/local/dillrod/keystore")
                .keystorePassword("super_secret".getBytes())
                .spEntityId("http://host.com/saml/entity/id")
                .build();

        return STSInstanceConfig.builder()
                .issuerName(ISSUER)
                .saml2Config(saml2Config)
                .build();
    }
}