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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.cert.X509Certificate;

import org.testng.annotations.Test;

/**
 * Verifies the Phase 1 trust-chain guard wired into CRLValidator.validateCertificate()
 * that prevents SSRF via CRL Distribution Point URLs in untrusted client certificates.
 */
public class CRLValidatorTest extends CertTestBase {

    @Test
    public void validateCertificate_selfSignedCertWithCrlDp_returnsFalse() throws Exception {
        // withNoFetch hands over a self-signed certificate whose CRL DP is a listener of its
        // own, then asserts it saw zero connections — proving the guard fires before any fetch.
        withNoFetch(attackerCert ->
            withTestTrustStore(CA_CERT, () -> assertFalse(
                    CRLValidator.validateCertificate(attackerCert, false),
                    "Phase 1 PKIX check must reject a self-signed cert not in the trust store")));
    }

    @Test
    public void getCRL_selfSignedCertWithCrlDp_returnsNullWithoutFetch() throws Exception {
        // The deprecated getCRL() entry point reaches the same CRL DP sink as
        // validateCertificate(); its guard must also reject the cert before any fetch.
        withNoFetch(attackerCert ->
            withTestTrustStore(CA_CERT, () -> assertNull(
                    CRLValidator.getCRL(attackerCert),
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

    /**
     * Why {@code validateCertificate()} tests {@code ldapParams} and {@code crlSearchAttr} before
     * resolving the issuer certificate for the CRL chain.
     *
     * <p>Both stay null unless {@code com.sun.identity.crl.cache.directory.host} is configured,
     * while {@code SAML2Utils.validateCertificate} calls in whenever
     * {@code com.sun.identity.saml2.crl.check} is on — the two settings are independent. Resolved
     * unguarded, this is the exception that would land in {@code validateCertificate}'s catch-all
     * and turn into "certificate rejected" for every certificate that is not a root CA, i.e. all
     * SAML2 signature validation in such a deployment.
     *
     * <p>The guard cannot be asserted through the return value: with no CRL store configured,
     * phase 2 returns false for a trusted certificate either way (see above), so this pins the
     * hazard itself.
     */
    @Test
    public void resolvingTheIssuerWithNoCrlDirectoryConfiguredThrows() {
        assertThrows(NullPointerException.class,
                () -> AMCertStore.getIssuerCertificate(null, EE_CERT, null));
    }

    /**
     * The CA-status recursion hands {@code validateCertificate} whatever
     * {@code AMCertStore.getIssuerCertificate} returned, which is null whenever the issuer has no
     * entry in the CRL directory — or has one carrying only {@code certificaterevocationlist} and
     * no {@code usercertificate}/{@code cacertificate} attribute, the usual shape of a directory
     * that publishes CRLs alone. A null certificate must be rejected, as it was before the issuer
     * was resolved here: {@code AMCertPath.verify} refused the null element of the path and the
     * catch-all turned it into "certificate rejected". It must not reach
     * {@code AMCertStore.isRootCA}, which sits above this method's own try and would throw a
     * NullPointerException out through {@code SAML2Utils.validateCertificate} — neither that
     * method nor {@code FMSigProvider.isValidSignature} catches one.
     */
    @Test
    public void aNullCertificateIsRejectedRatherThanThrowing() {
        assertFalse(CRLValidator.validateCertificate(null, true),
                "a certificate that could not be resolved must fail closed, not throw");
    }

    /**
     * ldapParams is one object shared by every thread validating a certificate, and AMCertStore's
     * issuer lookups read it too: a filter written into it hands this lookup's search to whichever
     * other search reads the object next.
     */
    @Test
    public void getCRLLeavesTheSharedSearchFilterAlone() throws Exception {
        AMLDAPCertStoreParameters shared = AMCertStore.setLdapStoreParam("127.0.0.1", closedPort(),
                "cn=admin", "secret", "dc=example,dc=com", null, false);
        shared.setSearchFilter("(cn=the caller's own)");
        Object savedParams = swapStatic("ldapParams", shared);
        Object savedAttr = swapStatic("crlSearchAttr", "CN");
        try {
            withTestTrustStore(CA_CERT, () -> CRLValidator.getCRL(EE_CERT));
        } finally {
            swapStatic("ldapParams", savedParams);
            swapStatic("crlSearchAttr", savedAttr);
        }
        assertEquals(shared.getSearchFilter(), "(cn=the caller's own)",
                "getCRL must set its filter on a copy");
    }

    /** Both statics stay null unless the CRL directory host is configured. */
    private static Object swapStatic(String name, Object value) throws Exception {
        Field field = CRLValidator.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    /** A loopback port nothing listens on, so the lookup fails fast instead of hanging. */
    private static int closedPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
