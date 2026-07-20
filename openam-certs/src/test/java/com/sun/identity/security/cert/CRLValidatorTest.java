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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.security.cert;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.security.cert.X509Certificate;

import org.testng.annotations.Test;

/**
 * Verifies the Phase 1 trust-chain guard wired into CRLValidator.validateCertificate()
 * that prevents SSRF via CRL Distribution Point URLs in untrusted client certificates.
 */
public class CRLValidatorTest extends CertTestBase {

    @Test
    public void validateCertificate_selfSignedCertWithCrlDp_returnsFalse() throws Exception {
        // withNoFetch asserts the loopback CRL DP listener (baked into ATTACKER_CERT) saw
        // zero connections — proving the guard fires before any fetch is attempted.
        withNoFetch(() ->
            withTestTrustStore(CA_CERT, () -> assertFalse(
                    CRLValidator.validateCertificate(ATTACKER_CERT, false),
                    "Phase 1 PKIX check must reject a self-signed cert not in the trust store")));
    }

    @Test
    public void getCRL_selfSignedCertWithCrlDp_returnsNullWithoutFetch() throws Exception {
        // The deprecated getCRL() entry point reaches the same CRL DP sink as
        // validateCertificate(); its guard must also reject the cert before any fetch.
        withNoFetch(() ->
            withTestTrustStore(CA_CERT, () -> assertNull(
                    CRLValidator.getCRL(ATTACKER_CERT),
                    "getCRL must reject an untrusted cert before any CRL DP fetch")));
    }

    /**
     * Phase 2 in validateCertificate() always uses crlEnabled=true. With no LDAP CRL store
     * configured in the test environment, Phase 2 returns false even for trusted-CA certs
     * (an end-to-end happy path would need a mocked CRL store). This test therefore verifies
     * Phase 1 directly — the guard that must NOT block a trusted cert before the CRL fetch.
     */
    @Test
    public void verify_certSignedByTrustedCA_passesPhaseOneGuard() throws Exception {
        // Trust anchor supplied via the trust store; only the end-entity cert is in the CertPath.
        withTestTrustStore(CA_CERT, () -> assertTrue(
                new AMCertPath(null).verify(new X509Certificate[]{EE_CERT}, false, false),
                "Phase 1 trust-chain guard must accept a cert whose issuer is in the trust store"));
    }
}
