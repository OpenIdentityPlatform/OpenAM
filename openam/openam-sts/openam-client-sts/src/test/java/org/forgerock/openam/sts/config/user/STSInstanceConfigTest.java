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

import org.testng.annotations.Test;

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