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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.statements;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactoryImpl;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.openam.utils.Time.*;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class DefaultSubjectProviderTest {
    private static final String KEY_INFO_CONFIRMATION_DATA_TYPE = SAML2Constants.ASSERTION_PREFIX + "KeyInfoConfirmationDataType";
    private static final String AUDIENCE_ID = "http://host.com:8080/openam/Consumer/metaAlias/sp";
    private static final String SUBJECT_ID = "bobo";
    private static final int TOKEN_LIFETIME_SECONDS = 600;
    private static final String NAME_ID_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
    private static final String X_509 = "X.509";

    class MyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(KeyInfoFactory.class).to(KeyInfoFactoryImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
        }
    }

    @Test
    public void testBearerStateSettings() throws TokenCreationException {
        SubjectProvider subjectProvider =
                new DefaultSubjectProvider(Guice.createInjector(new MyModule()).getInstance(KeyInfoFactory.class));
        Date issueInstant = newDate();
        ProofTokenState proof = null; //must be set only when SubjectConfirmation is HoK
        Subject subject = subjectProvider.get(SUBJECT_ID, AUDIENCE_ID, createSAML2Config(),
                SAML2SubjectConfirmation.BEARER, issueInstant, proof);
        assertTrue(SUBJECT_ID.equals(subject.getNameID().getValue()));
        assertTrue(NAME_ID_FORMAT.equals(subject.getNameID().getFormat()));

        SubjectConfirmation subjectConfirmation = (SubjectConfirmation)subject.getSubjectConfirmation().get(0);
        assertTrue(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER.equals(subjectConfirmation.getMethod()));
        SubjectConfirmationData subjectConfirmationData = subjectConfirmation.getSubjectConfirmationData();
        assertTrue((issueInstant.getTime() + (TOKEN_LIFETIME_SECONDS * 1000)) == subjectConfirmationData.getNotOnOrAfter().getTime());
    }

    @Test
    public void testHoKSubjectConfirmation() throws Exception {
        SubjectProvider subjectProvider =
                new DefaultSubjectProvider(Guice.createInjector(new MyModule()).getInstance(KeyInfoFactory.class));
        Date issueInstant = newDate();
        Subject subject = subjectProvider.get(SUBJECT_ID, AUDIENCE_ID, createSAML2Config(),
                SAML2SubjectConfirmation.HOLDER_OF_KEY, issueInstant, getProofState());
        assertEquals(SUBJECT_ID, subject.getNameID().getValue());
        assertEquals(NAME_ID_FORMAT, subject.getNameID().getFormat());

        SubjectConfirmation subjectConfirmation = (SubjectConfirmation)subject.getSubjectConfirmation().get(0);
        assertEquals(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_HOLDER_OF_KEY, subjectConfirmation.getMethod());
        SubjectConfirmationData subjectConfirmationData = subjectConfirmation.getSubjectConfirmationData();
        assertTrue(subjectConfirmationData != null);
        assertEquals(subjectConfirmationData.getContentType(), KEY_INFO_CONFIRMATION_DATA_TYPE);
        //see if we can go from xml back to class instance.
        AssertionFactory.getInstance().createSubjectConfirmationData(subjectConfirmationData.toXMLString(true, true));
    }

    private SAML2Config createSAML2Config() {
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("email", "mail");
        SAML2Config.SAML2ConfigBuilder builder = SAML2Config.builder();
        return builder
                .attributeMap(attributeMap)
                .nameIdFormat(NAME_ID_FORMAT)
                .spEntityId("http://host.com/sp/entity/id")
                .tokenLifetimeInSeconds(TOKEN_LIFETIME_SECONDS)
                .idpId("da_idp")
                .build();
    }

    private X509Certificate getCertificate() throws IOException, CertificateException {
        return (X509Certificate) CertificateFactory.getInstance(X_509).generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }

    ProofTokenState getProofState() throws Exception {
        return ProofTokenState.builder().x509Certificate(getCertificate()).build();
    }
}
