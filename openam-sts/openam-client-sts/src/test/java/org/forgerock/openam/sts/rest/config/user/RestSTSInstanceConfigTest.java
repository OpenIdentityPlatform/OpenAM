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

package org.forgerock.openam.sts.rest.config.user;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class RestSTSInstanceConfigTest {
    private static final boolean WITH_TLS_OFFLOAD_CONFIG = true;
    private static final boolean WITH_SAML2_CONFIG = true;
    private static final boolean WITH_OIDC_CONFIG = true;
    private static final boolean WITH_CUSTOM_PROVIDER = true;
    private static final boolean WITH_CUSTOM_VALIDATOR = true;
    private static final boolean WITH_CTS_TOKEN_PERSISTENCE = true;
    private static final String TLS_OFFLOAD_HOST_IP = "16.34.22.23";
    private static final String TLS_CLIENT_CERT_HEADER = "client_cert";
    private static final String CUSTOM_TOKEN_NAME = "BOBO";
    private static final String CUSTOM_TOKEN_VALIDATOR = "org.forgerock.bobo.BoboValidator";
    private static final String CUSTOM_TOKEN_PROVIDER = "org.forgerock.bobo.BoboProvider";

    @Test
    public void testEquals() throws UnsupportedEncodingException {
        RestSTSInstanceConfig ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        RestSTSInstanceConfig ric2 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {
        RestSTSInstanceConfig ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        RestSTSInstanceConfig ric2 = createInstanceConfig("/bobo",WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        assertNotEquals(ric1, ric2);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRejectIfNull() throws UnsupportedEncodingException {
        createIncompleteInstanceConfig();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testIllegalStateExceptionNoSaml2Config() throws UnsupportedEncodingException {
        createInstanceConfigWithoutSaml2Config("/bob");
    }

    @Test
    public void testJsonMarshalling() throws IOException {
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));

        origConfig = createInstanceConfig("/bob", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));

        origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));

        origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));
    }

    @Test
    public void testJsonStringMarshalling() throws IOException {
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details.
         */
        JsonParser parser = new ObjectMapper().getFactory().createParser(origConfig.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(new JsonValue(content)));
    }

    @Test
    public void testMapMarshalRoundTrip() throws IOException {
        RestSTSInstanceConfig config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));

        config = createInstanceConfig("/bob", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));
    }

    @Test
    public void testJsonMapMarshalRoundTrip() throws IOException {
        RestSTSInstanceConfig config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        Map<String, Set<String>> attributeMap = config.marshalToAttributeMap();
        JsonValue jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));

        config = createInstanceConfig("/bob", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG,
                WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));
    }

    @Test
    public void testFieldPersistenceJsonMapMarshalRoundTrip() throws IOException {
        RestSTSInstanceConfig config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        Map<String, Set<String>> attributeMap = config.marshalToAttributeMap();
        JsonValue jsonMap = new JsonValue(attributeMap);
        assertEquals(config.persistIssuedTokensInCTS(), RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap).persistIssuedTokensInCTS());

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        System.out.println("After marshalling to attribute map: " + attributeMap);
        assertEquals(config.persistIssuedTokensInCTS(), RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap).persistIssuedTokensInCTS());

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, WITH_CTS_TOKEN_PERSISTENCE);
        assertEquals(config.persistIssuedTokensInCTS(), RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()).persistIssuedTokensInCTS());

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG,
                !WITH_CUSTOM_VALIDATOR, !WITH_CUSTOM_PROVIDER, !WITH_CTS_TOKEN_PERSISTENCE);
        System.out.println("After marshalling to attribute map: " + config.marshalToAttributeMap());
        assertEquals(config.persistIssuedTokensInCTS(), RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()).persistIssuedTokensInCTS());
    }

    private RestSTSInstanceConfig createInstanceConfig(String uriElement, boolean withTlsOffloadConfig,
                                                       boolean withSAML2Config, boolean withOIDCConfig,
                                                       boolean withCustomValidator, boolean withCustomProvider, boolean withCTSTokenPersistence)
                                                        throws UnsupportedEncodingException {
        Map<String, String> oidcContext = new HashMap<>();
        oidcContext.put("context_key_1", "context_value_1");
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldapService")
                .addMapping(TokenType.OPENIDCONNECT, "module", "oidc", oidcContext)
                .build();
        Set<String> offloadHosts = new HashSet<>(1);
        offloadHosts.add(TLS_OFFLOAD_HOST_IP);
        DeploymentConfig deploymentConfig;
        if (withTlsOffloadConfig) {
            deploymentConfig =
                    DeploymentConfig.builder()
                            .uriElement(uriElement)
                            .authTargetMapping(mapping)
                            .tlsOffloadEngineHostIpAddrs(offloadHosts)
                            .offloadedTwoWayTLSHeaderKey(TLS_CLIENT_CERT_HEADER)
                            .build();
        } else {
            deploymentConfig =
                    DeploymentConfig.builder()
                            .uriElement(uriElement)
                            .authTargetMapping(mapping)
                            .build();
        }

        RestSTSInstanceConfig.RestSTSInstanceConfigBuilder restSTSInstanceConfigBuilder = RestSTSInstanceConfig.builder();
        Map<String,String> attributeMap = new HashMap<>();
        attributeMap.put("mail", "email");
        attributeMap.put("uid", "id");
        SAML2Config saml2Config = null;
        if (withSAML2Config) {
            addOutputTokenTypeTranslationSuite(TokenType.SAML2, restSTSInstanceConfigBuilder);
            saml2Config =
                    SAML2Config.builder()
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
        OpenIdConnectTokenConfig openIdConnectTokenConfig = null;
        if (withOIDCConfig) {
            addOutputTokenTypeTranslationSuite(TokenType.OPENIDCONNECT, restSTSInstanceConfigBuilder);
            openIdConnectTokenConfig =
                    OpenIdConnectTokenConfig.builder()
                            .keystoreLocation("keystore.jks")
                            .keystorePassword("bobo".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .signatureKeyAlias("test")
                            .signatureKeyPassword("bobo".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .signatureAlgorithm("RS256")
                            .addAudience("oidcTokenAudience")
                            .tokenLifetimeInSeconds(900)
                            .issuer("oidcTokenIssuer")
                            .build();
        }
        if (withCustomValidator) {
            restSTSInstanceConfigBuilder.addCustomTokenValidator(CUSTOM_TOKEN_NAME, CUSTOM_TOKEN_VALIDATOR);
            restSTSInstanceConfigBuilder.addCustomTokenTransform(CUSTOM_TOKEN_NAME, "SAML2", true);
        }
        if (withCustomProvider) {
            restSTSInstanceConfigBuilder.addCustomTokenProvider(CUSTOM_TOKEN_NAME, CUSTOM_TOKEN_PROVIDER);
            restSTSInstanceConfigBuilder.addCustomTokenTransform("OPENAM", CUSTOM_TOKEN_NAME, true);

        }
        return restSTSInstanceConfigBuilder
                .deploymentConfig(deploymentConfig)
                .saml2Config(saml2Config)
                .oidcIdTokenConfig(openIdConnectTokenConfig)
                .persistIssuedTokensInCTS(withCTSTokenPersistence)
                .build();
    }

    private void addOutputTokenTypeTranslationSuite(TokenType outputTokenType, RestSTSInstanceConfig.RestSTSInstanceConfigBuilder builder) {
        builder
            .addSupportedTokenTransform(
                    TokenType.USERNAME,
                    outputTokenType,
                    AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
            .addSupportedTokenTransform(
                    TokenType.OPENAM,
                    outputTokenType,
                    !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
            .addSupportedTokenTransform(
                    TokenType.OPENIDCONNECT,
                    outputTokenType,
                    AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
    }

    private RestSTSInstanceConfig createIncompleteInstanceConfig() throws UnsupportedEncodingException {
        //leave out the DeploymentConfig to test null rejection

        return RestSTSInstanceConfig.builder()
                .addSupportedTokenTransform(
                        TokenType.USERNAME,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTransform(
                        TokenType.OPENAM,
                        TokenType.SAML2,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .build();
    }

    /*
    Create RestSTSInstanceConfig with SAML2 output tokens, but without SAML2Config, to test IllegalStateException
     */
    private RestSTSInstanceConfig createInstanceConfigWithoutSaml2Config(String uriElement) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldapService")
                .build();

        DeploymentConfig deploymentConfig =
                DeploymentConfig.builder()
                        .uriElement(uriElement)
                        .authTargetMapping(mapping)
                        .build();

        return RestSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .addSupportedTokenTransform(
                        TokenType.USERNAME,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTransform(
                        TokenType.OPENAM,
                        TokenType.SAML2,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTransform(
                        TokenType.OPENIDCONNECT,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTransform(
                        TokenType.X509,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION
                )
                .build();
    }
}
