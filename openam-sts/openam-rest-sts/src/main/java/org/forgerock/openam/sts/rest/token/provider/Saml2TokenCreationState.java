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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.provider;

import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;

/**
 * Encapsulation of the state specific to generating SAML2 assertions.
 */
public class Saml2TokenCreationState implements TokenTypeId {
    private final SAML2SubjectConfirmation subjectConfirmation;
    private final ProofTokenState proofTokenState;

    public Saml2TokenCreationState(SAML2SubjectConfirmation subjectConfirmation, ProofTokenState proofTokenState) {
        this.subjectConfirmation = subjectConfirmation;
        this.proofTokenState = proofTokenState;
    }

    public Saml2TokenCreationState(SAML2SubjectConfirmation subjectConfirmation) {
        this(subjectConfirmation, null);
    }

    public SAML2SubjectConfirmation getSubjectConfirmation() {
        return subjectConfirmation;
    }

    public ProofTokenState getProofTokenState() {
        return proofTokenState;
    }

    @Override
    public String getId() {
        return TokenType.SAML2.getId();
    }
}
