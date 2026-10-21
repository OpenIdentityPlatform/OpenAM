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

import static com.sun.identity.security.cert.TestHttpServer.respond;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.RequestContext;
import org.forgerock.opendj.ldap.RequestHandler;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldif.LDIFEntryReader;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link AMCRLStore#getCRL(X509Certificate)} end to end against an in-memory LDAP CRL
 * store and a live distribution point, covering what the fetched CRL is allowed to do to the two
 * shared stores: the JVM-wide CRL cache and the LDAP {@code certificaterevocationlist} attribute
 * (GHSA-g5vw-v384-gw9v).
 */
public class AMCRLStoreUpdateFlowTest extends CertTestBase {

    private static final String BASE_DN = "dc=example,dc=com";
    private static final String CRL_ENTRY_DN = "cn=" + CA_CN + "," + BASE_DN;
    private static final String CRL_ATTRIBUTE = "certificateRevocationList;binary";
    private static final byte[] GARBAGE = "not-a-crl".getBytes();

    /**
     * Every connection factory handed to a store under test. {@code AMCertStore} never closes
     * {@code ldapconn} — the leak this PR documents — so each of these would otherwise keep
     * itself and its reference-counted transport alive for the whole surefire JVM.
     */
    private final List<ConnectionFactory> factories = new ArrayList<>();

    @AfterMethod
    public void closeFactories() {
        for (ConnectionFactory factory : factories) {
            factory.close();
        }
        factories.clear();
    }

