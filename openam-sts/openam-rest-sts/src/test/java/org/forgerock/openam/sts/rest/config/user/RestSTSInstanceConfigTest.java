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

package org.forgerock.openam.sts.rest.config.user;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AuthTargetMapping;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 */
public class RestSTSInstanceConfigTest {

    @Test
    public void testEquals() throws UnsupportedEncodingException {
        RestSTSInstanceConfig ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        RestSTSInstanceConfig ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        assertTrue(ric1.equals(ric2));
        assertTrue(ric1.hashCode() == ric2.hashCode());
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {
        RestSTSInstanceConfig ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        RestSTSInstanceConfig ric2 = createInstanceConfig("/bobo", "http://localhost:8080/openam");
        assertFalse(ric1.equals(ric2));
        assertFalse(ric1.hashCode() == ric2.hashCode());

        RestSTSInstanceConfig ric3 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        RestSTSInstanceConfig ric4 = createInstanceConfig("/bob", "http://localhost:8080/");
        assertFalse(ric3.equals(ric4));
        assertFalse(ric3.hashCode() == ric4.hashCode());

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRejectIfNull() throws UnsupportedEncodingException {
        createIncompleteInstanceConfig();
    }

    @Test
    public void testJsonMarshalling() throws IOException {
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", "http://localhost:8080/openam");
        assertTrue(origConfig.equals(RestSTSInstanceConfig.fromJson(origConfig.toJson())));
    }

    @Test
    public void testJsonStringMarhalling() throws IOException {
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", "http://localhost:8080/openam");
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details. This is using the older version of jackson
        (org.codehaus.jackson.map.ObjectMapper), and I will do the same (albeit with the newer version), to reproduce
        the same behavior.
         */
        JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(origConfig.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        Assert.assertTrue(origConfig.equals(RestSTSInstanceConfig.fromJson(new JsonValue(content))));
    }

    @Test
    public void testOldJacksonJsonStringMarhalling() throws IOException {
        RestSTSInstanceConfig origConfig = createInstanceConfig("/bob", "http://localhost:8080/openam");
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details. This is using the older version of jackson
        (org.codehaus.jackson.map.ObjectMapper), and I will do the same, to reproduce
        the same behavior.
         */
        org.codehaus.jackson.JsonParser parser =
                new org.codehaus.jackson.map.ObjectMapper().getJsonFactory().createJsonParser(origConfig.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        Assert.assertTrue(origConfig.equals(RestSTSInstanceConfig.fromJson(new JsonValue(content))));
    }

    private RestSTSInstanceConfig createInstanceConfig(String uriElement, String amDeploymentUrl) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(UsernameToken.class, "service", "ldapService")
                .build();

        RestDeploymentConfig deploymentConfig =
                RestDeploymentConfig.builder()
                        .uriElement(uriElement)
                        .authTargetMapping(mapping)
                        .build();

        KeystoreConfig keystoreConfig =
                KeystoreConfig.builder()
                        .fileName("stsstore.jks")
                        .password("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("mystskey")
                        .signatureKeyAlias("mystskey")
                        .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .build();

        return RestSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .amDeploymentUrl(amDeploymentUrl)
                .amJsonRestBase("/json")
                .amRestAuthNUriElement("/authenticate")
                .amRestLogoutUriElement("/sessions/?_action=logout")
                .amRestIdFromSessionUriElement("/users/?_action=idFromSession")
                .amSessionCookieName("iPlanetDirectoryPro")
                .keystoreConfig(keystoreConfig)
                .issuerName("Cornholio")
                .addSupportedTokenTranslation(
                        TokenType.USERNAME,
                        TokenType.OPENAM,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
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

    private RestSTSInstanceConfig createIncompleteInstanceConfig() throws UnsupportedEncodingException {
        //leave out the DeploymentConfig to test null rejection
        AuthTargetMapping mapping = AuthTargetMapping.builder().build();

        KeystoreConfig keystoreConfig =
                KeystoreConfig.builder()
                        .fileName("stsstore.jks")
                        .password("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("mystskey")
                        .signatureKeyAlias("mystskey")
                        .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .build();

        return RestSTSInstanceConfig.builder()
                .amDeploymentUrl("whatever")
                .amRestAuthNUriElement("/json/authenticate")
                .amRestLogoutUriElement("/json/sessions/?_action=logout")
                .amRestIdFromSessionUriElement("/json/users/?_action=idFromSession")
                .amSessionCookieName("iPlanetDirectoryPro")
                .keystoreConfig(keystoreConfig)
                .issuerName("Cornholio")
                .addSupportedTokenTranslation(
                        TokenType.USERNAME,
                        TokenType.OPENAM,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
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

}
