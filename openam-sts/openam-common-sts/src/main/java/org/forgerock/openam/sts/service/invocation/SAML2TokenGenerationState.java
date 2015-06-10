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

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.util.Reject;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Encapsulates the invocation-specific state necessary to generation SAML2 assertions. An instance of this class will
 * be encapsulated in the TokenGenerationServiceInvocationState instance when the TGS is invoked to generate a SAML2 assertion.
 */
public class SAML2TokenGenerationState {
    public static class SAML2TokenGenerationStateBuilder {
        private String authnContextClassRef;
        private SAML2SubjectConfirmation saml2SubjectConfirmation;

        private SAML2TokenGenerationStateBuilder() {}

        /*
        Contains the X509Certificate included in the KeyInfo element in the SubjectConfirmation element for
        SAML2 Holder-of-Key assertions.
         */
        private ProofTokenState proofTokenState;

        public SAML2TokenGenerationStateBuilder authenticationContextClassReference(String authnContextClassRef) {
            this.authnContextClassRef = authnContextClassRef;
            return this;
        }

        public SAML2TokenGenerationStateBuilder subjectConfirmation(SAML2SubjectConfirmation saml2SubjectConfirmation) {
            this.saml2SubjectConfirmation = saml2SubjectConfirmation;
            return this;
        }

        public SAML2TokenGenerationStateBuilder proofTokenState(ProofTokenState proofTokenState) {
            this.proofTokenState = proofTokenState;
            return this;
        }

        public SAML2TokenGenerationState build() {
            return new SAML2TokenGenerationState(this);
        }

    }
    private static final String AUTHN_CONTEXT_CLASS_REF = "authnContextClassRef";
    private static final String SAML2_SUBJECT_CONFIRMATION = "saml2SubjectConfirmation";
    private static final String PROOF_TOKEN_STATE = "proofTokenState";

    /**
     * This value is required, and will be used to set the AuthnContext in the AuthnStatement. See
     * http://docs.oasis-open.org/security/saml/v2.0/saml-authn-context-2.0-os.pdf for more information on
     * the AuthnContext. The STS will set this value based on the type of validated token in the
     * token transformation action. Examples of values: urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport,
     *  urn:oasis:names:tc:SAML:2.0:ac:classes:X509.
     */
    private final String authnContextClassRef;
    private final SAML2SubjectConfirmation saml2SubjectConfirmation;

    /*
    Contains the X509Certificate included in the KeyInfo element in the SubjectConfirmation element for
    SAML2 Holder-of-Key assertions.
     */
    private final ProofTokenState proofTokenState;

    private SAML2TokenGenerationState(SAML2TokenGenerationStateBuilder builder) {
        authnContextClassRef = builder.authnContextClassRef;
        saml2SubjectConfirmation = builder.saml2SubjectConfirmation;
        proofTokenState = builder.proofTokenState;
        Reject.ifNull(authnContextClassRef, AUTHN_CONTEXT_CLASS_REF + " must be set");
        Reject.ifNull(saml2SubjectConfirmation, SAML2_SUBJECT_CONFIRMATION + " must be set.");

        if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(saml2SubjectConfirmation) && (proofTokenState == null)) {
            throw new IllegalStateException("Specified a SAML2 HolderOfKey assertion without specifying the " +
                    "ProofTokenState necessary to prove assertion ownership.");
        }
    }

    public String getAuthnContextClassRef() {
        return authnContextClassRef;
    }

    public SAML2SubjectConfirmation getSaml2SubjectConfirmation() {
        return saml2SubjectConfirmation;
    }

    public ProofTokenState getProofTokenState() {
        return proofTokenState;
    }

    public static SAML2TokenGenerationStateBuilder builder() {
        return new SAML2TokenGenerationStateBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SAML2TokenGenerationState)) {
            return false;
        }

        SAML2TokenGenerationState that = (SAML2TokenGenerationState) o;

        return authnContextClassRef.equals(that.authnContextClassRef) &&
            saml2SubjectConfirmation.equals(that.saml2SubjectConfirmation) &&
            Objects.equal(proofTokenState, that.proofTokenState);

    }

    @Override
    public int hashCode() {
        int result = authnContextClassRef.hashCode();
        result = 31 * result + saml2SubjectConfirmation.hashCode();
        result = 31 * result + (proofTokenState != null ? proofTokenState.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public JsonValue toJson() {
        JsonValue jsonValue =  json(object(
                field(AUTHN_CONTEXT_CLASS_REF, authnContextClassRef),
                field(SAML2_SUBJECT_CONFIRMATION, saml2SubjectConfirmation.name())));
        if (proofTokenState != null) {
            jsonValue.add(PROOF_TOKEN_STATE, proofTokenState.toJson());
        }
        return jsonValue;
    }

    public static SAML2TokenGenerationState fromJson(JsonValue json) throws TokenMarshalException {
        if (json ==  null) {
            return null;
        }
        SAML2TokenGenerationStateBuilder builder = SAML2TokenGenerationState.builder();
        builder
                .authenticationContextClassReference(json.get(AUTHN_CONTEXT_CLASS_REF).asString())
                .subjectConfirmation(SAML2SubjectConfirmation.valueOf(json.get(SAML2_SUBJECT_CONFIRMATION).asString()));

        JsonValue proofToken = json.get(PROOF_TOKEN_STATE);
        if (!proofToken.isNull()) {
            builder.proofTokenState(ProofTokenState.fromJson(proofToken));
        }
        return builder.build();
    }
}