    @Test
    public void aFailedFetchKeepsThePreviouslyStoredCrl() throws Exception {
        X509CRL stored = expiredCrl(BigInteger.valueOf(5));
        MemoryBackend backend = backendHolding(stored);

        try (TestHttpServer dp = new TestHttpServer(respond(500, GARBAGE))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRL(cert))
                        .as("a failed fetch must not discard a good CRL")
                        .isEqualTo(stored);
                assertThat(store.getCRLFromCache(cert))
                        .as("a failed fetch must not evict the cache entry either")
                        .isEqualTo(stored);
                assertThat(crlIn(backend)).isEqualTo(stored.getEncoded());
            }));
        }
    }

    @Test
    public void aForgedCrlIsNeverStored() throws Exception {
        X509CRL stored = expiredCrl(BigInteger.valueOf(5));
        MemoryBackend backend = backendHolding(stored);

        KeyPair attacker = newRsaKeyPair();
        X509CRL forged = buildCrl(CA_CN, attacker.getPrivate(), BigInteger.valueOf(99));

        try (TestHttpServer dp = new TestHttpServer(respond(200, forged.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                store.getCRL(cert);
                assertThat(crlIn(backend))
                        .as("an unverified CRL must not be written to the LDAP CRL store")
                        .isEqualTo(stored.getEncoded());
                assertThat(store.getCRLFromCache(cert))
                        .as("an unverified CRL must not be written to the CRL cache")
                        .isEqualTo(stored);
            }));
        }
    }

    @Test
    public void aRolledBackCrlIsNeverStored() throws Exception {
        X509CRL stored = expiredCrl(BigInteger.valueOf(5));
        MemoryBackend backend = backendHolding(stored);

        // Genuine, correctly signed and still in validity — but from before a revocation.
        X509CRL older = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(3));

        try (TestHttpServer dp = new TestHttpServer(respond(200, older.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRL(cert)).isEqualTo(stored);
                assertThat(crlIn(backend))
                        .as("a superseded CRL must not replace the stored one")
                        .isEqualTo(stored.getEncoded());
                assertThat(store.getCRLFromCache(cert)).isEqualTo(stored);
            }));
        }
    }

    @Test
    public void aVerifiedNewerCrlIsStored() throws Exception {
        X509CRL stored = expiredCrl(BigInteger.valueOf(3));
        MemoryBackend backend = backendHolding(stored);

        X509CRL newer = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));

        try (TestHttpServer dp = new TestHttpServer(respond(200, newer.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRL(cert)).isEqualTo(newer);
                assertThat(crlIn(backend)).isEqualTo(newer.getEncoded());
                assertThat(store.getCRLFromCache(cert)).isEqualTo(newer);
            }));
        }
    }

    @Test
    public void aCrlVerifiedThroughThePresentedChainIsStored() throws Exception {
        // The common three-tier PKI: only the root is a trust anchor, so the issuing
        // intermediate's key is reachable only through the presented chain. Without it the CRL
        // can never be stored and is refetched on every single authentication.
        KeyPair intermediateKey = newRsaKeyPair();
        X509Certificate intermediate = buildCertificate("Intermediate", intermediateKey.getPublic(),
                CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(50), true, (String) null);

        X509CRL stored = expiredCrl("Intermediate", intermediateKey.getPrivate(),
                BigInteger.valueOf(3));
        MemoryBackend backend = backendHolding(stored, "Intermediate");
        X509CRL newer = buildCrl("Intermediate", intermediateKey.getPrivate(),
                BigInteger.valueOf(5));

        try (TestHttpServer dp = new TestHttpServer(respond(200, newer.getEncoded()))) {
            X509Certificate endEntity = buildCertificate("user", newRsaKeyPair().getPublic(),
                    "Intermediate", intermediateKey.getPrivate(), BigInteger.valueOf(51), false,
                    dp.url());
            AMCRLStore store = storeBackedBy(backend, "Intermediate");
            clearCache(endEntity);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRL(endEntity,
                        new X509Certificate[]{endEntity, intermediate, CA_CERT})).isEqualTo(newer);
                assertThat(crlIn(backend, "Intermediate"))
                        .as("a CRL verified through the chain must be written to the LDAP store")
                        .isEqualTo(newer.getEncoded());
                assertThat(store.getCRLFromCache(endEntity))
                        .as("and cached, so the next authentication does not refetch it")
                        .isEqualTo(newer);
            }));
        }
    }

    @Test
    public void aVerifiedCrlIsStoredWhenTheStaleOneCameFromTheCache() throws Exception {
        // Every other flow test starts from a cleared cache, which makes getCRL read the LDAP
        // entry on the way in. On the second and every later authentication under an issuer the
        // stale CRL comes from the cache instead and that read is skipped — the common case at
        // runtime, and the one in which the store must still be able to write the fresh CRL back.
        X509CRL stale = expiredCrl(BigInteger.valueOf(3));
        MemoryBackend backend = backendHolding(stale);
        X509CRL newer = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));

        try (TestHttpServer dp = new TestHttpServer(respond(200, newer.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            store.updateCRLCache(cert, stale);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRL(cert)).isEqualTo(newer);
                assertThat(crlIn(backend))
                        .as("the fresh CRL must reach LDAP even though the stale one was cached")
                        .isEqualTo(newer.getEncoded());
                assertThat(store.getCRLFromCache(cert)).isEqualTo(newer);
            }));
        }
    }

    @Test
    public void readingBackTheEntryBeforeTheWriteAsksForTheCrlOnly() throws Exception {
        // With the stale CRL taken from the cache, the entry is read for the first time just
        // before the write, to compare against what it holds. That needs its CRL and nothing
        // else: a CA entry routinely carries certificates and cross-certificates as well, and
        // every other read of it already names the two CRL attributes.
        X509CRL stale = expiredCrl(BigInteger.valueOf(3));
        RecordingLdapBackend directory = new RecordingLdapBackend(backendHolding(stale));
        X509CRL newer = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));

        try (TestHttpServer dp = new TestHttpServer(respond(200, newer.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(directory);
            store.updateCRLCache(cert, stale);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () ->
                    assertThat(store.getCRL(cert)).isEqualTo(newer)));
        }
        List<SearchRequest> searches = directory.searches();
        assertThat(searches).as("the entry is read back once").hasSize(1);
        assertThat(searches.get(0).getAttributes()).containsExactlyInAnyOrder(
                "certificaterevocationlist", "certificaterevocationlist;binary");
    }

    @Test
    public void aVerifiedCrlIsStoredIntoAnEntryThatHoldsNoCrlYet() throws Exception {
        // A freshly provisioned CRL entry: it exists, but nothing has written a CRL to it. This
        // is where the very first CRL for an issuer has to land.
        MemoryBackend backend = backendWithNoCrl();
        X509CRL fresh = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        try (TestHttpServer dp = new TestHttpServer(respond(200, fresh.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRL(cert)).isEqualTo(fresh);
                assertThat(crlIn(backend))
                        .as("the first CRL for an issuer must reach the empty LDAP entry")
                        .isEqualTo(fresh.getEncoded());
            }));
        }
    }

    @Test
    public void anUnverifiableCrlIsReturnedButLeavesTheStoresUntouched() throws Exception {
        // The deliberate asymmetry in getCRL: a CRL that cannot be verified here is still handed
        // back, because PKIX verifies it independently and the CRL already held is expired by
        // construction — needCRLUpdate only asks for a replacement past nextUpdate. What it must
        // not do is outlive the request by reaching either shared store.
        X509CRL stored = expiredCrl(BigInteger.valueOf(3));
        MemoryBackend backend = backendHolding(stored);
        X509CRL fresh = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));

        try (TestHttpServer dp = new TestHttpServer(respond(200, fresh.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            // An unrelated trust anchor, so the issuing CA's key is nowhere to be found.
            withEscapeHatch(() -> withTestTrustStore(ATTACKER_CERT, () -> {
                assertThat(store.getCRL(cert))
                        .as("the fresh CRL is still usable for this validation")
                        .isEqualTo(fresh);
                assertThat(crlIn(backend))
                        .as("but an unverified CRL must not reach the LDAP store")
                        .isEqualTo(stored.getEncoded());
                assertThat(store.getCRLFromCache(cert))
                        .as("nor the CRL cache")
                        .isEqualTo(stored);
            }));
        }
    }

    @Test
    public void aCrlOlderThanTheOneTheLdapStoreHoldsIsNotWrittenOverIt() throws Exception {
        // A cluster, mid-refresh. This node still has CRL #3 cached and expired; another node has
        // already written #5 to the shared entry. The distribution point serves a genuine,
        // correctly signed #4 — newer than what this node compared against, older than what the
        // cluster holds. updateCRL REPLACEs, so writing it would un-revoke cluster-wide every
        // certificate #5 added, until something else refreshes the entry.
        X509CRL stale = expiredCrl(BigInteger.valueOf(3));
        X509CRL newest = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        MemoryBackend backend = backendHolding(newest);

        X509CRL fetched = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(4));

        try (TestHttpServer dp = new TestHttpServer(respond(200, fetched.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            store.updateCRLCache(cert, stale);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRL(cert))
                        .as("the newer CRL the store already holds is the one to use")
                        .isEqualTo(newest);
                assertThat(crlIn(backend))
                        .as("a CRL older than the stored one must not replace it")
                        .isEqualTo(newest.getEncoded());
                assertThat(store.getCRLFromCache(cert))
                        .as("and the stale cache entry that caused this is repaired")
                        .isEqualTo(newest);
            }));
        }
    }

    @Test
    public void aCachedCrlSurvivesAnUnreachableCrlDirectory() throws Exception {
        // The CRL directory is down. A cached CRL is exactly what should carry the deployment
        // through that: returning nothing instead leaves PKIX with no revocation evidence, the
        // status undetermined, and every certificate login failing closed for the length of the
        // outage. Note that getConnection() returns null here rather than throwing — when it
        // throws, getCRL's own catch has always fallen through to the CRL already held.
        X509CRL cached = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(7));

        AMLDAPCertStoreParameters params = new AMLDAPCertStoreParameters("localhost", 389);
        params.setStartLoc(BASE_DN);
        params.setSearchFilter(AMCertStore.setSearchFilter("cn", CA_CN));
        AMCRLStore store = new AMCRLStore(params);
        store.ldapconn = trackedFactory(new LDAPConnectionFactory("127.0.0.1", closedPort()));

        X509Certificate cert = certificateWithDp("http://pki.example.com/ca.crl");
        store.updateCRLCache(cert, cached);

        assertThat(store.getCRL(cert))
                .as("an unreachable CRL directory must not discard the cached CRL")
                .isEqualTo(cached);
    }

    @Test
    public void readingTheStoredCrlLeavesTheSharedCertificateFactoryAlone() throws Exception {
        // AMCertStore's static factory is what other threads are parsing with. Putting a new
        // instance there on every read, through a plain static field, races them and buys
        // nothing a factory of the reader's own would not.
        X509CRL stored = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        MemoryBackend backend = backendHolding(stored);
        X509Certificate cert = certificateWithDp("http://pki.example.com/ca.crl");
        AMCRLStore store = storeBackedBy(backend);
        clearCache(cert);

        withUnusableSharedCertificateFactory(() -> {
            assertThat(store.getCRL(cert)).isEqualTo(stored);
            assertThat(AMCertStore.cf).isSameAs(UNUSABLE_CERTIFICATE_FACTORY);
        });
    }

    @Test
    public void theChainIsNotResolvedWhenNoCrlIsFetched() throws Exception {
        // The chain decides one thing: whether a fetched CRL may be stored. A CRL still inside
        // its nextUpdate is fetched from nowhere, so nothing needs it — and resolving it costs
        // CRLValidator a whole-subtree LDAP search per signed SAML2 message, on an
        // unauthenticated path, for a value nothing would read.
        X509CRL fresh = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(9));
        MemoryBackend backend = backendHolding(fresh);
        X509Certificate cert = certificateWithDp("http://pki.example.com/ca.crl");
        AMCRLStore store = storeBackedBy(backend);
        clearCache(cert);

        AtomicInteger resolutions = new AtomicInteger();
        assertThat(store.getCRLWithLazyChain(cert, () -> {
            resolutions.incrementAndGet();
            return new X509Certificate[]{cert, CA_CERT};
        })).isEqualTo(fresh);
        assertThat(resolutions.get())
                .as("a CRL that needs no refresh must not make the caller resolve a chain")
                .isZero();
    }

    @Test
    public void theChainIsNotResolvedForACrlRejectedOnItsIssuerName() throws Exception {
        // validateFetchedCrl's first and cheapest test is the issuer name, and it reads no key.
        // Resolving the chain to find one costs CRLValidator a whole-subtree LDAP search per
        // signed SAML2 message, on an unauthenticated path, followed by the anchoring walk — for
        // a CRL that is discarded before any of it is looked at.
        X509CRL stale = expiredCrl(BigInteger.valueOf(3));
        MemoryBackend backend = backendHolding(stale);
        KeyPair otherCa = newRsaKeyPair();
        X509CRL foreign = buildCrl("OtherCA", otherCa.getPrivate(), BigInteger.valueOf(5));

        try (TestHttpServer dp = new TestHttpServer(respond(200, foreign.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            AtomicInteger resolutions = new AtomicInteger();
            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRLWithLazyChain(cert, () -> {
                    resolutions.incrementAndGet();
                    return new X509Certificate[]{cert, CA_CERT};
                })).isEqualTo(stale);
                assertThat(resolutions.get())
                        .as("a CRL rejected on its issuer name needs no key, so nothing needs "
                                + "the chain either")
                        .isZero();
            }));
        }
    }

    @Test
    public void theChainIsResolvedOnceWhenACrlIsFetched() throws Exception {
        // The other half: once a CRL really is fetched, the chain is what lets it be verified
        // and therefore stored, so it has to be resolved — once.
        X509CRL stale = expiredCrl(BigInteger.valueOf(3));
        MemoryBackend backend = backendHolding(stale);
        X509CRL newer = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));

        try (TestHttpServer dp = new TestHttpServer(respond(200, newer.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMCRLStore store = storeBackedBy(backend);
            clearCache(cert);

            AtomicInteger resolutions = new AtomicInteger();
            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRLWithLazyChain(cert, () -> {
                    resolutions.incrementAndGet();
                    return new X509Certificate[]{cert, CA_CERT};
                })).isEqualTo(newer);
                assertThat(resolutions.get()).isEqualTo(1);
            }));
        }
    }

    @Test
    public void whatTheChainResolverDoesToTheCallersParametersDoesNotRedirectTheCrlWrite()
            throws Exception {
        // CRLValidator keeps one AMLDAPCertStoreParameters in a static, and its chain resolver
        // looks the issuer up through AMCertStore with that same object. The resolver runs
        // between the two reads the store makes, and the second one — taken when the stale CRL
        // came from the cache — names the entry updateCRL then REPLACEs. A filter written to the
        // caller's parameters in the meantime, by the resolver or by another thread validating
        // a certificate from another issuer, would pick the entry the fresh CRL lands on.
        X509CRL stale = expiredCrl(BigInteger.valueOf(3));
        X509CRL newer = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(5));
        MemoryBackend backend = backendHolding(stale);
        backend.load(new LDIFEntryReader(
                "dn: " + DECOY_DN,
                "objectClass: top",
                "objectClass: applicationProcess",
                "cn: decoy"), false);

        try (TestHttpServer dp = new TestHttpServer(respond(200, newer.getEncoded()))) {
            X509Certificate cert = certificateWithDp(dp.url());
            AMLDAPCertStoreParameters params = new AMLDAPCertStoreParameters("localhost", 389);
            params.setStartLoc(BASE_DN);
            params.setSearchFilter(AMCertStore.setSearchFilter("cn", CA_CN));
            AMCRLStore store = new AMCRLStore(params);
            store.ldapconn = trackedFactory(Connections.<Object>newInternalConnectionFactory(
                    Connections.<Object>newServerConnectionFactory(backend), null));
            store.updateCRLCache(cert, stale);

            withEscapeHatch(() -> withTestTrustStore(CA_CERT, () -> {
                assertThat(store.getCRLWithLazyChain(cert, () -> {
                    params.setSearchFilter(AMCertStore.setSearchFilter("cn", "decoy"));
                    return new X509Certificate[]{cert, CA_CERT};
                })).isEqualTo(newer);
                assertThat(crlIn(backend))
                        .as("the fresh CRL belongs on the issuer's CRL entry")
                        .isEqualTo(newer.getEncoded());
                assertThat(backend.get(DECOY_DN).getAttribute(CRL_ATTRIBUTE))
                        .as("and on no entry another search happened to name")
                        .isNull();
            }));
        }
    }

    @Test
    public void theStaticEntryPointLeavesTheCallersSearchFilterAlone() throws Exception {
        // The same shared object, seen from the other side: whatever filter the static entry
        // point needs, writing it into the caller's parameters hands it to every other search
        // that reads them, on this thread or another.
        AMLDAPCertStoreParameters params = AMCertStore.setLdapStoreParam("127.0.0.1", closedPort(),
                "cn=admin", "secret", BASE_DN, null, false);
        params.setSearchFilter("(cn=the caller's own)");

        AMCRLStore.getCRL(params, certificateWithDp("http://pki.example.com/ca.crl"), "CN");

        assertThat(params.getSearchFilter()).isEqualTo("(cn=the caller's own)");
    }

    private static final String DECOY_DN = "cn=decoy," + BASE_DN;

    /** A loopback port nothing listens on, so connecting to it is refused rather than hanging. */
    private static int closedPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static void withEscapeHatch(CertAction action) throws Exception {
        // The distribution point is on loopback, which the default address policy blocks; the
        // behaviour under test here is what happens to a CRL that was fetched.
        withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", action);
    }

    private static X509CRL expiredCrl(BigInteger crlNumber) throws Exception {
        return expiredCrl(CA_CN, CA_KEY_PAIR.getPrivate(), crlNumber);
    }

    private static X509CRL expiredCrl(String issuerCn, PrivateKey issuerKey, BigInteger crlNumber)
            throws Exception {
        // Already past nextUpdate, so needCRLUpdate() asks for a refetch.
        Date yesterday = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        return buildCrl(issuerCn, issuerKey, crlNumber, yesterday);
    }

    private static X509Certificate certificateWithDp(String dpUrl) throws Exception {
        KeyPair keyPair = newRsaKeyPair();
        return buildCertificate("subject", keyPair.getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                BigInteger.valueOf(11), false, dpUrl);
    }

    private static void clearCache(X509Certificate cert) {
        // The CRL cache is JVM-wide static state shared by every test in this module.
        new AMCRLStore(new AMLDAPCertStoreParameters("localhost", 389)).updateCRLCache(cert, null);
    }

    private static MemoryBackend backendHolding(X509CRL crl) throws Exception {
        return backendHolding(crl, CA_CN);
    }

    private static MemoryBackend backendHolding(X509CRL crl, String issuerCn) throws Exception {
        return new MemoryBackend(new LDIFEntryReader(
                "dn: " + BASE_DN,
                "objectClass: top",
                "objectClass: domain",
                "dc: example",
                "",
                "dn: cn=" + issuerCn + "," + BASE_DN,
                "objectClass: top",
                "objectClass: cRLDistributionPoint",
                "cn: " + issuerCn,
                CRL_ATTRIBUTE + ":: " + Base64.getEncoder().encodeToString(crl.getEncoded())));
    }

    private static MemoryBackend backendWithNoCrl() throws Exception {
        return new MemoryBackend(new LDIFEntryReader(
                "dn: " + BASE_DN,
                "objectClass: top",
                "objectClass: domain",
                "dc: example",
                "",
                "dn: " + CRL_ENTRY_DN,
                "objectClass: top",
                "objectClass: cRLDistributionPoint",
                "cn: " + CA_CN));
    }

    private <T extends ConnectionFactory> T trackedFactory(T factory) {
        factories.add(factory);
        return factory;
    }

    private AMCRLStore storeBackedBy(RequestHandler<RequestContext> backend) {
        return storeBackedBy(backend, CA_CN);
    }

    private AMCRLStore storeBackedBy(RequestHandler<RequestContext> backend, String issuerCn) {
        AMLDAPCertStoreParameters params = new AMLDAPCertStoreParameters("localhost", 389);
        params.setStartLoc(BASE_DN);
        params.setSearchFilter(AMCertStore.setSearchFilter("cn", issuerCn));

        AMCRLStore store = new AMCRLStore(params);
        // AMCertStore only builds an LDAPConnectionFactory when this field is unset, so an
        // in-JVM connection factory over the memory backend takes its place — no sockets, no
        // bind, and the modify requests the store issues are really applied.
        store.ldapconn = trackedFactory(Connections.<Object>newInternalConnectionFactory(
                Connections.<Object>newServerConnectionFactory(backend), null));
        return store;
    }

    @Test
    public void theVarargsEntryPointStaysUnambiguousForCallersPassingNull() throws Exception {
        // AMCRLStore is published API in com.sun.identity.security.cert, and getCRL(param, cert,
        // null) is how an out-of-tree caller spells "no attribute names". Adding the overload
        // that takes a chain does not break it: the three-argument form is applicable by fixed
        // arity, with String... read as String[] (JLS 15.12.2.2), and the chain overload needs
        // four arguments to get that far — so resolution never reaches the variable-arity phase
        // where the two could compete. This call is the guard against that changing.
        assertThat(AMCRLStore.getCRL(null, EE_CERT, null)).isNull();
    }

    @Test
    public void theConstructorStillAcceptsNoParameters() throws Exception {
        // Published API: the store now copies its parameters, and copying null must not turn a
        // construction that used to succeed into a NullPointerException.
        AMCRLStore store = new AMCRLStore(null);

        // An issuer no other test caches a CRL for: the cache is shared by the whole JVM.
        KeyPair uncachedCa = newRsaKeyPair();
        X509Certificate cert = buildCertificate("user", newRsaKeyPair().getPublic(), "UncachedCA",
                uncachedCa.getPrivate(), BigInteger.TEN, false, (String) null);
        assertThat(store.getCRLFromCache(cert)).isNull();
    }

    private static byte[] crlIn(MemoryBackend backend) {
        return crlIn(backend, CA_CN);
    }

    private static byte[] crlIn(MemoryBackend backend, String issuerCn) {
        return backend.get("cn=" + issuerCn + "," + BASE_DN)
                .getAttribute(CRL_ATTRIBUTE).firstValue().toByteArray();
    }
}
