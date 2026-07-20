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

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openidentityplatform.bouncycastle.asn1.x500.X500Name;
import org.openidentityplatform.bouncycastle.asn1.x509.BasicConstraints;
import org.openidentityplatform.bouncycastle.asn1.x509.CRLDistPoint;
import org.openidentityplatform.bouncycastle.asn1.x509.DistributionPoint;
import org.openidentityplatform.bouncycastle.asn1.x509.DistributionPointName;
import org.openidentityplatform.bouncycastle.asn1.x509.Extension;
import org.openidentityplatform.bouncycastle.asn1.x509.GeneralName;
import org.openidentityplatform.bouncycastle.asn1.x509.GeneralNames;
import org.openidentityplatform.bouncycastle.asn1.x509.KeyUsage;
import org.openidentityplatform.bouncycastle.cert.X509CertificateHolder;
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
 *   <li>{@link #ATTACKER_CERT} — self-signed (CN=attacker) whose CRL Distribution Point
 *       is a loopback {@link ServerSocket} opened at class-load time. Use {@link #withNoFetch}
 *       in negative tests to assert that the socket receives zero connections.</li>
 * </ul>
 */
abstract class CertTestBase {

    static final X509Certificate CA_CERT;
    static final X509Certificate EE_CERT;
    static final X509Certificate ATTACKER_CERT;
    private static final ServerSocket ATTACKER_CRL_LISTENER;

    static {
        try {
            KeyPair caKeyPair = newRsaKeyPair();
            CA_CERT = buildCertificate("TestCA", caKeyPair.getPublic(),
                    "TestCA", caKeyPair.getPrivate(), BigInteger.ONE, true, null);

            KeyPair eeKeyPair = newRsaKeyPair();
            EE_CERT = buildCertificate("user", eeKeyPair.getPublic(),
                    "TestCA", caKeyPair.getPrivate(), BigInteger.valueOf(2), false, null);

            ATTACKER_CRL_LISTENER = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            String crlDpUrl = "http://127.0.0.1:" + ATTACKER_CRL_LISTENER.getLocalPort() + "/crl";
            KeyPair attackerKeyPair = newRsaKeyPair();
            ATTACKER_CERT = buildCertificate("attacker", attackerKeyPair.getPublic(),
                    "attacker", attackerKeyPair.getPrivate(), BigInteger.valueOf(3), false,
                    crlDpUrl);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static KeyPair newRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static X509Certificate buildCertificate(String subjectCn, PublicKey subjectKey,
            String issuerCn, PrivateKey issuerKey, BigInteger serial, boolean ca, String crlDpUri)
            throws Exception {
        Date notBefore = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        Date notAfter = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365L * 10));

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=" + issuerCn), serial, notBefore, notAfter,
                new X500Name("CN=" + subjectCn), subjectKey);

        if (ca) {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        }
        if (crlDpUri != null) {
            GeneralName uri = new GeneralName(GeneralName.uniformResourceIdentifier, crlDpUri);
            DistributionPoint dp = new DistributionPoint(
                    new DistributionPointName(new GeneralNames(uri)), null, null);
            builder.addExtension(Extension.cRLDistributionPoints, false,
                    new CRLDistPoint(new DistributionPoint[]{dp}));
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey);
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    /**
     * Runs {@code action} (which should exercise {@link #ATTACKER_CERT}), then asserts that
     * {@link #ATTACKER_CRL_LISTENER} received zero connections — directly proving the SSRF
     * guard fires before any CRL Distribution Point fetch is attempted.
     */
    protected static void withNoFetch(CertAction action) throws Exception {
        action.run();
        ATTACKER_CRL_LISTENER.setSoTimeout(300);
        try {
            ATTACKER_CRL_LISTENER.accept().close();
            throw new AssertionError(
                    "SSRF guard regression: CRL DP was fetched before trust-chain check rejected the cert");
        } catch (SocketTimeoutException ignored) {
            // expected: no connection was made to the CRL DP
        }
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
        KeyStore testTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        testTrustStore.load(null, null);
        testTrustStore.setCertificateEntry("testca", anchor);

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

    @FunctionalInterface
    protected interface CertAction {
        void run() throws Exception;
    }
}
