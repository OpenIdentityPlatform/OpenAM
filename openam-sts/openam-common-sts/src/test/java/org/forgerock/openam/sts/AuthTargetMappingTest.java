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

package org.forgerock.openam.sts;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cxf.common.security.UsernameToken;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AuthTargetMapping;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.cert.X509Certificate;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class AuthTargetMappingTest {
    @Test
    public void testEquals() {
        AuthTargetMapping mapping1 = AuthTargetMapping
                .builder()
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        AuthTargetMapping mapping2 = AuthTargetMapping
                .builder()
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        assertTrue(mapping1.equals(mapping2));


        AuthTargetMapping mapping3 = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        AuthTargetMapping mapping4 = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        assertTrue(mapping3.equals(mapping4));

        AuthTargetMapping mapping5 = AuthTargetMapping
                .builder()
                .build();
        AuthTargetMapping mapping6 = AuthTargetMapping
                .builder()
                .build();
        assertTrue(mapping5.equals(mapping6));
    }

    @Test
    public void testEqualsNot() {
        AuthTargetMapping mapping1 = AuthTargetMapping
                .builder()
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        AuthTargetMapping mapping2 = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        assertFalse(mapping1.equals(mapping2));

        AuthTargetMapping mapping3 = AuthTargetMapping
                .builder()
                .build();
        AuthTargetMapping mapping4 = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        assertFalse(mapping3.equals(mapping4));
    }

    @Test
    public void testLookup() {
        AuthTargetMapping mapping1 = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .build();
        AuthTargetMapping.AuthTarget at1 = new AuthTargetMapping.AuthTarget(AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509");
        assertTrue(mapping1.getAuthTargetMapping(X509Certificate[].class).equals(at1));

        AuthTargetMapping mapping2 = AuthTargetMapping
                .builder()
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        AuthTargetMapping.AuthTarget at2 = new AuthTargetMapping.AuthTarget(AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username");
        assertTrue(mapping2.getAuthTargetMapping(UsernameToken.class).equals(at2));

        assertNull(mapping2.getAuthTargetMapping(String.class));

    }

    @Test
    public void testJsonRoundTrip() {
        AuthTargetMapping mapping = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        assertTrue(mapping.equals(AuthTargetMapping.fromJson(mapping.toJson())));
    }

    @Test
    public void testJsonStringRoundTrip() throws IOException {
        AuthTargetMapping mapping = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details. This is using the older version of jackson
        (org.codehaus.jackson.map.ObjectMapper), and I will do the same (albeit with the newer version), to reproduce
        the same behavior.
         */
        JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(mapping.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        assertTrue(mapping.equals(AuthTargetMapping.fromJson(new JsonValue(content))));
    }

    @Test
    public void testOlderJacksonJsonStringRoundTrip() throws IOException {
        AuthTargetMapping mapping = AuthTargetMapping
                .builder()
                .addMapping(X509Certificate[].class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                .addMapping(UsernameToken.class, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "username")
                .build();
        /*
        This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
        org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
        for details. This is using the older version of jackson
        (org.codehaus.jackson.map.ObjectMapper), and I will do the same, to reproduce
        the same behavior.
         */
        org.codehaus.jackson.JsonParser parser =
                new org.codehaus.jackson.map.ObjectMapper().getJsonFactory().createJsonParser(mapping.toJson().toString());
        final Object content = parser.readValueAs(Object.class);

        assertTrue(mapping.equals(AuthTargetMapping.fromJson(new JsonValue(content))));
    }

}
