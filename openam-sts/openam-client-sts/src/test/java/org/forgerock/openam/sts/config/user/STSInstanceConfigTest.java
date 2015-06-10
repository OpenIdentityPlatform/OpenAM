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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.config.user;

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class STSInstanceConfigTest {
    private static final String ISSUER = "cornholio";
    private static final long TOKEN_LIFETIME = 7000;
    private static final String RELYING_PARTY = "da_audience";
    private static final String KEYSTORE_LOCATION = "keystore.jks";
    private static final byte[] KEYSTORE_PASSWORD = "super_secret".getBytes();
    private static final String SIGNATURE_KEY_ALIAS = "sign_alias";
    private static final byte[] SIGNATURE_KEY_PASSWORD = "super_secret2".getBytes();
    private static final JwsAlgorithm JWS_ALGORITHM = JwsAlgorithm.RS256;
    private static final boolean WITH_SAML2_CONFIG = true;
    private static final boolean WITH_OIDC_CONFIG = true;

    @Test
    public void testSettings() throws UnsupportedEncodingException {
        STSInstanceConfig instance = buildConfig(WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertTrue(ISSUER.equals(instance.getSaml2Config().getIdpId()));
    }

    @Test
    public void testEquals() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfig(WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        STSInstanceConfig instance2 = buildConfig(WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instance1, instance2);

        instance1 = buildConfig(!WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        instance2 = buildConfig(!WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instance1, instance2);

        instance1 = buildConfig(WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        instance2 = buildConfig(WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(instance1, instance2);
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfig(WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        STSInstanceConfig instance2 = buildConfig(WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(instance1, instance2);

        instance1 = buildConfig(!WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        instance2 = buildConfig(!WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(instance1, instance2);

        instance1 = buildConfig(!WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        instance2 = buildConfig(WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(instance1, instance2);
    }

    @Test
    public void testJsonRoundTrip() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfig(WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instance1, STSInstanceConfig.fromJson(instance1.toJson()));

        instance1 = buildConfig(!WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instance1, STSInstanceConfig.fromJson(instance1.toJson()));

        instance1 = buildConfig(WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(instance1, STSInstanceConfig.fromJson(instance1.toJson()));
    }

    @Test
    public void testFieldPersistence() throws UnsupportedEncodingException {
        STSInstanceConfig instanceConfig =
                STSInstanceConfig.fromJson(buildConfig(WITH_SAML2_CONFIG, WITH_OIDC_CONFIG).toJson());
        SAML2Config saml2Config = instanceConfig.getSaml2Config();
        assertEquals(KEYSTORE_LOCATION, saml2Config.getKeystoreFileName());
        assertEquals(KEYSTORE_PASSWORD, saml2Config.getKeystorePassword());
        assertEquals(SIGNATURE_KEY_ALIAS, saml2Config.getSignatureKeyAlias());
        assertEquals(SIGNATURE_KEY_PASSWORD, saml2Config.getSignatureKeyPassword());
        assertEquals(RELYING_PARTY, saml2Config.getSpEntityId());

        OpenIdConnectTokenConfig openIdConnectTokenConfig = instanceConfig.getOpenIdConnectTokenConfig();
        assertEquals(KEYSTORE_LOCATION, openIdConnectTokenConfig.getKeystoreLocation());
        assertEquals(KEYSTORE_PASSWORD, openIdConnectTokenConfig.getKeystorePassword());
        assertEquals(SIGNATURE_KEY_ALIAS, openIdConnectTokenConfig.getSignatureKeyAlias());
        assertEquals(SIGNATURE_KEY_PASSWORD, openIdConnectTokenConfig.getSignatureKeyPassword());
        assertEquals(ISSUER, openIdConnectTokenConfig.getIssuer());
        assertTrue(openIdConnectTokenConfig.getAudience().contains(RELYING_PARTY));
    }


    @Test
    public void testMapMarshalRoundTrip() throws UnsupportedEncodingException {
        STSInstanceConfig instance1 = buildConfig(WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instance1, STSInstanceConfig.marshalFromAttributeMap(instance1.marshalToAttributeMap()));

        instance1 = buildConfig(!WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instance1, STSInstanceConfig.marshalFromAttributeMap(instance1.marshalToAttributeMap()));

        instance1 = buildConfig(WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
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
        assertTrue(restSTSfileContent.contains(SAML2Config.ISSUER_NAME));
        assertTrue(soapSTSfileContent.contains(SAML2Config.ISSUER_NAME));

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

        assertTrue(restSTSfileContent.contains(RestSTSInstanceConfig.SUPPORTED_TOKEN_TRANSLATIONS));

        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.ISSUER));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.CLAIM_MAP));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.TOKEN_LIFETIME));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.KEYSTORE_LOCATION));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.KEYSTORE_PASSWORD));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.SIGNATURE_KEY_ALIAS));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.SIGNATURE_KEY_PASSWORD));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.SIGNATURE_ALGORITHM));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.AUDIENCE));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.AUTHORIZED_PARTY));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.CUSTOM_CLAIM_MAPPER_CLASS));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.CUSTOM_AUTHN_CONTEXT_MAPPER_CLASS));
        assertTrue(restSTSfileContent.contains(OpenIdConnectTokenConfig.CUSTOM_AUTHN_METHOD_REFERENCES_MAPPER_CLASS));
    }


    private STSInstanceConfig buildConfig(boolean withSAM2Config, boolean withOIDCIdTokenConfig) throws UnsupportedEncodingException {
        SAML2Config saml2Config = null;
        if (withSAM2Config) {
            saml2Config = SAML2Config.builder()
                    .nameIdFormat("transient")
                    .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                    .keystoreFile(KEYSTORE_LOCATION)
                    .keystorePassword(KEYSTORE_PASSWORD)
                    .signatureKeyAlias(SIGNATURE_KEY_ALIAS)
                    .signatureKeyPassword(SIGNATURE_KEY_PASSWORD)
                    .spEntityId(RELYING_PARTY)
                    .idpId(ISSUER)
                    .build();
        }
        OpenIdConnectTokenConfig openIdConnectTokenConfig = null;
        if (withOIDCIdTokenConfig) {
            openIdConnectTokenConfig = OpenIdConnectTokenConfig.builder()
                    .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                    .issuer(ISSUER)
                    .addAudience(RELYING_PARTY)
                    .keystoreLocation(KEYSTORE_LOCATION)
                    .keystorePassword(KEYSTORE_PASSWORD)
                    .signatureAlgorithm(JWS_ALGORITHM)
                    .signatureKeyAlias(SIGNATURE_KEY_ALIAS)
                    .signatureKeyPassword(SIGNATURE_KEY_PASSWORD)
                    .build();
        }
        return STSInstanceConfig.builder()
                .saml2Config(saml2Config)
                .oidcIdTokenConfig(openIdConnectTokenConfig)
                .build();
    }
}