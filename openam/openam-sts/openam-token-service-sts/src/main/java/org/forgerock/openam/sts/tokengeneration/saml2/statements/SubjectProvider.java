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

import com.sun.identity.saml2.assertion.Subject;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;

import java.util.Date;

/**
 * Defines the concerns of providing the Subject to be included in the generated SAML2 assertion. If no custom class
 * name is specified in the SAML2Config, then the DefaultSubjectProvider will be used.
 */
public interface SubjectProvider {
    /**
     * Called to obtain the Subject instance to be included in the generated SAML2 assertion
     * @param subjectId The identifier for the subject
     * @param audienceId Used to set the recipient in the SubjectConfirmationData
     * @param saml2Config The STS-instance specific SAML2 configuration state
     * @param subjectConfirmation The type of subject confirmation
     * @param assertionIssueInstant The issue instant assertion
     * @param proofTokenState The instance containing the proof token necessary for HoK assertions.
     * @return The to-be-included Subject instance
     * @throws TokenCreationException
     */
    Subject get(String subjectId, String audienceId, SAML2Config saml2Config,
                SAML2SubjectConfirmation subjectConfirmation, Date assertionIssueInstant,
                ProofTokenState proofTokenState) throws TokenCreationException;
}
