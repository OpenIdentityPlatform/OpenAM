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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.statements;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactory;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.SubjectProvider
 */
public class DefaultSubjectProvider implements SubjectProvider {
    /*
    Section 3.1 of http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf states that the data type of the
    SubjectConfirmationData, if specified, must be KeyInfoConfirmationDataType, prefixed by the saml assertion prefix. This
    value is specified here.
     */
    private static final String KEY_INFO_CONFIRMATION_DATA_TYPE = SAML2Constants.ASSERTION_PREFIX + "KeyInfoConfirmationDataType";
    private final KeyInfoFactory keyInfoFactory;

    public DefaultSubjectProvider(KeyInfoFactory keyInfoFactory) {
        this.keyInfoFactory = keyInfoFactory;
    }

    public Subject get(String subjectId, String spAcsUrl, SAML2Config saml2Config,
                       SAML2SubjectConfirmation subjectConfirmation,
                       Date assertionIssueInstant, ProofTokenState proofTokenState) throws TokenCreationException {
        try {
            Subject subject = AssertionFactory.getInstance().createSubject();
            setNameIdentifier(subject, subjectId, saml2Config.getNameIdFormat());
            SubjectConfirmation subConfirmation =
                    AssertionFactory.getInstance().createSubjectConfirmation();
            switch (subjectConfirmation) {
                case BEARER:
                    subConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);
                     /*
                    see section 4.1.4.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf -
                    Recipient attribute of SubjectConfirmation element must be set to the Service Provider
                    ACS url.
                     */
                    SubjectConfirmationData bearerConfirmationData =
                            AssertionFactory.getInstance().createSubjectConfirmationData();
                    bearerConfirmationData.setRecipient(spAcsUrl);
                    /*
                    see section 4.1.4.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf - NotBefore cannot
                    be set, but NotOnOrAfter must be set.
                     */
                    bearerConfirmationData.setNotOnOrAfter(new Date(assertionIssueInstant.getTime() +
                            (saml2Config.getTokenLifetimeInSeconds() * 1000)));
                    subConfirmation.setSubjectConfirmationData(bearerConfirmationData);
                    break;
                case SENDER_VOUCHES:
                    subConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_SENDER_VOUCHES);
                    break;
                case HOLDER_OF_KEY:
                    subConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_HOLDER_OF_KEY);
                    subConfirmation.setSubjectConfirmationData(getHoKSubjectConfirmationData(proofTokenState.getX509Certificate()));
                    break;
                default:
                    throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                            "Unexpected SubjectConfirmation value in DefaultSubjectProvider: " + subjectConfirmation);

            }
            List<SubjectConfirmation> subjectConfirmationList = new ArrayList<SubjectConfirmation>();
            subjectConfirmationList.add(subConfirmation);
            subject.setSubjectConfirmation(subjectConfirmationList);
            return subject;
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting subject confirmation state in DefaultSubjectProvider: " + e, e);
        }
    }

    private void setNameIdentifier(Subject subject, String subjectId, String nameIdFormat) throws TokenCreationException {
        try {
            subject.setNameID(createNameIdentifier(subjectId, nameIdFormat));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting NameID state in DefaultSubjectProvider: " + e, e);
        }

    }
    private NameID createNameIdentifier(String subjectId, String nameIdFormat) throws TokenCreationException {
        NameID nameID = AssertionFactory.getInstance().createNameID();
        try {
            nameID.setValue(subjectId);
            nameID.setFormat(nameIdFormat);
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting NameID state in DefaultSubjectProvider: " + e, e);
        }
        return nameID;
    }

    private SubjectConfirmationData getHoKSubjectConfirmationData(X509Certificate certificate) throws TokenCreationException {
        Element keyInfoElement;
        try {
            keyInfoElement = keyInfoFactory.generatePublicKeyInfo(certificate);
        } catch (ParserConfigurationException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught generating KeyInfo for HoK SubjectConfirmation DefaultSubjectProvider: " + e, e);
        } catch (XMLSecurityException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught generating KeyInfo for HoK SubjectConfirmation DefaultSubjectProvider: " + e, e);
        }
        try {
            final List<Element> elementList = new ArrayList<Element>();
            elementList.add(keyInfoElement);
            final SubjectConfirmationData subjectConfirmationData = AssertionFactory.getInstance().createSubjectConfirmationData();
            subjectConfirmationData.setContentType(KEY_INFO_CONFIRMATION_DATA_TYPE);
            subjectConfirmationData.setContent(elementList);
            return subjectConfirmationData;
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught generating SubjectConfirmationData with HoK KeyInfo element in DefaultSubjectProvider: " + e, e);
        }
    }
}
