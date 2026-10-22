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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateFactorySpi;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openidentityplatform.bouncycastle.asn1.x500.X500Name;
import org.openidentityplatform.bouncycastle.asn1.x509.BasicConstraints;
import org.openidentityplatform.bouncycastle.asn1.x509.CRLDistPoint;
import org.openidentityplatform.bouncycastle.asn1.x509.CRLNumber;
import org.openidentityplatform.bouncycastle.asn1.x509.DistributionPoint;
import org.openidentityplatform.bouncycastle.asn1.x509.DistributionPointName;
import org.openidentityplatform.bouncycastle.asn1.x509.Extension;
import org.openidentityplatform.bouncycastle.asn1.x509.GeneralName;
import org.openidentityplatform.bouncycastle.asn1.x509.GeneralNames;
import org.openidentityplatform.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.openidentityplatform.bouncycastle.asn1.x509.KeyUsage;
import org.openidentityplatform.bouncycastle.cert.X509CRLHolder;
import org.openidentityplatform.bouncycastle.cert.X509CertificateHolder;
import org.openidentityplatform.bouncycastle.cert.X509v2CRLBuilder;
import org.openidentityplatform.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.openidentityplatform.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.openidentityplatform.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.openidentityplatform.bouncycastle.operator.ContentSigner;
import org.openidentityplatform.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.sun.identity.security.keystore.AMX509TrustManager;

/**
 * Shared test certificates for the CRL Distribution Point SSRF-guard unit tests.
 *
 * <p>The certificates are generated at runtime (rather than embedded as fixed DER) with a
 * validity of now-1day..now+10years, so the tests never expire and do not depend on the
 * wall-clock date.
 *
 * <ul>
 *   <li>{@link #CA_CERT} — self-signed CA (CN=TestCA).</li>
 *   <li>{@link #EE_CERT} — end-entity (CN=user) signed by {@code CA_CERT}, no CRL DP.</li>
 *   <li>{@link #ATTACKER_CERT} — self-signed (CN=attacker), for the tests that need a
 *       certificate outside the trust store. It names no distribution point: to assert that no
 *       fetch was attempted, use {@link #withNoFetch}, which mints a certificate and a listener
 *       of its own.</li>
 * </ul>
 *
 * <p><strong>These tests must run single-threaded.</strong> {@link #withTestTrustStore} and
 * {@link #withSystemProperty} both swap JVM-global state for the duration of a call, and
 * {@code AMCRLStore}'s CRL cache is a static shared by every test in the module. The root
 * pom's surefire configuration sets no {@code parallel} mode, so this holds today; enabling
 * parallel methods would make these tests interfere with each other.
 */
abstract class CertTestBase {

    static final String CA_CN = "TestCA";

    static final KeyPair CA_KEY_PAIR;
    static final X509Certificate CA_CERT;
    static final X509Certificate EE_CERT;
    static final X509Certificate ATTACKER_CERT;

