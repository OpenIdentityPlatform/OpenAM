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
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.service.TokenGenerationServiceInvocationState;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.SubjectProvider
 */
public class DefaultSubjectProvider implements SubjectProvider {
    /**
     * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.SubjectProvider#get(String, String,
     * org.forgerock.openam.sts.config.user.SAML2Config,
     * org.forgerock.openam.sts.tokengeneration.service.TokenGenerationServiceInvocationState.SAML2SubjectConfirmation,
     * java.util.Date)
     */
    public Subject get(String subjectId, String audienceId, SAML2Config saml2Config,
                       TokenGenerationServiceInvocationState.SAML2SubjectConfirmation subjectConfirmation,
                       Date assertionIssueInstant) throws TokenCreationException {
        try {
            Subject subject = AssertionFactory.getInstance().createSubject();
            setNameIdentifier(subject, subjectId, saml2Config.getNameIdFormat());

            SubjectConfirmation subConfirmation =
                    AssertionFactory.getInstance().createSubjectConfirmation();
            SubjectConfirmationData confirmationData =
                    AssertionFactory.getInstance().createSubjectConfirmationData();
            confirmationData.setRecipient(audienceId);
            /*
            see section 4.1.4.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf - NotBefore cannot
            be set, but NotOnOrAfter must be set.
             */
            confirmationData.setNotOnOrAfter(new Date(assertionIssueInstant.getTime() +
                    (saml2Config.getTokenLifetimeInSeconds() * 1000)));
            switch (subjectConfirmation) {
                case BEARER:
                    subConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);
                    subConfirmation.setSubjectConfirmationData(confirmationData);
                    break;
                case SENDER_VOUCHES:
                    subConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_SENDER_VOUCHES);
                    subConfirmation.setSubjectConfirmationData(confirmationData);
                    break;
                case HOLDER_OF_KEY:
                    subConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_HOLDER_OF_KEY);
                    addKeyInfoToSubjectConfirmationData(confirmationData);
                    subConfirmation.setSubjectConfirmationData(confirmationData);
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

    private void addKeyInfoToSubjectConfirmationData(SubjectConfirmationData confirmationData) throws TokenCreationException {
        Element keyInfoElement = null; //TODO - get from KeyInfoFactory, and snake that into this class...
        confirmationData.getContent().add(keyInfoElement);
    }

}
