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
 * Copyright 2014 ForgeRock AS.
 */
package com.iplanet.security.x509;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.*;

public class CertUtilsTest {

    private static final String CERT_PATH = "/cert/svn.forgerock.org.der";
    private static final String COMPLEX_CERT_PATH = "/cert/complex-cert.der";
    private static final String ISSUER_CN = "StartCom Class 1 Primary Intermediate Server CA";
    private static final String ISSUER_OU = "Secure Digital Certificate Signing";
    private static final String ISSUER_O = "StartCom Ltd.";
    private static final String ISSUER_C = "IL";
    private static final String ISSUER_NAME = "CN=" + ISSUER_CN + ",OU=" + ISSUER_OU + ",O=" + ISSUER_O
            + ",C=" + ISSUER_C;
    private static final String SUBJECT_EMAIL = "postmaster@forgerock.org";
    private static final String SUBJECT_CN = "svn.forgerock.org";
    private static final String SUBJECT_C = "GB";
    private static final String SUBJECT_NAME = "E=" + SUBJECT_EMAIL + ",CN=" + SUBJECT_CN + ",C=" + SUBJECT_C
            + ",2.5.4.13=#131042723878424475366132364930453265";
    private static final String DUMMY_CN = "hello";
    private static final String DUMMY_UID = "world";
    private static final String DUMMY_DC = "com";
    private static final X500Principal MULTI_VALUED_PRINCIPAL
            = new X500Principal("cn=" + DUMMY_CN + "+uid=" + DUMMY_UID + ",dc=internal,dc=forgerock,dc=" + DUMMY_DC);
    private static X509Certificate cert;

    @BeforeClass
    public void setup() throws Exception {
        cert = getCertificate(CERT_PATH);
    }

    private X509Certificate getCertificate(String path) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(getClass().getResourceAsStream(path));
    }

    @Test
    public void issuerNameIsCorrectlyRetrieved() {
        assertThat(CertUtils.getIssuerName(cert)).isEqualTo(ISSUER_NAME);
    }

    @Test
    public void subjectNameIsCorrectlyRetrieved() {
        assertThat(CertUtils.getSubjectName(cert)).isEqualTo(SUBJECT_NAME);
    }

    @Test
    public void attributesAreCorrectlyRetrievedFromX500Principals() {
        assertThat(CertUtils.getAttributeValue(cert.getIssuerX500Principal(), "cn")).isEqualTo(ISSUER_CN);
        assertThat(CertUtils.getAttributeValue(cert.getIssuerX500Principal(), "ou")).isEqualTo(ISSUER_OU);
        assertThat(CertUtils.getAttributeValue(cert.getIssuerX500Principal(), "o")).isEqualTo(ISSUER_O);
        assertThat(CertUtils.getAttributeValue(cert.getIssuerX500Principal(), "c")).isEqualTo(ISSUER_C);
        assertThat(CertUtils.getAttributeValue(cert.getSubjectX500Principal(), "e"))
                .isEqualTo(SUBJECT_EMAIL);
        assertThat(CertUtils.getAttributeValue(cert.getSubjectX500Principal(), "cn")).isEqualTo(SUBJECT_CN);
        assertThat(CertUtils.getAttributeValue(cert.getSubjectX500Principal(), "c")).isEqualTo(SUBJECT_C);
    }

    @Test
    public void multiValuedRDNsAreCorrectlyHandled() {
        assertThat(CertUtils.getAttributeValue(MULTI_VALUED_PRINCIPAL, "cn")).isEqualTo(DUMMY_CN);
        assertThat(CertUtils.getAttributeValue(MULTI_VALUED_PRINCIPAL, "uid")).isEqualTo(DUMMY_UID);
    }

    @Test
    public void topLevelValueReturnedWhenAttributeIsDefinedMultipleTimes() {
        assertThat(CertUtils.getAttributeValue(MULTI_VALUED_PRINCIPAL, "dc")).isEqualTo(DUMMY_DC);
    }

    @Test
    public void testComplexSubjectDN() throws Exception {
        X509Certificate complexCert = getCertificate(COMPLEX_CERT_PATH);
        X500Principal principal = complexCert.getSubjectX500Principal();
        assertThat(CertUtils.getAttributeValue(principal, "givenName")).isEqualTo("Barbara");
        assertThat(CertUtils.getAttributeValue(principal, "sn")).isEqualTo("Jensen");
        assertThat(CertUtils.getAttributeValue(principal, "serialNumber")).isEqualTo("123");
        assertThat(CertUtils.getAttributeValue(principal, "street")).isEqualTo("Anystreet");
        assertThat(CertUtils.getAttributeValue(principal, "title")).isEqualTo("CEO");
        assertThat(CertUtils.getAttributeValue(principal, "uid")).isEqualTo("bjensen");
        assertThat(CertUtils.getAttributeValue(principal, "dc")).isEqualTo("Foo");
        assertThat(CertUtils.getAttributeValue(principal, "initials")).isEqualTo("BJ");
        assertThat(CertUtils.getAttributeValue(principal, "generationQualifier")).isEqualTo("I");
        assertThat(CertUtils.getAttributeValue(principal, "dnQualifier")).isEqualTo("123456A");
        assertThat(CertUtils.getAttributeValue(principal, "unstructuredName")).isEqualTo("Jensen Barbara");
        assertThat(CertUtils.getAttributeValue(principal, "unstructuredAddress")).isEqualTo("streetAny");
        assertThat(CertUtils.getAttributeValue(principal, "c")).isEqualTo("US");
        assertThat(CertUtils.getAttributeValue(principal, "st")).isEqualTo("California");
        assertThat(CertUtils.getAttributeValue(principal, "l")).isEqualTo("San Francisco");
        assertThat(CertUtils.getAttributeValue(principal, "o")).isEqualTo("Demo");
        assertThat(CertUtils.getAttributeValue(principal, "ou")).isEqualTo("Product Development");
        assertThat(CertUtils.getAttributeValue(principal, "cn")).isEqualTo("Babs Jensen");
        assertThat(CertUtils.getAttributeValue(principal, "e")).isEqualTo("bjensen@example.com");
    }
}