    static {
        try {
            KeyPair caKeyPair = newRsaKeyPair();
            CA_KEY_PAIR = caKeyPair;
            CA_CERT = buildCertificate(CA_CN, caKeyPair.getPublic(),
                    CA_CN, caKeyPair.getPrivate(), BigInteger.ONE, true, null);

            KeyPair eeKeyPair = newRsaKeyPair();
            EE_CERT = buildCertificate("user", eeKeyPair.getPublic(),
                    CA_CN, caKeyPair.getPrivate(), BigInteger.valueOf(2), false, null);

            // No distribution point: both remaining uses only need a self-signed certificate
            // that is not in the trust store, and reserving a port for a URL nothing fetches
            // meant a ServerSocket held open for the life of the surefire JVM.
            KeyPair attackerKeyPair = newRsaKeyPair();
            ATTACKER_CERT = buildCertificate("attacker", attackerKeyPair.getPublic(),
                    "attacker", attackerKeyPair.getPrivate(), BigInteger.valueOf(3), false,
                    null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static KeyPair newRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    static X509Certificate buildCertificate(String subjectCn, PublicKey subjectKey,
            String issuerCn, PrivateKey issuerKey, BigInteger serial, boolean ca, String crlDpUri)
            throws Exception {
        GeneralName[] dpNames = (crlDpUri == null) ? new GeneralName[0]
                : new GeneralName[]{new GeneralName(GeneralName.uniformResourceIdentifier, crlDpUri)};
        return buildCertificateWithDps(subjectCn, subjectKey, issuerCn, issuerKey, serial, ca,
                dpNames);
    }

    /**
     * Builds a certificate carrying one CRL Distribution Point per supplied {@link GeneralName},
     * so tests can mint the non-URI name types ({@code rfc822Name}, {@code dNSName}, …) that the
     * distribution point parser must ignore, and multi-DP certificates.
     */
    static X509Certificate buildCertificateWithDps(String subjectCn, PublicKey subjectKey,
            String issuerCn, PrivateKey issuerKey, BigInteger serial, boolean ca,
            GeneralName... crlDpNames) throws Exception {
        return buildCertificateWithKeyUsage(subjectCn, subjectKey, issuerCn, issuerKey, serial, ca,
                ca ? Integer.valueOf(KeyUsage.keyCertSign | KeyUsage.cRLSign) : null, crlDpNames);
    }

    /**
     * Builds a certificate with an explicit {@code keyUsage}, so tests can mint the shapes a real
     * PKI produces but the convenience builders do not: a CA that may sign certificates but not
     * CRLs, or a CA with no {@code keyUsage} extension at all.
     *
     * @param ca value of the {@code basicConstraints} CA flag.
     * @param keyUsageBits {@link KeyUsage} bits, or {@code null} to omit the extension entirely.
     */
    static X509Certificate buildCertificateWithKeyUsage(String subjectCn, PublicKey subjectKey,
            String issuerCn, PrivateKey issuerKey, BigInteger serial, boolean ca,
            Integer keyUsageBits, GeneralName... crlDpNames) throws Exception {
        Date notBefore = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        Date notAfter = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365L * 10));

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=" + issuerCn), serial, notBefore, notAfter,
                new X500Name("CN=" + subjectCn), subjectKey);

        if (ca) {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        }
        if (keyUsageBits != null) {
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsageBits.intValue()));
        }
        if (crlDpNames != null && crlDpNames.length > 0) {
            DistributionPoint[] dps = new DistributionPoint[crlDpNames.length];
            for (int i = 0; i < crlDpNames.length; i++) {
                dps[i] = new DistributionPoint(
                        new DistributionPointName(new GeneralNames(crlDpNames[i])), null, null);
            }
            builder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(dps));
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey);
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    /**
     * Builds a certificate carrying a single CRL Distribution Point whose {@code fullName} holds
     * every supplied {@link GeneralName} — the RFC 5280 shape for one distribution point that is
     * reachable at several locations.
     */
    static X509Certificate buildCertificateWithMultiNameDp(String subjectCn, PublicKey subjectKey,
            String issuerCn, PrivateKey issuerKey, BigInteger serial, GeneralName... crlDpNames)
            throws Exception {
        Date notBefore = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        Date notAfter = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365L * 10));

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=" + issuerCn), serial, notBefore, notAfter,
                new X500Name("CN=" + subjectCn), subjectKey);

        DistributionPoint dp = new DistributionPoint(
                new DistributionPointName(new GeneralNames(crlDpNames)), null, null);
        builder.addExtension(Extension.cRLDistributionPoints, false,
                new CRLDistPoint(new DistributionPoint[]{dp}));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey);
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    /**
     * Builds a certificate whose {@code cRLDistributionPoints} holds the supplied distribution
     * points verbatim, so a test can mint the RFC 5280 shapes the convenience builders cannot:
     * a point that carries only a {@code cRLIssuer}, or a {@code nameRelativeToCRLIssuer}.
     * Neither names a location, so {@code DistributionPoint.getFullName()} is null for them.
     */
    static X509Certificate buildCertificateWithRawDps(String subjectCn, PublicKey subjectKey,
            String issuerCn, PrivateKey issuerKey, BigInteger serial, DistributionPoint... dps)
            throws Exception {
        Date notBefore = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        Date notAfter = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365L * 10));

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=" + issuerCn), serial, notBefore, notAfter,
                new X500Name("CN=" + subjectCn), subjectKey);
        builder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(dps));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey);
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /** A distribution point that names a CRL issuer but no location to fetch from. */
    static DistributionPoint crlIssuerOnlyDp(String crlIssuerCn) {
        return new DistributionPoint(null, null,
                new GeneralNames(new GeneralName(GeneralName.directoryName,
                        new X500Name("CN=" + crlIssuerCn))));
    }

    /**
     * A distribution point that names both a location and the CRL issuer the certificate
     * delegates to — the RFC 5280 shape of a PKI whose CRLs are signed by a delegated signer,
     * and the only shape for which an indirect CRL may be used at all.
     */
    static DistributionPoint uriDpWithCrlIssuer(String url, String crlIssuerCn) {
        return new DistributionPoint(new DistributionPointName(new GeneralNames(uri(url))), null,
                new GeneralNames(new GeneralName(GeneralName.directoryName,
                        new X500Name("CN=" + crlIssuerCn))));
    }

    /** An ordinary distribution point: one location, named by URI. */
    static DistributionPoint uriDp(String url) {
        return new DistributionPoint(new DistributionPointName(new GeneralNames(uri(url))),
                null, null);
    }

    /**
     * Builds an indirect CRL (RFC 5280 section 5.2.5): one whose issuer is a delegated CRL
     * signer rather than the CA that issued the certificates it covers, marked as such by the
     * {@code indirectCRL} flag of its {@code IssuingDistributionPoint}.
     */
    static X509CRL buildIndirectCrl(String signerCn, PrivateKey signerKey, BigInteger crlNumber)
            throws Exception {
        Date thisUpdate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));

        X509v2CRLBuilder builder = new X509v2CRLBuilder(new X500Name("CN=" + signerCn), thisUpdate);
        builder.setNextUpdate(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)));
        if (crlNumber != null) {
            builder.addExtension(Extension.cRLNumber, false, new CRLNumber(crlNumber));
        }
        builder.addExtension(Extension.issuingDistributionPoint, true,
                new IssuingDistributionPoint(null, false, false, null, true, false));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(signerKey);
        return new JcaX509CRLConverter().getCRL(builder.build(signer));
    }

    /**
     * Builds a CRL issued by {@code CN=issuerCn} and signed with {@code issuerKey}, valid from
     * yesterday until 30 days from now.
     *
     * @param crlNumber value of the {@code CRLNumber} extension, or {@code null} to omit it.
     */
    static X509CRL buildCrl(String issuerCn, PrivateKey issuerKey, BigInteger crlNumber)
            throws Exception {
        return buildCrl(issuerCn, issuerKey, crlNumber,
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)));
    }

    /**
     * @param nextUpdate the CRL's {@code nextUpdate}; a date in the past makes
     *     {@code AMCRLStore.needCRLUpdate()} ask for a refetch.
     */
    static X509CRL buildCrl(String issuerCn, PrivateKey issuerKey, BigInteger crlNumber,
            Date nextUpdate) throws Exception {
        return buildCrl(issuerCn, issuerKey, crlNumber, nextUpdate, new GeneralName[0]);
    }

    /**
     * @param idpNames locations for the CRL's own {@code IssuingDistributionPoint} extension —
     *     where {@code AMCRLStore} looks for a replacement once this CRL has expired.
     */
    static X509CRL buildCrl(String issuerCn, PrivateKey issuerKey, BigInteger crlNumber,
            Date nextUpdate, GeneralName... idpNames) throws Exception {
        Date thisUpdate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));

        X509v2CRLBuilder builder = new X509v2CRLBuilder(new X500Name("CN=" + issuerCn), thisUpdate);
        builder.setNextUpdate(nextUpdate);
        if (crlNumber != null) {
            builder.addExtension(Extension.cRLNumber, false, new CRLNumber(crlNumber));
        }
        if (idpNames != null && idpNames.length > 0) {
            builder.addExtension(Extension.issuingDistributionPoint, true,
                    new IssuingDistributionPoint(
                            new DistributionPointName(new GeneralNames(idpNames)),
                            false, false, null, false, false));
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey);
        X509CRLHolder holder = builder.build(signer);
        return new JcaX509CRLConverter().getCRL(holder);
    }

    /**
     * Builds a CRL with an explicit {@code thisUpdate}, for tests about the ordering of two CRLs
     * from the same issuer.
     */
    static X509CRL buildCrlIssuedAt(String issuerCn, PrivateKey issuerKey, BigInteger crlNumber,
            Date thisUpdate) throws Exception {
        X509v2CRLBuilder builder = new X509v2CRLBuilder(new X500Name("CN=" + issuerCn), thisUpdate);
        builder.setNextUpdate(new Date(thisUpdate.getTime() + TimeUnit.DAYS.toMillis(30)));
        if (crlNumber != null) {
            builder.addExtension(Extension.cRLNumber, false, new CRLNumber(crlNumber));
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey);
        return new JcaX509CRLConverter().getCRL(builder.build(signer));
    }

    /** A {@code uniformResourceIdentifier} {@link GeneralName}, the usual CRL location type. */
    static GeneralName uri(String value) {
        return new GeneralName(GeneralName.uniformResourceIdentifier, value);
    }

    /**
     * Hands {@code action} a freshly minted self-signed certificate whose CRL Distribution Point
     * is a loopback listener opened for this call alone, then asserts that the listener saw no
     * connection — directly proving the SSRF guard fires before any fetch is attempted.
     *
     * <p>Per call, not once per JVM: a listener shared by the whole module carries any
     * connection an earlier test made over into the next assertion, so one real regression would
     * be reported against an unrelated test and the test that caused it would pass.
     */
    protected static void withNoFetch(AttackerCertAction action) throws Exception {
        try (ConnectionDetector listener = new ConnectionDetector()) {
            KeyPair keyPair = newRsaKeyPair();
            X509Certificate attackerCert = buildCertificate("attacker", keyPair.getPublic(),
                    "attacker", keyPair.getPrivate(), BigInteger.valueOf(3), false,
                    listener.url());

            action.run(attackerCert);

            if (listener.sawConnection()) {
                throw new AssertionError("SSRF guard regression: the CRL DP was fetched before "
                        + "the trust-chain check rejected the certificate");
            }
        }
    }

    /** A {@link CertAction} handed the certificate {@link #withNoFetch} built for it. */
    protected interface AttackerCertAction {
        void run(X509Certificate attackerCert) throws Exception;
    }

    /**
     * Runs {@code action} with {@code anchor} installed as the sole trust anchor of
     * {@link AMX509TrustManager}, restoring the original trust store afterwards.
     *
     * <p>{@code AMX509TrustManager} loads its trust store into a package-private static field
     * with no injection point, so the field is set via reflection. Centralised here to avoid
     * duplicating the reflection in each test.
     */
    protected static void withTestTrustStore(X509Certificate anchor, CertAction action)
            throws Exception {
        withTestTrustStore(Collections.singletonList(anchor), action);
    }

    /**
     * As {@link #withTestTrustStore(X509Certificate, CertAction)}, but installs several anchors —
     * the shape a CA in the middle of a key rollover produces, where two certificates carry one
     * subject name.
     */
    protected static void withTestTrustStore(List<X509Certificate> anchors, CertAction action)
            throws Exception {
        KeyStore testTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        testTrustStore.load(null, null);
        for (int i = 0; i < anchors.size(); i++) {
            testTrustStore.setCertificateEntry("testca" + i, anchors.get(i));
        }

        Field trustKeyStoreField = AMX509TrustManager.class.getDeclaredField("trustKeyStore");
        trustKeyStoreField.setAccessible(true);
        KeyStore original = (KeyStore) trustKeyStoreField.get(null);
        trustKeyStoreField.set(null, testTrustStore);
        try {
            action.run();
        } finally {
            trustKeyStoreField.set(null, original);
        }
    }

    /**
     * Runs {@code action} with the JVM system property {@code key} set to {@code value},
     * restoring its previous value afterwards.
     *
     * <p>{@code SystemPropertiesManager} has no configuration provider on this module's test
     * classpath, so {@code AMCRLStore} falls back to plain JVM system properties — the same
     * fallback an operator gets from a {@code -D} flag.
     */
    protected static void withSystemProperty(String key, String value, CertAction action)
            throws Exception {
        String original = System.getProperty(key);
        System.setProperty(key, value);
        try {
            action.run();
        } finally {
            if (original == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, original);
            }
        }
    }

    /** A certificate factory that refuses everything it is handed. */
    static final CertificateFactory UNUSABLE_CERTIFICATE_FACTORY = new CertificateFactory(
            new CertificateFactorySpi() {
                @Override
                public Certificate engineGenerateCertificate(InputStream in)
                        throws CertificateException {
                    throw new CertificateException("unusable");
                }

                @Override
                public Collection<? extends Certificate> engineGenerateCertificates(
                        InputStream in) throws CertificateException {
                    throw new CertificateException("unusable");
                }

                @Override
                public CRL engineGenerateCRL(InputStream in) throws CRLException {
                    throw new CRLException("unusable");
                }

                @Override
                public Collection<? extends CRL> engineGenerateCRLs(InputStream in)
                        throws CRLException {
                    throw new CRLException("unusable");
                }
            }, null, "X.509") { };

    /**
     * Runs {@code action} with {@code AMCertStore}'s static {@link CertificateFactory} replaced
     * by {@link #UNUSABLE_CERTIFICATE_FACTORY}, restoring it afterwards — so a test can tell
     * whether the code under it parses with, or writes to, that shared instance.
     */
    protected static void withUnusableSharedCertificateFactory(CertAction action)
            throws Exception {
        CertificateFactory original = AMCertStore.cf;
        AMCertStore.cf = UNUSABLE_CERTIFICATE_FACTORY;
        try {
            action.run();
        } finally {
            AMCertStore.cf = original;
        }
    }

    @FunctionalInterface
    protected interface CertAction {
        void run() throws Exception;
    }

    /**
     * A loopback TCP listener that counts connections and closes them straight away, so a
     * client that should not have connected fails fast instead of blocking the test.
     */
    static final class ConnectionDetector implements Closeable {

        /**
         * How long {@link #sawConnection()} waits for the connection it expects not to find. The
         * TCP handshake of a connection the code under test made completed before that call
         * returned, so the wait is not what makes the answer right — it only has to be longer
         * than the accept() syscall on an empty queue.
         */
        private static final int DRAIN_TIMEOUT_MS = 250;

        private final ServerSocket listener;

        ConnectionDetector() throws IOException {
            listener = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        }

        String url() {
            return "http://127.0.0.1:" + listener.getLocalPort() + "/crl";
        }

        int port() {
            return listener.getLocalPort();
        }

        /**
         * Whether anything connected to this listener while the call under test ran.
         *
         * <p>Asked of the kernel's accept queue from the test thread, not of a counter an
         * acceptor thread keeps. A connection whose handshake the kernel completed is queued
         * whether or not any thread has accepted it yet, so this answer does not depend on the
         * scheduler; a counter incremented by a background thread reads zero for a connection
         * that was really made whenever that thread has not run yet, and reports a genuine SSRF
         * regression as a pass.
         */
        boolean sawConnection() throws IOException {
            listener.setSoTimeout(DRAIN_TIMEOUT_MS);
            try (Socket accepted = listener.accept()) {
                return true;
            } catch (SocketTimeoutException e) {
                return false;
            }
        }

        @Override
        public void close() throws IOException {
            listener.close();
        }
    }
}
