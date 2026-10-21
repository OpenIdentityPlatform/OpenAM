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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openidentityplatform.bouncycastle.asn1.x509.KeyUsage;
import org.testng.annotations.Test;

import com.sun.identity.security.cert.AMCRLStore.CrlUpdateDecision;

/**
 * Verifies that a CRL fetched from a distribution point is verified against the issuer before it
 * is written into the shared CRL stores, and that it can never move the store backwards
 * (GHSA-g5vw-v384-gw9v).
 *
 * <p>The stores are the JVM-wide static CRL cache and the {@code certificaterevocationlist}
 * attribute in the LDAP CRL store, both keyed on issuer DN. Anything written there outlives the
 * request that fetched it — and, for the LDAP entry, the process that fetched it.
 */
public class AMCRLStoreCrlUpdateTest extends CertTestBase {

    /** The single candidate key a deployment with one certificate per CA offers. */
    private static List<PublicKey> caKey() {
        return singletonList(CA_KEY_PAIR.getPublic());
    }

    private static X509Certificate certificateIssuedByTestCa() throws Exception {
        KeyPair keyPair = newRsaKeyPair();
        return buildCertificate("subject", keyPair.getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                BigInteger.valueOf(7), false, (String) null);
    }

    /**
     * A certificate whose distribution point names {@code crlIssuerCn} as the CRL issuer, i.e.
     * one issued by a PKI that delegates CRL signing.
     */
    private static X509Certificate certificateDelegatingCrlsTo(String crlIssuerCn)
            throws Exception {
        KeyPair keyPair = newRsaKeyPair();
        return buildCertificateWithRawDps("subject", keyPair.getPublic(), CA_CN,
                CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(8),
                uriDpWithCrlIssuer("http://pki.example.com/delegated.crl", crlIssuerCn));
    }

