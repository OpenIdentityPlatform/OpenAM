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

package org.forgerock.openam.sts.service.invocation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.forgerock.openam.sts.TokenType;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;

public class TokenGenerationServiceInvocationStateTest {
    private static final String X_509 = "X.509";
    private static final boolean WITH_SPECIOUS_INSTANCE_ID = true;
    private static final boolean WITH_PROOF_TOKEN_STATE = true;
    private static final String SSO_TOKEN_STRING = "abbssccsdfd";
    private static final String STS_INSTANCE_ID = "sts_instance_id";
    private static final String AUTHN_CONTEXT_CLASS_REF = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    private static final TokenType TOKEN_TYPE = TokenType.SAML2;

    @Test
    public void testEquals() throws Exception {
        TokenGenerationServiceInvocationState config1 = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID,
                !WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.SENDER_VOUCHES);
        TokenGenerationServiceInvocationState config2 = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID,
                !WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.SENDER_VOUCHES);
        assertEquals(config1, config2);

        config1 = buildInvocationState(WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER);
        config2 = buildInvocationState(WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER);
        assertEquals(config1, config2);

        config1 = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID,
                !WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER);
        config2 = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID,
                !WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER);
        assertEquals(config1, config2);

        config1 = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER);
        config2 = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER);
        assertEquals(config1, config2);

        config1 = buildInvocationState(WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.HOLDER_OF_KEY);
        config2 = buildInvocationState(WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.HOLDER_OF_KEY);
        assertEquals(config1, config2);

        config1 = buildInvocationState(WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.HOLDER_OF_KEY);
        config2 = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.HOLDER_OF_KEY);
        assertNotEquals(config1, config2);
    }

    @Test
    public void testJsonRoundTrip1() throws Exception {
        TokenGenerationServiceInvocationState state = buildInvocationState(WITH_SPECIOUS_INSTANCE_ID,
                WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER);
        assertTrue(state.equals(TokenGenerationServiceInvocationState.fromJson(state.toJson())));
    }

    @Test
    public void testJsonRoundTrip2() throws Exception {
        TokenGenerationServiceInvocationState state = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID, !WITH_PROOF_TOKEN_STATE,
                SAML2SubjectConfirmation.BEARER);
        assertTrue(state.equals(TokenGenerationServiceInvocationState.fromJson(state.toJson())));
    }

    @Test
    public void testFieldPersistenceAfterRoundTrip() throws Exception {
        TokenGenerationServiceInvocationState state = buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID, WITH_PROOF_TOKEN_STATE,
                SAML2SubjectConfirmation.HOLDER_OF_KEY);
        assertEquals(state, TokenGenerationServiceInvocationState.fromJson(state.toJson()));

        assertEquals(SAML2SubjectConfirmation.HOLDER_OF_KEY,
                state.getSaml2SubjectConfirmation());
        assertEquals(SSO_TOKEN_STRING, state.getSsoTokenString());
        assertEquals(TOKEN_TYPE, state.getTokenType());
        assertEquals(STS_INSTANCE_ID, state.getStsInstanceId());
        assertEquals(AUTHN_CONTEXT_CLASS_REF, state.getAuthnContextClassRef());
        assertTrue(state.getProofTokenState() != null);
    }

    @Test (expectedExceptions=IllegalStateException.class)
    public void testHoKNoProofTokenStateValidation() throws Exception {
        buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID, !WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.HOLDER_OF_KEY);
    }

    @Test
    public void testToString() throws Exception {
        /*
        build a few different types of instances, and call toString to insure no NPE ensues.
         */
        buildInvocationState(WITH_SPECIOUS_INSTANCE_ID, WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER).toString();
        buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID, WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.BEARER).toString();
        buildInvocationState(WITH_SPECIOUS_INSTANCE_ID, WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.HOLDER_OF_KEY).toString();
        buildInvocationState(!WITH_SPECIOUS_INSTANCE_ID, !WITH_PROOF_TOKEN_STATE, SAML2SubjectConfirmation.SENDER_VOUCHES).toString();

    }
    private TokenGenerationServiceInvocationState buildInvocationState(boolean withSpeciousInstanceId,
                                                                       boolean withProofTokenState,
                                                                       SAML2SubjectConfirmation subjectConfirmation) throws Exception {
        TokenGenerationServiceInvocationState.TokenGenerationServiceInvocationStateBuilder builder =
                TokenGenerationServiceInvocationState.builder();
        builder.tokenType(TOKEN_TYPE);
        builder.ssoTokenString(SSO_TOKEN_STRING);
        builder.saml2SubjectConfirmation(subjectConfirmation);
        builder.authNContextClassRef(AUTHN_CONTEXT_CLASS_REF);

        if (!withSpeciousInstanceId) {
            builder.stsInstanceId(STS_INSTANCE_ID);
        } else {
            builder.stsInstanceId("specious instance id");
        }

        if (withProofTokenState) {
            builder.proofTokenState(ProofTokenState.builder().x509Certificate(getCertificate()).build());
        }
        return builder.build();
    }

    private X509Certificate getCertificate() throws IOException, CertificateException {
        return (X509Certificate) CertificateFactory.getInstance(X_509).generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }
}