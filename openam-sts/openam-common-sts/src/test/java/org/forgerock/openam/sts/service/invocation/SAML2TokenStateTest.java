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

import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.testng.Assert.assertEquals;

public class SAML2TokenStateTest {
    @Test
    public void testJsonRoundTripNoProofTokenState() throws Exception {
        SAML2TokenState tokenState = SAML2TokenState.builder()
                .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                .build();
        assertEquals(tokenState, SAML2TokenState.fromJson(tokenState.toJson()));
    }

    @Test
    public void testJsonRoundTripWithProofTokenState() throws Exception {
        ProofTokenState proofTokenState = ProofTokenState.builder().x509Certificate(getCertificate()).build();
        SAML2TokenState tokenState = SAML2TokenState.builder()
                .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                .proofTokenState(proofTokenState)
                .build();
        assertEquals(tokenState, SAML2TokenState.fromJson(tokenState.toJson()));
    }

    @Test (expectedExceptions = TokenMarshalException.class)
    public void testHoKWithoutProofTokenState() throws Exception {
        SAML2TokenState.builder()
                .saml2SubjectConfirmation(SAML2SubjectConfirmation.HOLDER_OF_KEY)
                .build();
    }

    private X509Certificate getCertificate() throws IOException, CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }

}
