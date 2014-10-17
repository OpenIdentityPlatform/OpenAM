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

package com.sun.identity.workflow;

import com.sun.identity.shared.datastruct.CollectionHelper;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;
import static com.sun.identity.workflow.ParameterKeys.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static com.sun.identity.workflow.ConfigureSocialAuthN.*;
import static org.mockito.Mockito.*;
import static com.sun.identity.shared.datastruct.CollectionHelper.*;

public class ConfigureSocialAuthNTest {

    private static final String WELL_KNOWN_CONTENT = "{" +
            "\"" + WELL_KNOWN_AUTH_URL + "\":\"http://local.example.com/auth\"," +
            "\"" + WELL_KNOWN_PROFILE_URL + "\":\"http://local.example.com/profile\"," +
            "\"" + WELL_KNOWN_TOKEN_URL + "\":\"http://local.example.com/token\"," +
            "\"" + WELL_KNOWN_ISSUER + "\":\"local.example.com\"" +
            "}";
    private ConfigureSocialAuthN task = new ConfigureSocialAuthN();

    @Test
    public void testGetValidatedClientSecret() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_SECRET, asSet("fred"));
        params.put(P_CLIENT_SECRET_CONFIRM, asSet("fred"));
        assertEquals(task.getValidatedClientSecret(params), "fred");
    }

    @Test
    public void testGetValidatedClientSecretMissingSecret() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_SECRET_CONFIRM, asSet("freddy"));
        try {
            task.getValidatedClientSecret(params);
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "missing-clientSecret");
        }
    }

    @Test
    public void testGetValidatedClientSecretMissingSecretConfirm() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_SECRET, asSet("freddy"));
        try {
            task.getValidatedClientSecret(params);
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "missing-clientSecretConfirm");
        }
    }

    @Test
    public void testGetValidatedClientSecretMismatch() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_SECRET, asSet("fred"));
        params.put(P_CLIENT_SECRET_CONFIRM, asSet("freddy"));
        try {
            task.getValidatedClientSecret(params);
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "secrets-doesnt-match");
        }
    }

    @Test
    public void testGetValidatedRedirectUrl() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_REDIRECT_URL, asSet("http://example.com"));
        assertEquals(task.getValidatedRedirectUrl(params), "http://example.com");
    }

    @Test
    public void testGetValidatedRedirectUrlMissing() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        try {
            task.getValidatedRedirectUrl(params);
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "missing-redirectUrl");
        }
    }

    @Test
    public void testGetValidatedRedirectUrlInvalid() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_REDIRECT_URL, asSet("fred"));
        try {
            task.getValidatedRedirectUrl(params);
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "invalid-redirectUrl");
        }
    }

    @Test
    public void testGetValidatedFieldPresentOther() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put("field", asSet("fred"));
        assertEquals(task.getValidatedField("other", params, null, "field", null), "fred");
    }

    @Test
    public void testGetValidatedFieldMissingOther() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        try {
            task.getValidatedField("other", params, null, "field", "missing-image-url");
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "missing-image-url");
        }
    }

    @Test
    public void testGetValidatedFieldPresent() throws Exception {
        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
        attrs.put("field", asSet("fred"));
        assertEquals(task.getValidatedField("google", null, attrs, "field", null), "fred");
    }

    @Test
    public void testGetValidatedFieldMissing() throws Exception {
        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
        try {
            task.getValidatedField("google", null, attrs, "field", "missing-image-url");
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "missing-image-url");
        }
    }

    @Test
    public void testCollectAuthModuleAttributes() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_ID, asSet("fred"));
        params.put(P_CLIENT_SECRET, asSet("freddy"));
        params.put(P_CLIENT_SECRET_CONFIRM, asSet("freddy"));
        params.put(P_REDIRECT_URL, asSet("http://example.com"));
        Map<String, Set<String>> attrs = task.collectAuthModuleAttributes(Locale.ENGLISH, "microsoft", params);
        assertEquals(getMapAttr(attrs, "providerName"), "Microsoft");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CLIENT_ID), "fred");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CLIENT_SECRET), "freddy");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_TYPE), "client_secret");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_VALUE), "freddy");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_PROXY_URL), "http://example.com");
    }

    @Test
    public void testCollectAuthModuleAttributesUsingWellKnownConfig() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_ID, asSet("fred"));
        params.put(P_CLIENT_SECRET, asSet("freddy"));
        params.put(P_CLIENT_SECRET_CONFIRM, asSet("freddy"));
        params.put(P_REDIRECT_URL, asSet("http://example.com"));
        String wellKnownUrl = "http://local.example.com/.well-known/config";
        params.put(P_OPENID_DISCOVERY_URL, asSet(wellKnownUrl));
        ConfigureSocialAuthN task = spy(new ConfigureSocialAuthN());
        doReturn(WELL_KNOWN_CONTENT).when(task).getWebContent(Locale.ENGLISH, wellKnownUrl);
        Map<String, Set<String>> attrs = task.collectAuthModuleAttributes(Locale.ENGLISH, "microsoft", params);
        assertEquals(getMapAttr(attrs, "providerName"), "Microsoft");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CLIENT_ID), "fred");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CLIENT_SECRET), "freddy");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_TYPE), "client_secret");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_VALUE), "freddy");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_PROXY_URL), "http://example.com");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_AUTH_URL), "http://local.example.com/auth");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_TOKEN_URL), "http://local.example.com/token");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_USER_PROFILE_URL), "http://local.example.com/profile");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_ISSUER), "local.example.com");
    }

    @Test
    public void testCollectAuthModuleAttributesUsingWellKnownConfigWithEmptyJwkUrl() throws Exception {
        // Given
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_ID, asSet("fred"));
        params.put(P_CLIENT_SECRET, asSet("freddy"));
        params.put(P_CLIENT_SECRET_CONFIRM, asSet("freddy"));
        params.put(P_REDIRECT_URL, asSet("http://example.com"));
        String wellKnownUrl = "http://local.example.com/.well-known/config";
        params.put(P_OPENID_DISCOVERY_URL, asSet(wellKnownUrl));
        ConfigureSocialAuthN task = spy(new ConfigureSocialAuthN());
        String content = WELL_KNOWN_CONTENT.replaceAll("}", ",\""+ WELL_KNOWN_JWK +"\": \"\"}");
        doReturn(content).when(task).getWebContent(Locale.ENGLISH, wellKnownUrl);

        // When
        Map<String, Set<String>> attrs = task.collectAuthModuleAttributes(Locale.ENGLISH, "microsoft", params);

        // Then
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_TYPE), "client_secret");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_VALUE), "freddy");
    }

    @Test
    public void testCollectAuthModuleAttributesUsingWellKnownConfigWithJwkUrl() throws Exception {
        // Given
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        params.put(P_CLIENT_ID, asSet("fred"));
        params.put(P_CLIENT_SECRET, asSet("freddy"));
        params.put(P_CLIENT_SECRET_CONFIRM, asSet("freddy"));
        params.put(P_REDIRECT_URL, asSet("http://example.com"));
        String wellKnownUrl = "http://local.example.com/.well-known/config";
        params.put(P_OPENID_DISCOVERY_URL, asSet(wellKnownUrl));
        ConfigureSocialAuthN task = spy(new ConfigureSocialAuthN());
        String content = WELL_KNOWN_CONTENT.replaceAll("}", ",\""+ WELL_KNOWN_JWK +"\": \"a\"}");
        doReturn(content).when(task).getWebContent(Locale.ENGLISH, wellKnownUrl);

        // When
        Map<String, Set<String>> attrs = task.collectAuthModuleAttributes(Locale.ENGLISH, "microsoft", params);

        // Then
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_TYPE), "jwk_url");
        assertEquals(getMapAttr(attrs, AUTH_MODULE_CRYPTO_VALUE), "a");
    }

    @Test
    public void testCollectAuthModuleAttributesMissingClientId() throws Exception {
        Map<String, Set<String>> params = new HashMap<String, Set<String>>();
        try {
            task.collectAuthModuleAttributes(Locale.ENGLISH, "other", params);
            fail("Expect exception");
        } catch (WorkflowException e) {
            assertEquals(e.getErrorCode(), "missing-clientId");
        }
    }

    @Test
    public void testReadPropertiesFile() throws Exception {
        Map<String, Set<String>> properties = task.readPropertiesFile("test");
        assertEquals(getMapAttr(properties, "providerName"), "Test");
        assertTrue(properties.get("multivalue").contains("value1"));
        assertTrue(properties.get("multivalue").contains("value2"));
    }

    @Test
    public void testMergeProperties() throws Exception {
        Map<String, Set<String>> props1 = new HashMap<String, Set<String>>();
        props1.put("a", asSet("A"));
        props1.put("b", asSet("b"));
        Map<String, Set<String>> props2 = new HashMap<String, Set<String>>();
        props2.put("b", asSet("B"));
        props2.put("c", asSet("C"));
        Map<String, Set<String>> result = task.mergeAttributes(props1, props2);
        assertEquals(result.get("a"), asSet("A"));
        assertEquals(result.get("c"), asSet("C"));
        assertTrue(result.get("b").contains("b"));
        assertTrue(result.get("b").contains("B"));
    }

}