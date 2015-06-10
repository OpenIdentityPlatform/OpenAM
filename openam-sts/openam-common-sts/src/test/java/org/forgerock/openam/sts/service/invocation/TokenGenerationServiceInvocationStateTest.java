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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.service.invocation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.forgerock.guava.common.collect.Sets;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Set;

public class TokenGenerationServiceInvocationStateTest {
    private static final String OIDC_AUTHN_CONTEXT_CLASS_REF = "whatever";
    private static final Set<String> AUTHN_MODE_REFS = Sets.newHashSet("ref1", "ref2");
    private static final long AUTHN_TIME = 23333333333L;
    private static final String NONCE = "ddfdfd";

    private static final String SAML2_AUTHN_CONTEXT_CLASS_REF = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    private final SAML2SubjectConfirmation SUBJECT_CONFIRMATION = SAML2SubjectConfirmation.BEARER;
    private static final String SSO_TOKEN_STRING = "abbssccsdfd";
    private static final String STS_INSTANCE_ID = "sts_instance_id";

    @Test
    public void testEquals() throws Exception {
        TokenGenerationServiceInvocationState config1 = buildInvocationState(TokenType.SAML2);
        TokenGenerationServiceInvocationState config2 = buildInvocationState(TokenType.SAML2);
        assertEquals(config1, config2);

        config1 = buildInvocationState(TokenType.OPENIDCONNECT);
        config2 = buildInvocationState(TokenType.OPENIDCONNECT);
        assertEquals(config1, config2);

        config1 = buildInvocationState(TokenType.SAML2);
        config2 = buildInvocationState(TokenType.OPENIDCONNECT);
        assertNotEquals(config1, config2);
    }

    @Test
    public void testJsonRoundTrip() throws Exception {
        TokenGenerationServiceInvocationState state = buildInvocationState(TokenType.SAML2);
        assertTrue(state.equals(TokenGenerationServiceInvocationState.fromJson(state.toJson())));

        state = buildInvocationState(TokenType.OPENIDCONNECT);
        assertTrue(state.equals(TokenGenerationServiceInvocationState.fromJson(state.toJson())));
    }

    @Test
    public void testFieldPersistenceAfterRoundTrip() throws Exception {
        TokenGenerationServiceInvocationState state = buildInvocationState(TokenType.OPENIDCONNECT);
        assertEquals(state, TokenGenerationServiceInvocationState.fromJson(state.toJson()));

        assertEquals(SSO_TOKEN_STRING, state.getSsoTokenString());
        assertEquals(TokenType.OPENIDCONNECT, state.getTokenType());
        assertEquals(STS_INSTANCE_ID, state.getStsInstanceId());
    }


    @Test
    public void testToString() throws Exception {
        /*
        build a few different types of instances, and call toString to insure no NPE ensues.
         */
        buildInvocationState(TokenType.SAML2).toString();
        buildInvocationState(TokenType.OPENIDCONNECT).toString();

    }
    private TokenGenerationServiceInvocationState buildInvocationState(TokenType tokenType) throws Exception {
        TokenGenerationServiceInvocationState.TokenGenerationServiceInvocationStateBuilder builder =
                TokenGenerationServiceInvocationState.builder();
        builder
                .tokenType(tokenType)
                .stsType(AMSTSConstants.STSType.REST)
                .stsInstanceId(STS_INSTANCE_ID)
                .ssoTokenString(SSO_TOKEN_STRING);
        if (TokenType.SAML2.equals(tokenType)) {
            builder.saml2GenerationState(buildSAML2TokenGenerationState());
        }
        if (TokenType.OPENIDCONNECT.equals(tokenType)) {
            builder.openIdConnectTokenGenerationState(buildOpenIdConnectTokenGenerationState());
        }
        return builder.build();
    }

    SAML2TokenGenerationState buildSAML2TokenGenerationState() throws IOException, CertificateException, TokenMarshalException {
        return SAML2TokenGenerationState.builder()
                .authenticationContextClassReference(SAML2_AUTHN_CONTEXT_CLASS_REF)
                .subjectConfirmation(SUBJECT_CONFIRMATION)
                .build();
    }

    OpenIdConnectTokenGenerationState buildOpenIdConnectTokenGenerationState() {
        return OpenIdConnectTokenGenerationState.builder()
                .authenticationTimeInSeconds(AUTHN_TIME)
                .authenticationContextClassReference(OIDC_AUTHN_CONTEXT_CLASS_REF)
                .authenticationMethodReferences(AUTHN_MODE_REFS)
                .nonce(NONCE)
                .build();
    }
}