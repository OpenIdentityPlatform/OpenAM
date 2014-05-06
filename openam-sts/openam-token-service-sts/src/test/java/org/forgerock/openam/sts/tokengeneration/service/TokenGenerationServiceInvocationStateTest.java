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

package org.forgerock.openam.sts.tokengeneration.service;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.forgerock.openam.sts.TokenType;
import org.testng.annotations.Test;

public class TokenGenerationServiceInvocationStateTest {
    private static final boolean WITH_SPECIOUS_CONFIG = true;
    private static final String SSO_TOKEN_STRING = "abbssccsdfd";
    private static final String STS_INSTANCE_ID = "sts_instance_id";
    private static final String SP_ACS_URL = "http://host.com:8080/openam/Consumer/metaAlias/s";
    private static final TokenGenerationServiceInvocationState.SAML2SubjectConfirmation SAML_2_SUBJECT_CONFIRMATION =
            TokenGenerationServiceInvocationState.SAML2SubjectConfirmation.HOLDER_OF_KEY;
    private static final TokenType TOKEN_TYPE = TokenType.SAML2;

    @Test
    public void testEquals() {
        TokenGenerationServiceInvocationState config1 = buildInvocationState(!WITH_SPECIOUS_CONFIG);
        TokenGenerationServiceInvocationState config2 = buildInvocationState(!WITH_SPECIOUS_CONFIG);
        assertTrue(config1.equals(config2));

        config1 = buildInvocationState(WITH_SPECIOUS_CONFIG);
        config2 = buildInvocationState(WITH_SPECIOUS_CONFIG);
        assertTrue(config1.equals(config2));

        config1 = buildInvocationState(WITH_SPECIOUS_CONFIG);
        config2 = buildInvocationState(!WITH_SPECIOUS_CONFIG);
        assertFalse(config1.equals(config2));
    }

    @Test
    public void testJsonRoundTrip1() {
        TokenGenerationServiceInvocationState state = buildInvocationState(WITH_SPECIOUS_CONFIG);
        assertTrue(state.equals(TokenGenerationServiceInvocationState.fromJson(state.toJson())));
    }

    @Test
    public void testJsonRoundTrip2() {
        TokenGenerationServiceInvocationState state = buildInvocationState(!WITH_SPECIOUS_CONFIG);
        assertTrue(state.equals(TokenGenerationServiceInvocationState.fromJson(state.toJson())));
    }

    @Test
    public void testFieldPersistenceAfterRoundTrip() {
        TokenGenerationServiceInvocationState state = buildInvocationState(!WITH_SPECIOUS_CONFIG);
        assertTrue(state.equals(TokenGenerationServiceInvocationState.fromJson(state.toJson())));

        assertTrue(SAML_2_SUBJECT_CONFIRMATION.equals(state.getSaml2SubjectConfirmation()));
        assertTrue(SSO_TOKEN_STRING.equals(state.getSsoTokenString()));
        assertTrue(TOKEN_TYPE.equals(state.getTokenType()));
        assertTrue(STS_INSTANCE_ID.equals(state.getStsInstanceId()));
        assertTrue(SP_ACS_URL.equals(state.getSpAcsUrl()));
    }

    @Test (expectedExceptions=IllegalStateException.class)
    public void testInvocationStateValidation() {
        buildFaultyState();
    }

    private TokenGenerationServiceInvocationState buildInvocationState(boolean withSpeciousConfig) {
        if (!withSpeciousConfig) {
            return TokenGenerationServiceInvocationState.builder()
                    .stsInstanceId(STS_INSTANCE_ID)
                    .saml2SubjectConfirmation(SAML_2_SUBJECT_CONFIRMATION)
                    .ssoTokenString(SSO_TOKEN_STRING)
                    .tokenType(TOKEN_TYPE)
                    .serviceProviderAssertionConsumerServiceUrl(SP_ACS_URL)
                    .build();
        } else {
            //don't set spAcsUrl, to test null equals and marshalling
            return TokenGenerationServiceInvocationState.builder()
                    .stsInstanceId("specious instance id")
                    .saml2SubjectConfirmation(SAML_2_SUBJECT_CONFIRMATION)
                    .ssoTokenString(SSO_TOKEN_STRING)
                    .tokenType(TOKEN_TYPE)
                    .build();
        }
    }

    /*
    Build with BEARER, but without spAcsUrl, to generate IllegalStateException.
     */
    private TokenGenerationServiceInvocationState buildFaultyState() {
            //don't set spAcsUrl, to test null equals and marshalling
            return TokenGenerationServiceInvocationState.builder()
                    .stsInstanceId("specious instance id")
                    .saml2SubjectConfirmation(TokenGenerationServiceInvocationState.SAML2SubjectConfirmation.BEARER)
                    .ssoTokenString(SSO_TOKEN_STRING)
                    .tokenType(TOKEN_TYPE)
                    .build();
    }

}