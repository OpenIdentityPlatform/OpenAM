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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.testng.Assert.assertTrue;


public class OpenIdConnectConfigTest {
    private Map<String, Set<String>> configState;
    private static final String PRINCIPAL_MAPPER  = "org.forgerock.openam.authentication.modules.oidc.DefaultPrincipalMapper";

    @BeforeTest
    void initalize() {
        configState = new HashMap<String, Set<String>>();
        configState.put(OpenIdConnectConfig.HEADER_NAME_KEY, setOf("header_name"));
        configState.put(OpenIdConnectConfig.ISSUER_NAME_KEY, setOf("accounts.google.com"));
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY, setOf(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL));
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY, setOf("https://accounts.google.com/.well-known/openid-configuration"));
        configState.put(OpenIdConnectConfig.PRINCIPAL_MAPPER_CLASS_KEY, setOf(PRINCIPAL_MAPPER));
        configState.put(OpenIdConnectConfig.LOCAL_TO_JWK_ATTRIBUTE_MAPPINGS_KEY, setOf("id=sub"));
    }

    private Set<String> setOf(String value) {
        Set<String> set = new HashSet<String>();
        set.add(value);
        return set;
    }

    @Test
    public void testAttributeConversion() {
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertTrue("sub".equals(config.getLocalToJwkAttributeMappings().get("id")));
    }

    @Test
    public void testDuplicateMappingEntries() {
        configState.get(OpenIdConnectConfig.LOCAL_TO_JWK_ATTRIBUTE_MAPPINGS_KEY).add("id=bobo");
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertTrue(config.getLocalToJwkAttributeMappings().size() == 1);
        assertTrue("sub".equals(config.getLocalToJwkAttributeMappings().get("id")));
    }

    @Test
    public void testBasicAttributeLookup() {
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertTrue(PRINCIPAL_MAPPER.equals(config.getPrincipalMapperClass()));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidCryptoContext() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY);
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY, setOf("bogus_type"));
        new OpenIdConnectConfig(configState);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidUrl() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY);
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY, setOf("bogus_url"));
        new OpenIdConnectConfig(configState);
    }
}

