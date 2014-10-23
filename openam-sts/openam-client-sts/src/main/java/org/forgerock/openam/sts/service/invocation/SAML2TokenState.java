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

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This class encapsulates state passed to the REST STS about the nature of the to-be-issued SAML2 token. The
 * REST-STS, in turn, passes some of this information to the TokenGenerationService.
 */
public class SAML2TokenState {
    public static class SAML2TokenStateBuilder {
        private SAML2SubjectConfirmation subjectConfirmation;
        private ProofTokenState proofTokenState;

        public SAML2TokenStateBuilder saml2SubjectConfirmation(SAML2SubjectConfirmation subjectConfirmation) {
            this.subjectConfirmation = subjectConfirmation;
            return this;
        }

        public SAML2TokenStateBuilder proofTokenState(ProofTokenState proofTokenState) {
            this.proofTokenState = proofTokenState;
            return this;
        }

        public SAML2TokenState build() throws TokenMarshalException {
            return new SAML2TokenState(this);
        }
    }

    /*
    These variables are public so that hand-rolled JsonValues corresponding to SAML2TokenState can be created.
     */
    public static final String SUBJECT_CONFIRMATION = "subject_confirmation";
    public static final String PROOF_TOKEN_STATE = "proof_token_state";

    private final SAML2SubjectConfirmation subjectConfirmation;
    private final ProofTokenState proofTokenState;

    private SAML2TokenState(SAML2TokenStateBuilder builder) throws TokenMarshalException {
        this.subjectConfirmation = builder.subjectConfirmation;
        this.proofTokenState = builder.proofTokenState;
        if (subjectConfirmation == null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "SubjectConfirmation type must be set.");
        }
        if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(subjectConfirmation) && (proofTokenState == null)) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "If " +
                    SAML2SubjectConfirmation.HOLDER_OF_KEY + " is specified, proofTokenState must also be set.");
        }
    }

    public static SAML2TokenStateBuilder builder() {
        return new SAML2TokenStateBuilder();
    }

    public SAML2SubjectConfirmation getSubjectConfirmation() {
        return subjectConfirmation;
    }

    public ProofTokenState getProofTokenState() {
        return proofTokenState;
    }

    public static SAML2TokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        String subjectConfirmationString = jsonValue.get(SUBJECT_CONFIRMATION).asString();
        if (subjectConfirmationString == null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                    "Value corresponding to " + SUBJECT_CONFIRMATION + " key is null");
        }
        SAML2SubjectConfirmation saml2SubjectConfirmation;
        try {
            saml2SubjectConfirmation = SAML2SubjectConfirmation.valueOf(subjectConfirmationString);
        } catch (IllegalArgumentException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Invalid subject confirmation type specified.");
        }
        SAML2TokenStateBuilder builder = SAML2TokenState.builder()
                .saml2SubjectConfirmation(saml2SubjectConfirmation);
        JsonValue jsonProofToken = jsonValue.get(PROOF_TOKEN_STATE);
        if (!jsonProofToken.isNull()) {
            builder.proofTokenState(ProofTokenState.fromJson(jsonProofToken));
        }
        return builder.build();
    }

    public JsonValue toJson() {
        if (proofTokenState != null) {
            return json(object(
                    field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.SAML2.name()),
                    field(SUBJECT_CONFIRMATION, subjectConfirmation.name()),
                    field(PROOF_TOKEN_STATE, proofTokenState.toJson())));

        } else {
            return json(object(
                    field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.SAML2.name()),
                    field(SUBJECT_CONFIRMATION, subjectConfirmation.name())));
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SAML2TokenState) {
            SAML2TokenState otherTokenState = (SAML2TokenState)other;
            return subjectConfirmation.equals(otherTokenState.getSubjectConfirmation()) &&
                    proofTokenState != null ? proofTokenState.equals(otherTokenState.getProofTokenState()) :
                    (otherTokenState.getProofTokenState() == null);
        }
        return false;
    }
}
