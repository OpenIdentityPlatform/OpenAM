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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import java.security.cert.X509Certificate;

/**
 * Verifies the Phase 1 trust-chain guard in AMCertPath that prevents SSRF via
 * CRL Distribution Point URLs embedded in untrusted client certificates.
 */
public class AMCertPathTest extends CertTestBase {

    @Test
    public void verify_selfSignedCertWithCrlDp_returnsFalse() throws Exception {
        // withNoFetch asserts the loopback CRL DP listener (baked into ATTACKER_CERT) saw
        // zero connections — proving the guard fires before any fetch is attempted.
        withNoFetch(() ->
            withTestTrustStore(CA_CERT, () -> assertFalse(
                    new AMCertPath(null).verify(new X509Certificate[]{ATTACKER_CERT}, false, false),
                    "Phase 1 PKIX check must reject a self-signed cert not in the trust store")));
    }

    @Test
    public void verify_certSignedByTrustedCA_returnsTrue() throws Exception {
        // The trust anchor (CA_CERT) is supplied via the trust store, not inside the CertPath,
        // so only the end-entity cert is passed to verify() — the canonical, provider-independent
        // PKIX form.
        withTestTrustStore(CA_CERT, () -> assertTrue(
                new AMCertPath(null).verify(new X509Certificate[]{EE_CERT}, false, false),
                "Phase 1 PKIX check must pass for a cert that roots in a trusted CA"));
    }
}
