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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;


import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SAML2TokenGenerationStateTest {
    private static final boolean WITH_PROOF_TOKEN = true;
    private static final String X_509 = "X.509";
    private static final String AUTHN_CONTEXT_CLASS_REF = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    private final SAML2SubjectConfirmation SUBJECT_CONFIRMATION = SAML2SubjectConfirmation.BEARER;


    @Test
    public void testEquals() throws CertificateException, TokenMarshalException, IOException {
        SAML2TokenGenerationState stateOne = buildState(WITH_PROOF_TOKEN);
        SAML2TokenGenerationState stateTwo = buildState(WITH_PROOF_TOKEN);
        assertEquals(stateOne, stateTwo);

        stateOne = buildState(!WITH_PROOF_TOKEN);
        stateTwo = buildState(!WITH_PROOF_TOKEN);
        assertEquals(stateOne, stateTwo);

        stateOne = buildState(WITH_PROOF_TOKEN);
        stateTwo = buildState(!WITH_PROOF_TOKEN);
        assertNotEquals(stateOne, stateTwo);
    }

    @Test
    public void testFieldPersistence() throws CertificateException, TokenMarshalException, IOException {
        SAML2TokenGenerationState stateOne = buildState(WITH_PROOF_TOKEN);
        assertEquals(AUTHN_CONTEXT_CLASS_REF, stateOne.getAuthnContextClassRef());
        assertEquals(SUBJECT_CONFIRMATION, stateOne.getSaml2SubjectConfirmation());
        assertEquals(getCertificate().getSerialNumber(), stateOne.getProofTokenState().getX509Certificate().getSerialNumber());
    }

    @Test
    public void testJsonRoundTrip() throws CertificateException, TokenMarshalException, IOException {
        SAML2TokenGenerationState stateOne = buildState(WITH_PROOF_TOKEN);
        assertEquals(stateOne, SAML2TokenGenerationState.fromJson(stateOne.toJson()));

        stateOne = buildState(!WITH_PROOF_TOKEN);
        assertEquals(stateOne, SAML2TokenGenerationState.fromJson(stateOne.toJson()));
    }

    SAML2TokenGenerationState buildState(boolean withProofTokenState) throws IOException, CertificateException, TokenMarshalException {
        SAML2TokenGenerationState.SAML2TokenGenerationStateBuilder builder = SAML2TokenGenerationState.builder();
        if (withProofTokenState) {
            builder.proofTokenState(ProofTokenState.builder().x509Certificate(getCertificate()).build());
        }
        return builder
                .authenticationContextClassReference(AUTHN_CONTEXT_CLASS_REF)
                .subjectConfirmation(SUBJECT_CONFIRMATION)
                .build();
    }

    private X509Certificate getCertificate() throws IOException, CertificateException {
        return (X509Certificate) CertificateFactory.getInstance(X_509).generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }
}