    @Test
    public void crlSignedByTheIssuerIsStored() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                .isEqualTo(CrlUpdateDecision.STORE);
    }

    @Test
    public void crlFromAnotherIssuerIsDiscarded() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        KeyPair otherKeyPair = newRsaKeyPair();
        X509CRL crl = buildCrl("OtherCA", otherKeyPair.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void anIndirectCrlIsUsedButNeverStored() throws Exception {
        // A PKI that delegates CRL signing (RFC 5280 section 5.2.5) publishes a CRL whose issuer
        // is the delegated signer, not the CA named in the certificate. Discarding it outright —
        // the only reading of an issuer mismatch before the indirectCRL flag was looked at —
        // leaves such a deployment on an expiring CRL and then fails every certificate login
        // closed, silently, from the moment it upgrades.
        //
        // It still must not be stored: nothing reachable from here establishes that this CA
        // really delegated to that signer beyond the certificate's own say-so, and the shared
        // stores are keyed on issuer DN. PKIX, which also checks the delegated signer's own
        // certificate path, is where the delegation is really established.
        X509Certificate cert = certificateDelegatingCrlsTo("CRL Signer");
        KeyPair signerKeyPair = newRsaKeyPair();
        X509CRL crl = buildIndirectCrl("CRL Signer", signerKeyPair.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                .isEqualTo(CrlUpdateDecision.USE_ONLY);
    }

    @Test
    public void anIndirectCrlTheCertificateDelegatesNothingToIsDiscarded() throws Exception {
        // The indirectCRL branch is the one decision here that can check neither a signature —
        // the delegated signer's key is not the issuer's — nor a CRL number, which belongs to a
        // different series. Without the delegation the certificate itself declares, setting one
        // bit in a self-signed CRL would be enough for whoever answers a distribution point to
        // have it handed back to the caller in place of the CRL already held. RFC 5280 section
        // 6.3.3 is the same rule: a CRL from another issuer counts only for a certificate whose
        // distribution point named that issuer.
        X509Certificate cert = certificateIssuedByTestCa();
        KeyPair attackerKeyPair = newRsaKeyPair();
        X509CRL crl = buildIndirectCrl("Whoever", attackerKeyPair.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void anIndirectCrlFromASignerOtherThanTheDelegatedOneIsDiscarded() throws Exception {
        // Naming a cRLIssuer is not a blanket permission for indirect CRLs: it authorises the one
        // signer it names, so a CRL from any other is in exactly the position of one from a
        // certificate that delegates to nobody.
        X509Certificate cert = certificateDelegatingCrlsTo("CRL Signer");
        KeyPair attackerKeyPair = newRsaKeyPair();
        X509CRL crl = buildIndirectCrl("Someone Else", attackerKeyPair.getPrivate(),
                BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void aCrlFromAnotherIssuerIsStillDiscardedWhenItIsNotMarkedIndirect() throws Exception {
        // The indirectCRL flag is the whole discriminator: without it, an issuer mismatch is
        // either the wrong CRL or one substituted by whoever answered the distribution point.
        X509Certificate cert = certificateIssuedByTestCa();
        KeyPair otherKeyPair = newRsaKeyPair();
        // Carries an IssuingDistributionPoint, but with indirectCRL absent.
        X509CRL crl = buildCrl("OtherCA", otherKeyPair.getPrivate(), BigInteger.ONE,
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)),
                uri("http://pki.example.com/ca.crl"));

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void crlWithAForgedSignatureIsDiscarded() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        // Claims to be the TestCA CRL, but is signed with a key the attacker controls.
        KeyPair attackerKeyPair = newRsaKeyPair();
        X509CRL crl = buildCrl(CA_CN, attackerKeyPair.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void crlIsUsedButNotStoredWhenTheIssuerKeyIsUnknown() throws Exception {
        // The issuer is an intermediate CA that is not a trust anchor: the CRL cannot be checked
        // here, so PKIX is left to check it, but it must not reach the shared stores.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, emptyList()))
                .isEqualTo(CrlUpdateDecision.USE_ONLY);
    }

    @Test
    public void olderCrlIsDiscardedEvenWhenTheIssuerKeyIsUnknown() throws Exception {
        // Comparing CRL numbers needs no key, so not being able to verify the signature is no
        // reason to accept a CRL that is plainly superseded by the one already held.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL current = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        X509CRL older = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(3));

        assertThat(AMCRLStore.validateFetchedCrl(cert, older, current, emptyList()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void missingCrlIsDiscarded() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();

        assertThat(AMCRLStore.validateFetchedCrl(cert, null, null, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void olderCrlNumberIsDiscarded() throws Exception {
        // The rollback: a genuine, correctly signed, still-in-validity CRL from before a
        // revocation would otherwise replace the current one and un-revoke a certificate.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL current = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        X509CRL older = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(3));

        assertThat(AMCRLStore.validateFetchedCrl(cert, older, current, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void newerCrlNumberIsStored() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL current = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(3));
        X509CRL newer = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));

        assertThat(AMCRLStore.validateFetchedCrl(cert, newer, current, caKey()))
                .isEqualTo(CrlUpdateDecision.STORE);
    }

    @Test
    public void sameCrlNumberIsStored() throws Exception {
        // Re-fetching the current CRL is normal once it is close to nextUpdate.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL current = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        X509CRL same = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));

        assertThat(AMCRLStore.validateFetchedCrl(cert, same, current, caKey()))
                .isEqualTo(CrlUpdateDecision.STORE);
    }

    @Test
    public void crlWithoutANumberIsDiscardedWhenTheStoredOneHasOne() throws Exception {
        // Otherwise the CRLNumber check is bypassed simply by omitting the extension.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL current = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        X509CRL unnumbered = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), null);

        assertThat(AMCRLStore.validateFetchedCrl(cert, unnumbered, current,
                caKey())).isEqualTo(CrlUpdateDecision.DISCARD);
    }

    @Test
    public void crlWithoutANumberIsStoredWhenTheStoredOneHasNoneEither() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL current = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), null);
        X509CRL fetched = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), null);

        assertThat(AMCRLStore.validateFetchedCrl(cert, fetched, current, caKey()))
                .isEqualTo(CrlUpdateDecision.STORE);
    }

    @Test
    public void issuerPublicKeyIsTakenFromTheTrustStore() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        withTestTrustStore(CA_CERT, () -> {
            assertThat(AMCRLStore.findIssuerPublicKeys(cert))
                    .containsExactly(CA_KEY_PAIR.getPublic());
        });
    }

    @Test
    public void noIssuerPublicKeyIsOfferedWhenTheIssuerIsNotATrustAnchor() throws Exception {
        withTestTrustStore(CA_CERT, () -> {
            // ATTACKER_CERT is self-signed and not in the trust store.
            assertThat(AMCRLStore.findIssuerPublicKeys(ATTACKER_CERT)).isEmpty();
        });
    }

    @Test
    public void crlWithAnUnverifiableSignatureIsStoredWhenTheSignatureCheckIsDisabled()
            throws Exception {
        // A CRL signed by a delegated CRL signer (RFC 5280 section 5.2.5) or under a rotated CA
        // key cannot be told apart from a forgery here, so such a deployment would freeze on a
        // stale CRL and then fail closed. This is the documented way out.
        X509Certificate cert = certificateIssuedByTestCa();
        KeyPair delegatedSigner = newRsaKeyPair();
        X509CRL crl = buildCrl(CA_CN, delegatedSigner.getPrivate(), BigInteger.ONE);

        withSystemProperty(AMCRLStore.CRL_REQUIRE_VERIFIED_SIGNATURE, "false", () ->
                assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                        .isEqualTo(CrlUpdateDecision.STORE));
    }

    @Test
    public void disablingTheSignatureCheckStillRejectsAnotherIssuersCrl() throws Exception {
        // The escape hatch relaxes only the signature check. A CRL naming a different issuer is
        // either the wrong CRL or a substituted one, and this code cannot tell which: the one
        // legitimate case, a delegated CRL signer (RFC 5280 section 5.2.5), is recognisable only
        // by matching the distribution point's cRLIssuer against the CRL's indirectCRL flag, and
        // accepting it without that would let anyone who controls a distribution point store a
        // CRL for an issuer they have nothing to do with. Indirect CRLs are not fetched.
        X509Certificate cert = certificateIssuedByTestCa();
        KeyPair otherKeyPair = newRsaKeyPair();
        X509CRL crl = buildCrl("OtherCA", otherKeyPair.getPrivate(), BigInteger.ONE);

        withSystemProperty(AMCRLStore.CRL_REQUIRE_VERIFIED_SIGNATURE, "false", () ->
                assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, caKey()))
                        .isEqualTo(CrlUpdateDecision.DISCARD));
    }

    @Test
    public void disablingTheSignatureCheckStillRejectsARollback() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL current = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        X509CRL older = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(3));

        withSystemProperty(AMCRLStore.CRL_REQUIRE_VERIFIED_SIGNATURE, "false", () ->
                assertThat(AMCRLStore.validateFetchedCrl(cert, older, current,
                        caKey())).isEqualTo(CrlUpdateDecision.DISCARD));
    }

    @Test
    public void issuerKeyIsTakenFromTheChainWhenItReachesATrustAnchor() throws Exception {
        // The common three-tier shape: the root is the anchor, the intermediate that issued the
        // certificate is only in the presented chain. Without this the key is never found, the
        // CRL is never stored, and it is refetched on every single authentication.
        KeyPair intermediateKey = newRsaKeyPair();
        X509Certificate intermediate = buildCertificate("Intermediate", intermediateKey.getPublic(),
                CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(20), true, (String) null);
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Intermediate", intermediateKey.getPrivate(), BigInteger.valueOf(21), false,
                (String) null);

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(endEntity,
                new X509Certificate[]{endEntity, intermediate, CA_CERT}))
                .as("the intermediate's key is vouched for by the anchor at the top of the chain")
                .containsExactly(intermediateKey.getPublic()));
    }

    @Test
    public void issuerKeyIsNotTakenFromAChainThatDoesNotReachATrustAnchor() throws Exception {
        // getCRL is published API, so an out-of-tree caller may pass a chain nobody validated.
        // A key from such a chain must not be able to authorise a CRL into the shared stores.
        KeyPair rogueRootKey = newRsaKeyPair();
        X509Certificate rogueRoot = buildCertificate("RogueRoot", rogueRootKey.getPublic(),
                "RogueRoot", rogueRootKey.getPrivate(), BigInteger.valueOf(30), true, (String) null);
        KeyPair intermediateKey = newRsaKeyPair();
        X509Certificate intermediate = buildCertificate("Intermediate", intermediateKey.getPublic(),
                "RogueRoot", rogueRootKey.getPrivate(), BigInteger.valueOf(31), true, (String) null);
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Intermediate", intermediateKey.getPrivate(), BigInteger.valueOf(32), false,
                (String) null);

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(endEntity,
                new X509Certificate[]{endEntity, intermediate, rogueRoot})).isEmpty());
    }

    @Test
    public void issuerKeyIsNotTakenFromAChainWhoseLinkToTheAnchorIsForged() throws Exception {
        // The intermediate names the real CA as its issuer but is signed with the attacker's key,
        // so the chain only appears to reach the anchor.
        KeyPair forgedKey = newRsaKeyPair();
        X509Certificate forgedIntermediate = buildCertificate("Intermediate",
                forgedKey.getPublic(), CA_CN, forgedKey.getPrivate(), BigInteger.valueOf(40), true,
                (String) null);
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Intermediate", forgedKey.getPrivate(), BigInteger.valueOf(41), false,
                (String) null);

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(endEntity,
                new X509Certificate[]{endEntity, forgedIntermediate})).isEmpty());
    }

    @Test
    public void anOlderCrlIsRejectedEvenWhenNeitherCarriesACrlNumber() throws Exception {
        // RFC 5280 requires the CRLNumber extension, but CAs that omit it exist — and for those
        // the number comparison has nothing to compare, so replaying an older CRL would be
        // accepted as an update and un-revoke everything revoked since.
        X509Certificate cert = certificateIssuedByTestCa();
        Date now = new Date();
        X509CRL stored = buildCrlIssuedAt(CA_CN, CA_KEY_PAIR.getPrivate(), null,
                new Date(now.getTime() - TimeUnit.DAYS.toMillis(1)));
        X509CRL older = buildCrlIssuedAt(CA_CN, CA_KEY_PAIR.getPrivate(), null,
                new Date(now.getTime() - TimeUnit.DAYS.toMillis(10)));

        assertThat(AMCRLStore.validateFetchedCrl(cert, older, stored, caKey()))
                .isEqualTo(CrlUpdateDecision.DISCARD);
        assertThat(AMCRLStore.validateFetchedCrl(cert, stored, older, caKey()))
                .as("the newer one in the same pair is still accepted")
                .isEqualTo(CrlUpdateDecision.STORE);
    }

    @Test
    public void disablingTheSignatureCheckStoresACrlWhoseIssuerKeyIsUnknown() throws Exception {
        // This is the case the switch exists for. A CRL whose signing key OpenAM cannot reach is
        // otherwise never stored, so it is refetched on every single validation — for the
        // SAML2/WS-Fed path, on every unauthenticated request. Leaving the switch inert here
        // would leave that with no remedy at all.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, emptyList()))
                .as("with the check on, an unverifiable CRL is used but never stored")
                .isEqualTo(CrlUpdateDecision.USE_ONLY);

        withSystemProperty(AMCRLStore.CRL_REQUIRE_VERIFIED_SIGNATURE, "false", () ->
                assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, emptyList()))
                        .isEqualTo(CrlUpdateDecision.STORE));
    }

    @Test
    public void aSwitchValueOtherThanTrueOrFalseLeavesTheSignatureCheckOn() throws Exception {
        // An operator who writes one of these means "on". Read as anything but the default, the
        // check would be off with nothing in the log to tell that apart from a deliberate opt-out,
        // and unverified CRLs would go into the JVM-wide cache and the directory.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        for (String value : Arrays.asList("yes", "1", "on", "")) {
            withSystemProperty(AMCRLStore.CRL_REQUIRE_VERIFIED_SIGNATURE, value, () ->
                    assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, emptyList()))
                            .as("with the switch set to \"%s\"", value)
                            .isEqualTo(CrlUpdateDecision.USE_ONLY));
        }
    }

    @Test
    public void theSignatureCheckSwitchIgnoresCaseAndSurroundingSpace() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        withSystemProperty(AMCRLStore.CRL_REQUIRE_VERIFIED_SIGNATURE, " FALSE ", () ->
                assertThat(AMCRLStore.validateFetchedCrl(cert, crl, null, emptyList()))
                        .isEqualTo(CrlUpdateDecision.STORE));
    }

    @Test
    public void disablingTheSignatureCheckStillRejectsARollbackWhenNoKeyIsAvailable()
            throws Exception {
        // The switch relaxes the signature check only. Without a key there is nothing else left
        // to catch a replayed CRL, so the CRL-number check has to keep applying.
        X509Certificate cert = certificateIssuedByTestCa();
        X509CRL stored = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        X509CRL older = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(3));

        withSystemProperty(AMCRLStore.CRL_REQUIRE_VERIFIED_SIGNATURE, "false", () ->
                assertThat(AMCRLStore.validateFetchedCrl(cert, older, stored, emptyList()))
                        .isEqualTo(CrlUpdateDecision.DISCARD));
    }

    @Test
    public void issuerKeyIsNotTakenFromAChainAnchoredThroughANonCaCertificate() throws Exception {
        // Every signature in this chain is genuine, so a walk that only checks signatures accepts
        // it. What it misses is that the certificate at the top of the walk is an ordinary leaf,
        // which was never authorised to issue anything. The attacker needs only one certificate
        // issued under a trusted anchor — with the default trust store being the JDK cacerts,
        // that is a free public certificate — and can then mint an intermediate holding a key of
        // their choosing, and with it a CRL that would be written to the shared stores.
        KeyPair leafKey = newRsaKeyPair();
        X509Certificate anchorIssuedLeaf = buildCertificate("shop.example.com", leafKey.getPublic(),
                CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(50), false, (String) null);
        KeyPair forgedKey = newRsaKeyPair();
        X509Certificate forgedIntermediate = buildCertificate("Intermediate", forgedKey.getPublic(),
                "shop.example.com", leafKey.getPrivate(), BigInteger.valueOf(51), true,
                (String) null);
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Intermediate", forgedKey.getPrivate(), BigInteger.valueOf(52), false,
                (String) null);

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(endEntity,
                new X509Certificate[]{endEntity, forgedIntermediate, anchorIssuedLeaf})).isEmpty());
    }

    @Test
    public void issuerKeyIsNotTakenFromAnIssuerThatMayNotSignCrls() throws Exception {
        // An intermediate whose keyUsage withholds cRLSign cannot legitimately have signed this
        // CRL, and PKIX will reject it downstream (RFC 5280 6.3.3). Storing it would guarantee a
        // revocation check that fails closed for every principal under the issuer.
        KeyPair intermediateKey = newRsaKeyPair();
        X509Certificate intermediate = buildCertificateWithKeyUsage("Intermediate",
                intermediateKey.getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                BigInteger.valueOf(53), true, Integer.valueOf(KeyUsage.keyCertSign));
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Intermediate", intermediateKey.getPrivate(), BigInteger.valueOf(54), false,
                (String) null);

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(endEntity,
                new X509Certificate[]{endEntity, intermediate, CA_CERT})).isEmpty());
    }

    @Test
    public void issuerKeyIsNotTakenFromATrustAnchorThatMayNotSignCrls() throws Exception {
        // Same rule for the trust store as for the chain, and it was applied only to the chain.
        // A trust anchor whose keyUsage withholds cRLSign but which signs its own CRLs anyway —
        // non-conformant, and real for older private CAs — would otherwise have that CRL written
        // to the JVM-wide cache and the LDAP entry, where PKIX then rejects it and revocation
        // fails closed for every principal under it, permanently.
        KeyPair anchorKey = newRsaKeyPair();
        X509Certificate anchor = buildCertificateWithKeyUsage("NoCrlSignCA", anchorKey.getPublic(),
                "NoCrlSignCA", anchorKey.getPrivate(), BigInteger.valueOf(60), true,
                Integer.valueOf(KeyUsage.keyCertSign));
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "NoCrlSignCA", anchorKey.getPrivate(), BigInteger.valueOf(61), false,
                (String) null);
        X509CRL crl = buildCrl("NoCrlSignCA", anchorKey.getPrivate(), BigInteger.ONE);

        withTestTrustStore(anchor, () -> {
            assertThat(AMCRLStore.findIssuerPublicKeys(endEntity, new X509Certificate[]{endEntity}))
                    .as("a trust anchor that may not sign CRLs may not authorise one either")
                    .isEmpty();
            assertThat(AMCRLStore.validateFetchedCrl(endEntity, crl, null,
                    AMCRLStore.findIssuerPublicKeys(endEntity, new X509Certificate[]{endEntity})))
                    .isEqualTo(CrlUpdateDecision.USE_ONLY);
        });
    }

    @Test
    public void aTrustAnchorThatMaySignCrlsStillAuthorisesOne() throws Exception {
        // The other half of the pair: the filter must not cost a conformant anchor its key.
        // CA_CERT is built with keyCertSign | cRLSign.
        X509Certificate endEntity = certificateIssuedByTestCa();
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.validateFetchedCrl(endEntity, crl,
                null, AMCRLStore.findIssuerPublicKeys(endEntity,
                        new X509Certificate[]{endEntity}))).isEqualTo(CrlUpdateDecision.STORE));
    }

    @Test
    public void issuerKeyIsTakenFromAnIssuerWithNoKeyUsageExtension() throws Exception {
        // Withholding cRLSign is a decision; omitting keyUsage altogether is not, and RFC 5280
        // leaves such a certificate unrestricted. Rejecting these would break older private CAs.
        KeyPair intermediateKey = newRsaKeyPair();
        X509Certificate intermediate = buildCertificateWithKeyUsage("Intermediate",
                intermediateKey.getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                BigInteger.valueOf(55), true, null);
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Intermediate", intermediateKey.getPrivate(), BigInteger.valueOf(56), false,
                (String) null);

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(endEntity,
                new X509Certificate[]{endEntity, intermediate, CA_CERT}))
                .containsExactly(intermediateKey.getPublic()));
    }

    @Test
    public void issuerKeyIsStillTakenFromTheTrustStoreWhenNoChainIsSupplied() throws Exception {
        X509Certificate cert = certificateIssuedByTestCa();
        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(cert, null))
                .containsExactly(CA_KEY_PAIR.getPublic()));
    }

    @Test
    public void everyTrustAnchorCarryingTheIssuerNameIsOffered() throws Exception {
        // A CA rolling its key over has two certificates with one subject name, and both are in
        // the trust store for the overlap. Offering only whichever the keystore enumerates first
        // means the CRL fails to verify half the time — and a CRL that fails to verify is not
        // stored, so the held one expires and every principal under that CA is locked out.
        KeyPair rolledOver = newRsaKeyPair();
        X509Certificate newCaCert = buildCertificate(CA_CN, rolledOver.getPublic(), CA_CN,
                rolledOver.getPrivate(), BigInteger.valueOf(60), true, (String) null);
        X509Certificate cert = certificateIssuedByTestCa();

        withTestTrustStore(Arrays.asList(CA_CERT, newCaCert), () ->
                assertThat(AMCRLStore.findIssuerPublicKeys(cert))
                        .containsExactlyInAnyOrder(CA_KEY_PAIR.getPublic(), rolledOver.getPublic()));
    }

    @Test
    public void aCrlVerifiesAgainstTheChainIssuerThatSignedItRatherThanItsNamesake() throws Exception {
        // The intermediate rolled its key over, so the chain carries the retired certificate and
        // the current one under the same name. Taking only the first is a coin toss, and losing it
        // discards a perfectly good CRL.
        KeyPair retiredKey = newRsaKeyPair();
        KeyPair currentKey = newRsaKeyPair();
        X509Certificate retired = buildCertificate("Intermediate", retiredKey.getPublic(), CA_CN,
                CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(61), true, (String) null);
        X509Certificate current = buildCertificate("Intermediate", currentKey.getPublic(), CA_CN,
                CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(62), true, (String) null);
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Intermediate", currentKey.getPrivate(), BigInteger.valueOf(63), false,
                (String) null);
        X509CRL crl = buildCrl("Intermediate", currentKey.getPrivate(), BigInteger.ONE);
        X509Certificate[] chain = {endEntity, retired, current, CA_CERT};

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.validateFetchedCrl(endEntity, crl,
                null, AMCRLStore.findIssuerPublicKeys(endEntity, chain)))
                .isEqualTo(CrlUpdateDecision.STORE));
    }

    @Test
    public void anIssuerIsAnchoredThroughTheChainMemberThatActuallySignedIt() throws Exception {
        // A root rollover, so the sub-CA has two certificates: the retired one chains to a root
        // that is no longer trusted, the current one to the anchor. Both carry the same subject
        // name, and only one of them signed the issuing CA. Stopping the walk at whichever name
        // matches first lands on a dead end and judges the whole chain unanchored — losing the
        // key, so the CRL is refetched on every authentication and never stored.
        KeyPair retiredRootKey = newRsaKeyPair();
        X509Certificate retiredRoot = buildCertificate("RetiredRoot", retiredRootKey.getPublic(),
                "RetiredRoot", retiredRootKey.getPrivate(), BigInteger.valueOf(64), true,
                (String) null);
        KeyPair retiredSubKey = newRsaKeyPair();
        X509Certificate retiredSub = buildCertificate("SubCA", retiredSubKey.getPublic(),
                "RetiredRoot", retiredRootKey.getPrivate(), BigInteger.valueOf(65), true,
                (String) null);
        KeyPair currentSubKey = newRsaKeyPair();
        X509Certificate currentSub = buildCertificate("SubCA", currentSubKey.getPublic(), CA_CN,
                CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(66), true, (String) null);
        KeyPair issuingKey = newRsaKeyPair();
        X509Certificate issuing = buildCertificate("Issuing", issuingKey.getPublic(), "SubCA",
                currentSubKey.getPrivate(), BigInteger.valueOf(67), true, (String) null);
        X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                "Issuing", issuingKey.getPrivate(), BigInteger.valueOf(68), false, (String) null);
        // The retired certificates come first, as a server presenting its historical chain sends
        // them.
        X509Certificate[] chain = {endEntity, issuing, retiredSub, retiredRoot, currentSub};

        withTestTrustStore(CA_CERT, () -> assertThat(AMCRLStore.findIssuerPublicKeys(endEntity,
                chain)).containsExactly(issuingKey.getPublic()));
    }

}
