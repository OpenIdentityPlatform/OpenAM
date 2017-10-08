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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.jwtgenerator;

import static org.forgerock.json.JsonValue.*;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class JwtGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: JwtGenerator <subject> <issuer> <audience>");
            System.exit(1);
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair keyPair = keyGen.genKeyPair();
        PublicKey publicKey = keyPair.getPublic();

        long validTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24 / 2;
        String jwt = new JwtBuilderFactory().jws(new SigningManager().newRsaSigningHandler(keyPair.getPrivate()))
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(new JwtClaimsSet(json(object(
                        field("iss", args[0]),
                        field("sub", args[1]),
                        field("aud", args[2]),
                        field("exp", validTime / 1000)
                )).asMap()))
                .build();
        System.out.println("JWT: " + jwt);

        Calendar expiry = org.forgerock.openam.utils.Time.getCalendarInstance();
        expiry.add(Calendar.DAY_OF_YEAR, 7);

        X509CertInfo info = new X509CertInfo();
        CertificateValidity interval = new CertificateValidity(org.forgerock.openam.utils.Time.newDate(), new Date(validTime));
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name("CN=ForgeRock,L=Bristol,C=GB");

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
        info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
        info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(keyPair.getPrivate(), "SHA256withRSA");
        System.out.println("Certificate:");
        BASE64Encoder encoder = new BASE64Encoder();
        System.out.println(X509Factory.BEGIN_CERT);
        encoder.encodeBuffer(cert.getEncoded(), System.out);
        System.out.println(X509Factory.END_CERT);
    }
}
