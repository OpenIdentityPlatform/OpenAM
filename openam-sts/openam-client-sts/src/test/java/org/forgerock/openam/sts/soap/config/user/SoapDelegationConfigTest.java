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
 * Copyright 2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.openam.sts.TokenType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class SoapDelegationConfigTest {
    public static final boolean WITH_CUSTOM_DELEGATION_HANDLERS = true;
    public static final boolean WITH_DELEGATION_TOKEN_TYPES = true;
    public static final String CUSTOM_DELEGATION_HANDLER_CLASS_NAME = "org.company.DelegationHandlerImpl";

    @Test
    public void testEquals() {
        SoapDelegationConfig sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        SoapDelegationConfig sdc2 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, sdc2);
        assertEquals(sdc1.hashCode(), sdc2.hashCode());

        sdc1 = buildSoapDelegationConfig(!WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        sdc2 = buildSoapDelegationConfig(!WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, sdc2);
        assertEquals(sdc1.hashCode(), sdc2.hashCode());

        sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, !WITH_CUSTOM_DELEGATION_HANDLERS);
        sdc2 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, !WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, sdc2);
        assertEquals(sdc1.hashCode(), sdc2.hashCode());
    }

    @Test
    public void testNotEquals() {
        SoapDelegationConfig sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        SoapDelegationConfig sdc2 = buildSoapDelegationConfig(!WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertNotEquals(sdc1, sdc2);

        sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, !WITH_CUSTOM_DELEGATION_HANDLERS);
        sdc2 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertNotEquals(sdc1, sdc2);

        sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, !WITH_CUSTOM_DELEGATION_HANDLERS);
        sdc2 = buildSoapDelegationConfig(!WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertNotEquals(sdc1, sdc2);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testIllegalConstruction() {
        buildSoapDelegationConfig(!WITH_DELEGATION_TOKEN_TYPES, !WITH_CUSTOM_DELEGATION_HANDLERS);
    }

    @Test
    public void testJsonRoundTrip() {
        SoapDelegationConfig sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, SoapDelegationConfig.fromJson(sdc1.toJson()));
        assertTrue(SoapDelegationConfig.fromJson(sdc1.toJson()).getCustomDelegationTokenHandlers().contains(CUSTOM_DELEGATION_HANDLER_CLASS_NAME));
        assertTrue(SoapDelegationConfig.fromJson(sdc1.toJson()).getValidatedDelegatedTokenTypes().contains(TokenType.OPENAM));
        assertTrue(SoapDelegationConfig.fromJson(sdc1.toJson()).getValidatedDelegatedTokenTypes().contains(TokenType.USERNAME));

        sdc1 = buildSoapDelegationConfig(!WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, SoapDelegationConfig.fromJson(sdc1.toJson()));

        sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, !WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, SoapDelegationConfig.fromJson(sdc1.toJson()));
    }

    @Test
    public void testMapMarshalRoundTrip() {
        SoapDelegationConfig sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, SoapDelegationConfig.marshalFromAttributeMap(sdc1.marshalToAttributeMap()));

        sdc1 = buildSoapDelegationConfig(!WITH_DELEGATION_TOKEN_TYPES, WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, SoapDelegationConfig.marshalFromAttributeMap(sdc1.marshalToAttributeMap()));

        sdc1 = buildSoapDelegationConfig(WITH_DELEGATION_TOKEN_TYPES, !WITH_CUSTOM_DELEGATION_HANDLERS);
        assertEquals(sdc1, SoapDelegationConfig.marshalFromAttributeMap(sdc1.marshalToAttributeMap()));
    }

    private SoapDelegationConfig buildSoapDelegationConfig(boolean withDelegationTokenTypes, boolean withCustomDelegationHandlers) {
        SoapDelegationConfig.SoapDelegationConfigBuilder builder = SoapDelegationConfig.builder();
        if (withDelegationTokenTypes) {
            builder.addValidatedDelegationTokenType(TokenType.OPENAM).addValidatedDelegationTokenType(TokenType.USERNAME);
        }
        if (withCustomDelegationHandlers) {
            builder.addCustomDelegationTokenHandler(CUSTOM_DELEGATION_HANDLER_CLASS_NAME);
        }
        return builder.build();
    }

}
