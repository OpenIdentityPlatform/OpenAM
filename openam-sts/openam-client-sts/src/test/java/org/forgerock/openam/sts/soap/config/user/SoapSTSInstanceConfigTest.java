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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class SoapSTSInstanceConfigTest {
    private static final boolean WITH_SAML2_CONFIG = true;
    private static final boolean WITH_OIDC_CONFIG = true;
    private static final boolean WITH_KEYSTORE_CONFIG = true;
    private static final boolean WITH_VALIDATE_CONFIG = true;
    private static final boolean DELEGATION_VALIDATORS_SPECIFIED = true;
    private static final boolean CUSTOM_DELEGATION_HANDLER = true;

    @Test
    public void testEquals() throws UnsupportedEncodingException {
        SoapSTSInstanceConfig ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        SoapSTSInstanceConfig ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", !WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG,
                !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam", !WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG,
                !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", !WITH_KEYSTORE_CONFIG, !WITH_VALIDATE_CONFIG,
                DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, !WITH_SAML2_CONFIG,
                WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam", !WITH_KEYSTORE_CONFIG, !WITH_VALIDATE_CONFIG,
                DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG,
                !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG,
                !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", !WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG,
                DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam", !WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG,
                DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG, !WITH_VALIDATE_CONFIG,
                DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG, !WITH_VALIDATE_CONFIG,
                DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {
        SoapSTSInstanceConfig ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        SoapSTSInstanceConfig ric2 = createInstanceConfig("/bobo", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/", !WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/", !WITH_KEYSTORE_CONFIG,
                !WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/", !WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", "http://localhost:8080/", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRejectIfNull() throws UnsupportedEncodingException {
        createIncompleteInstanceConfig();
    }

    @Test
    public void testJsonRoundTrip() throws UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am",
                WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER,
                WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.fromJson(instanceConfig.toJson()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", !WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER,
                WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.fromJson(instanceConfig.toJson()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER,
                WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.fromJson(instanceConfig.toJson()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", !WITH_KEYSTORE_CONFIG,
                !WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER,
                !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.fromJson(instanceConfig.toJson()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER,
                WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.fromJson(instanceConfig.toJson()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", !WITH_KEYSTORE_CONFIG,
                !WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER,
                WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.fromJson(instanceConfig.toJson()));
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testNoValidationAndNoDelegation() throws UnsupportedEncodingException {
        createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG, !WITH_VALIDATE_CONFIG,
                !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);

    }

    @Test
    public void testMapMarshalRoundTrip() throws UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am",
                WITH_KEYSTORE_CONFIG, WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED,
                !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.marshalFromAttributeMap(instanceConfig.marshalToAttributeMap()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", !WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.marshalFromAttributeMap(instanceConfig.marshalToAttributeMap()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.marshalFromAttributeMap(instanceConfig.marshalToAttributeMap()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                !WITH_VALIDATE_CONFIG, !DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.marshalFromAttributeMap(instanceConfig.marshalToAttributeMap()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.marshalFromAttributeMap(instanceConfig.marshalToAttributeMap()));

        instanceConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                !WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(instanceConfig, SoapSTSInstanceConfig.marshalFromAttributeMap(instanceConfig.marshalToAttributeMap()));
    }

    @Test
    public void testOldJacksonJsonStringMarhalling() throws IOException {
        SoapSTSInstanceConfig origConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details. Covering old and new Jackson versions for completeness.
         */
        org.codehaus.jackson.JsonParser parser =
                new org.codehaus.jackson.map.ObjectMapper().getJsonFactory().createJsonParser(origConfig.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        assertEquals(origConfig, SoapSTSInstanceConfig.fromJson(new JsonValue(content)));
    }

    @Test
    public void testJsonStringMarshalling() throws IOException {
        SoapSTSInstanceConfig origConfig = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);

        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details. Covering old and new Jackson versions for completeness.
         */
        JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(origConfig.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        assertEquals(origConfig, SoapSTSInstanceConfig.fromJson(new JsonValue(content)));
    }

    @Test
    public void testJsonMapMarshalRoundTrip() throws IOException {
        SoapSTSInstanceConfig config = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, !DELEGATION_VALIDATORS_SPECIFIED, !CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        Map<String, Set<String>> attributeMap = config.marshalToAttributeMap();
        JsonValue jsonMap = new JsonValue(attributeMap);
        assertEquals(config, SoapSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));

        config = createInstanceConfig("/bobo/instance1", "http://host.com:8080/am", !WITH_KEYSTORE_CONFIG,
                WITH_VALIDATE_CONFIG, DELEGATION_VALIDATORS_SPECIFIED, CUSTOM_DELEGATION_HANDLER, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        assertEquals(config, SoapSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));
    }

    /*
    Because SoapSTSInstanceConfig and encapsulated instances must be marshaled to a Map<String, Set<String>> for SMS persistence,
    SoapSTSInstanceConfig and encapsulated classes define statics that define the keys which must correspond to the String
    keys in the SMS-persisted-map, values which must correspond to the entries defined in soapSTS.xml. This unit test
    tests this correspondence.
     */
    @Test
    public void testServicePropertyFileCorrespondence() throws IOException {
        String fileContent =
                IOUtils.getFileContent("../../openam-server-only/src/main/resources/services/soapSTS.xml");
        assertTrue(fileContent.contains(SoapSTSKeystoreConfig.KEYSTORE_FILENAME));
        assertTrue(fileContent.contains(SoapSTSKeystoreConfig.KEYSTORE_PASSWORD));
        assertTrue(fileContent.contains(SoapSTSKeystoreConfig.SIGNATURE_KEY_ALIAS));
        assertTrue(fileContent.contains(SoapSTSKeystoreConfig.ENCRYPTION_KEY_ALIAS));
        assertTrue(fileContent.contains(SoapSTSKeystoreConfig.SIGNATURE_KEY_PASSWORD));
        assertTrue(fileContent.contains(SoapSTSKeystoreConfig.ENCRYPTION_KEY_ALIAS));
        assertTrue(fileContent.contains(SoapSTSKeystoreConfig.ENCRYPTION_KEY_PASSWORD));

        assertTrue(fileContent.contains(SoapDeploymentConfig.SERVICE_QNAME));
        assertTrue(fileContent.contains(SoapDeploymentConfig.SERVICE_PORT));
        assertTrue(fileContent.contains(SoapDeploymentConfig.WSDL_LOCATION));
        assertTrue(fileContent.contains(SoapDeploymentConfig.AM_DEPLOYMENT_URL));
        assertTrue(fileContent.contains(SoapDeploymentConfig.CUSTOM_PORT_QNAME));
        assertTrue(fileContent.contains(SoapDeploymentConfig.CUSTOM_SERVICE_QNAME));
        assertTrue(fileContent.contains(SoapDeploymentConfig.CUSTOM_WSDL_LOCATION));
        assertTrue(fileContent.contains(SoapDeploymentConfig.CUSTOM_SOAP_STS_SERVICE_NAME_INDICATOR));
        assertTrue(fileContent.contains(SoapDeploymentConfig.CUSTOM_SOAP_STS_SERVICE_PORT_INDICATOR));
        assertTrue(fileContent.contains(SoapDeploymentConfig.CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR));

        assertTrue(fileContent.contains(SoapDelegationConfig.DELEGATION_TOKEN_VALIDATORS));
        assertTrue(fileContent.contains(SoapDelegationConfig.CUSTOM_DELEGATION_TOKEN_HANDLERS));

        assertTrue(fileContent.contains(SoapSTSInstanceConfig.ISSUE_TOKEN_TYPES));
        assertTrue(fileContent.contains(SoapSTSInstanceConfig.VALIDATED_TOKEN_CONFIG));
        assertTrue(fileContent.contains(SoapSTSInstanceConfig.DELEGATION_RELATIONSHIP_SUPPORTED));

        assertTrue(fileContent.contains(AMSTSConstants.UT_ASYMMETRIC_STS_SERVICE.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.UT_SYMMETRIC_STS_SERVICE.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.UT_TRANSPORT_STS_SERVICE.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.UT_ASYMMETRIC_STS_SERVICE_PORT.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.UT_SYMMETRIC_STS_SERVICE_PORT.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.UT_TRANSPORT_STS_SERVICE_PORT.toString()));

        assertTrue(fileContent.contains(AMSTSConstants.X509_ASYMMETRIC_STS_SERVICE.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.X509_SYMMETRIC_STS_SERVICE.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.X509_ASYMMETRIC_STS_SERVICE_PORT.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.X509_SYMMETRIC_STS_SERVICE_PORT.toString()));

        assertTrue(fileContent.contains(AMSTSConstants.AM_TRANSPORT_STS_SERVICE.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.AM_TRANSPORT_STS_SERVICE_PORT.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.AM_BARE_STS_SERVICE.toString()));
        assertTrue(fileContent.contains(AMSTSConstants.AM_BARE_STS_SERVICE_PORT.toString()));
    }

    private SoapSTSInstanceConfig createInstanceConfig(String uriElement, String amDeploymentUrl,
                                                       boolean withKeystoreConfig,
                                                       boolean withValidationConfig,
                                                       boolean delegationValidatorsSpecified,
                                                       boolean customDelegationHandler,
                                                       boolean withSAML2Config,
                                                       boolean withOIDCConfig) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldap")
                .build();

        SoapDeploymentConfig deploymentConfig =
                SoapDeploymentConfig.builder()
                        .portQName(AMSTSConstants.AM_TRANSPORT_STS_SERVICE_PORT)
                        .serviceQName(AMSTSConstants.AM_TRANSPORT_STS_SERVICE)
                        .wsdlLocation("wsdl_loc")
                        .realm("realm")
                        .amDeploymentUrl(amDeploymentUrl)
                        .uriElement(uriElement)
                        .authTargetMapping(mapping)
                        .build();

        SoapSTSKeystoreConfig keystoreConfig = null;
        if (withKeystoreConfig) {
            keystoreConfig =
                    SoapSTSKeystoreConfig.builder()
                    .keystoreFileName("stsstore.jks")
                    .keystorePassword("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .encryptionKeyAlias("mystskey")
                    .signatureKeyAlias("mystskey")
                    .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .build();
        }

        SoapSTSInstanceConfig.SoapSTSInstanceConfigBuilderBase<?> builder = SoapSTSInstanceConfig.builder();
        if (withValidationConfig) {
            builder.addSecurityPolicyTokenValidationConfiguration(TokenType.OPENAM, false);
            builder.addSecurityPolicyTokenValidationConfiguration(TokenType.USERNAME, true);
            builder.addSecurityPolicyTokenValidationConfiguration(TokenType.X509, true);
        }
        Map<String,String> attributeMap = new HashMap<>();
        attributeMap.put("mail", "email");
        attributeMap.put("uid", "id");
        SAML2Config saml2Config = null;
        if (withSAML2Config) {
            builder.addIssueTokenType(TokenType.SAML2);
            saml2Config = buildSAML2Config(attributeMap);
        }
        OpenIdConnectTokenConfig openIdConnectTokenConfig = null;
        if (withOIDCConfig) {
            builder.addIssueTokenType(TokenType.OPENIDCONNECT);
            openIdConnectTokenConfig = buildOIDCConfig(attributeMap);
        }

        boolean delegationRelationshipsSupported = customDelegationHandler || delegationValidatorsSpecified;
        if (delegationRelationshipsSupported) {
            SoapDelegationConfig.SoapDelegationConfigBuilder delegationConfigBuilder = SoapDelegationConfig.builder();
            if (delegationValidatorsSpecified) {
                delegationConfigBuilder
                        .addValidatedDelegationTokenType(TokenType.USERNAME, true)
                        .addValidatedDelegationTokenType(TokenType.OPENAM, false);
            }
            if (customDelegationHandler) {
                delegationConfigBuilder.addCustomDelegationTokenHandler("com.org.TokenDelegationHandlerImpl");
            }
            builder.soapDelegationConfig(delegationConfigBuilder.build());
        }
        return  builder
                .deploymentConfig(deploymentConfig)
                .soapSTSKeystoreConfig(keystoreConfig)
                .saml2Config(saml2Config)
                .oidcIdTokenConfig(openIdConnectTokenConfig)
                .delegationRelationshipsSupported(delegationRelationshipsSupported)
                .build();
    }

    private SoapSTSInstanceConfig createIncompleteInstanceConfig() throws UnsupportedEncodingException {
        //leave out the AuthTargetMapping and SAML2Config

        SoapDeploymentConfig deploymentConfig =
                SoapDeploymentConfig.builder()
                        .uriElement("whatever")
                        .amDeploymentUrl("whatever")
                        .build();

        SoapSTSKeystoreConfig keystoreConfig =
                SoapSTSKeystoreConfig.builder()
                        .keystoreFileName("stsstore.jks")
                        .keystorePassword("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("mystskey")
                        .signatureKeyAlias("mystskey")
                        .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .build();

        return SoapSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .soapSTSKeystoreConfig(keystoreConfig)
                .build();
    }

    private SAML2Config buildSAML2Config(Map<String, String> attributeMap) {
        return SAML2Config.builder()
                .nameIdFormat("transient")
                .tokenLifetimeInSeconds(500000)
                .spEntityId("http://host.com/saml2/sp/entity/id")
                .encryptAssertion(true)
                .signAssertion(true)
                .encryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#aes128-cbc")
                .encryptionKeyAlias("test")
                .signatureKeyAlias("test")
                .signatureKeyPassword("super.secret".getBytes())
                .encryptionAlgorithmStrength(128)
                .keystoreFile("da/directory/file")
                .keystorePassword("super.secret".getBytes())
                .attributeMap(attributeMap)
                .idpId("da_idp")
                .build();

    }

    private OpenIdConnectTokenConfig buildOIDCConfig(Map<String, String> claimMap) throws UnsupportedEncodingException {
        return OpenIdConnectTokenConfig.builder()
                .keystoreLocation("keystore.jks")
                .keystorePassword("bobo".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("test")
                .signatureKeyPassword("bobo".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureAlgorithm("RS256")
                .addAudience("oidcTokenAudience")
                .tokenLifetimeInSeconds(900)
                .claimMap(claimMap)
                .issuer("oidcTokenIssuer")
                .build();

    }
}
