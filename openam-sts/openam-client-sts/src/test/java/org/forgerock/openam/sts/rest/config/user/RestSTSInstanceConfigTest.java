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
import org.forgerock.json.fluent.JsonValue;
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
    private static final String TLS_OFFLOAD_HOST_IP = "16.34.22.23";
    private static final String TLS_CLIENT_CERT_HEADER = "client_cert";

    @Test
    public void testEquals() throws UnsupportedEncodingException {
        RestSTSInstanceConfig ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        RestSTSInstanceConfig ric2 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());

        ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(ric1, ric2);
        assertEquals(ric1.hashCode(), ric2.hashCode());
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {
        RestSTSInstanceConfig ric1 = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        RestSTSInstanceConfig ric2 = createInstanceConfig("/bobo",WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertNotEquals(ric1, ric2);

        ric1 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        ric2 = createInstanceConfig("/bobo", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
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
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));

        origConfig = createInstanceConfig("/bob", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));

        origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));

        origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(origConfig.toJson()));
    }

    @Test
    public void testJsonStringMarshalling() throws IOException {
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details. This is using the older version of jackson
        (org.codehaus.jackson.map.ObjectMapper), and I will do the same (albeit with the newer version), to reproduce
        the same behavior.
         */
        JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(origConfig.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(new JsonValue(content)));
    }

    @Test
    public void testOldJacksonJsonStringMarshalling() throws IOException {
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details.
         */
        org.codehaus.jackson.JsonParser parser =
                new org.codehaus.jackson.map.ObjectMapper().getJsonFactory().createJsonParser(origConfig.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        assertEquals(origConfig, RestSTSInstanceConfig.fromJson(new JsonValue(content)));
    }

    @Test
    public void testMapMarshalRoundTrip() throws IOException {
        RestSTSInstanceConfig config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));

        config = createInstanceConfig("/bob", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        assertEquals(config, RestSTSInstanceConfig.marshalFromAttributeMap(config.marshalToAttributeMap()));
    }

    @Test
    public void testJsonMapMarshalRoundTrip() throws IOException {
        RestSTSInstanceConfig config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        Map<String, Set<String>> attributeMap = config.marshalToAttributeMap();
        JsonValue jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));

        config = createInstanceConfig("/bob", !WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, !WITH_SAML2_CONFIG, WITH_OIDC_CONFIG);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));

        config = createInstanceConfig("/bob", WITH_TLS_OFFLOAD_CONFIG, WITH_SAML2_CONFIG, !WITH_OIDC_CONFIG);
        attributeMap = config.marshalToAttributeMap();
        jsonMap = new JsonValue(attributeMap);
        assertEquals(config, RestSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMap));
    }

    private RestSTSInstanceConfig createInstanceConfig(String uriElement, boolean withTlsOffloadConfig,
                                                       boolean withSAML2Config, boolean withOIDCConfig)
                                                        throws UnsupportedEncodingException {
        Map<String,String> oidcContext = new HashMap<>();
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

        return restSTSInstanceConfigBuilder
                .deploymentConfig(deploymentConfig)
                .saml2Config(saml2Config)
                .oidcIdTokenConfig(openIdConnectTokenConfig)
                .build();
    }

    private void addOutputTokenTypeTranslationSuite(TokenType outputTokenType, RestSTSInstanceConfig.RestSTSInstanceConfigBuilder builder) {
        builder
            .addSupportedTokenTranslation(
                    TokenType.USERNAME,
                    outputTokenType,
                    AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
            .addSupportedTokenTranslation(
                    TokenType.OPENAM,
                    outputTokenType,
                    !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
            .addSupportedTokenTranslation(
                    TokenType.OPENIDCONNECT,
                    outputTokenType,
                    AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
    }

    private RestSTSInstanceConfig createIncompleteInstanceConfig() throws UnsupportedEncodingException {
        //leave out the DeploymentConfig to test null rejection

        return RestSTSInstanceConfig.builder()
                .addSupportedTokenTranslation(
                        TokenType.USERNAME,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
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
                .addSupportedTokenTranslation(
                        TokenType.USERNAME,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.OPENAM,
                        TokenType.SAML2,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.OPENIDCONNECT,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.X509,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION
                )
                .build();
    }
}
