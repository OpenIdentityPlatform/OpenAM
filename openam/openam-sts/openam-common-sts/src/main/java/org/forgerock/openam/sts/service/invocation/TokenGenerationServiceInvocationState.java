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
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.util.Reject;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This class defines the parameters necessary to invoke the TokenGenerationService, as well as the json
 * marshalling/unmarshalling necessary for this state to be POSTed at this service.
 *
 */
public class TokenGenerationServiceInvocationState {
    public static class TokenGenerationServiceInvocationStateBuilder {
        private String ssoTokenString;
        /**
         * This value is required, and will be used to set the AuthnContext in the AuthnStatement. See
         * http://docs.oasis-open.org/security/saml/v2.0/saml-authn-context-2.0-os.pdf for more information on
         * the AuthnContext. The STS will set this value based on the type of validated token in the
         * token transformation action. Examples of values: urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport,
         *  urn:oasis:names:tc:SAML:2.0:ac:classes:X509.
         */
        private String authnContextClassRef;
        private TokenType tokenType = TokenType.SAML2;
        private String stsInstanceId;
        private SAML2SubjectConfirmation saml2SubjectConfirmation;
        private AMSTSConstants.STSType stsType = AMSTSConstants.STSType.REST;
        private ProofTokenState proofTokenState;
        private String realm = "/"; //default value

        public TokenGenerationServiceInvocationStateBuilder ssoTokenString(String ssoTokenString) {
            this.ssoTokenString = ssoTokenString;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder authNContextClassRef(String authnContextClassRef) {
            this.authnContextClassRef = authnContextClassRef;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder tokenType(TokenType tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder stsInstanceId(String stsInstanceId) {
            this.stsInstanceId = stsInstanceId;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder saml2SubjectConfirmation(SAML2SubjectConfirmation saml2SubjectConfirmation) {
            this.saml2SubjectConfirmation = saml2SubjectConfirmation;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder stsType(AMSTSConstants.STSType stsType) {
            this.stsType = stsType;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder realm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Contains the X509Certificate included in the KeyInfo element in the SubjectConfirmation element for
         * SAML2 Holder-of-Key assertions.
         * @param proofTokenState The ProofTokenState containing the X509Certificate proof token
         * @return the Builder to facilitate fluent construction
         */
        public TokenGenerationServiceInvocationStateBuilder proofTokenState(ProofTokenState proofTokenState) {
            this.proofTokenState = proofTokenState;
            return this;
        }

        public TokenGenerationServiceInvocationState build() {
            return new TokenGenerationServiceInvocationState(this);
        }
    }
    /*
    Define the names of fields to aid in json marshalling.
     */
    private static final String SSO_TOKEN_STRING = "ssoTokenString";
    private static final String AUTHN_CONTEXT_CLASS_REF = "authnContextClassRef";
    private static final String TOKEN_TYPE = "tokenType";
    private static final String STS_INSTANCE_ID = "stsInstanceId";
    private static final String SAML2_SUBJECT_CONFIRMATION = "saml2SubjectConfirmation";
    private static final String STS_TYPE = "stsType";
    private static final String PROOF_TOKEN_STATE = "proofTokenState";
    private static final String REALM = "realm";

    private final String ssoTokenString;
    private final String authnContextClassRef;
    private final TokenType tokenType;
    private final String stsInstanceId;
    private final SAML2SubjectConfirmation saml2SubjectConfirmation;
    private final AMSTSConstants.STSType stsType;
    private final ProofTokenState proofTokenState;
    private final String realm;

    private TokenGenerationServiceInvocationState(TokenGenerationServiceInvocationStateBuilder builder) {
        this.ssoTokenString = builder.ssoTokenString;
        this.authnContextClassRef = builder.authnContextClassRef;
        this.tokenType = builder.tokenType;
        this.stsInstanceId = builder.stsInstanceId;
        this.saml2SubjectConfirmation = builder.saml2SubjectConfirmation;
        this.stsType = builder.stsType;
        this.realm = builder.realm;
        this.proofTokenState = builder.proofTokenState;//no reject if null, as it is optional
        Reject.ifNull(ssoTokenString, "SSO Token String must be set");
        Reject.ifNull(authnContextClassRef, "authNContextClassRef String must be set");
        Reject.ifNull(tokenType, "Token Type must be set");
        Reject.ifNull(stsInstanceId, "STS instance id must be set");
        Reject.ifNull(saml2SubjectConfirmation, "SAML2 subject confirmation method must be set");
        Reject.ifNull(stsType, "sts type must be set");
        Reject.ifNull(realm, "realm must be set");
        if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(saml2SubjectConfirmation) && (proofTokenState == null)) {
            throw new IllegalStateException("Specified a SAML2 HolderOfKey assertion without specifying the " +
                    "ProofTokenState necessary to prove assertion ownership.");
        }
    }

    public static TokenGenerationServiceInvocationStateBuilder builder() {
        return new TokenGenerationServiceInvocationStateBuilder();
    }

    public String getSsoTokenString() {
        return ssoTokenString;
    }

    public String getAuthnContextClassRef() {
        return authnContextClassRef;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public String getStsInstanceId() {
        return stsInstanceId;
    }

    public SAML2SubjectConfirmation getSaml2SubjectConfirmation() {
        return saml2SubjectConfirmation;
    }


    public AMSTSConstants.STSType getStsType() {
        return stsType;
    }

    public ProofTokenState getProofTokenState() {
        return proofTokenState;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TokenGenerationServiceInvocationState instance:").append('\n');
        sb.append('\t').append("TokenType: ").append(tokenType.name()).append('\n');
        sb.append('\t').append("STS instance id: ").append(stsInstanceId).append('\n');
        sb.append('\t').append("saml2 subject confirmation ").append(saml2SubjectConfirmation).append('\n');
        sb.append('\t').append("sts type ").append(stsType).append('\n');
        sb.append('\t').append("ProofTokenState ").append(proofTokenState).append('\n');
        sb.append('\t').append("AuthNContextClassRef ").append(authnContextClassRef).append('\n');
        sb.append('\t').append("realm ").append(realm).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TokenGenerationServiceInvocationState) {
            TokenGenerationServiceInvocationState otherConfig = (TokenGenerationServiceInvocationState)other;
            return ssoTokenString.equals(otherConfig.getSsoTokenString()) &&
                    tokenType.equals(otherConfig.getTokenType()) &&
                    stsInstanceId.equals(otherConfig.getStsInstanceId()) &&
                    stsType.equals(otherConfig.getStsType()) &&
                    realm.equals(otherConfig.getRealm()) &&
                    saml2SubjectConfirmation.equals(otherConfig.getSaml2SubjectConfirmation()) &&
                    authnContextClassRef.equals(otherConfig.getAuthnContextClassRef()) &&
                    (proofTokenState != null ? proofTokenState.equals(otherConfig.getProofTokenState()) : otherConfig.getProofTokenState() == null);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ssoTokenString.hashCode();
    }

    public JsonValue toJson() {
        JsonValue jsonValue =  json(object(
                field(SSO_TOKEN_STRING, ssoTokenString),
                field(AUTHN_CONTEXT_CLASS_REF, authnContextClassRef),
                field(TOKEN_TYPE, tokenType.name()),
                field(STS_INSTANCE_ID, stsInstanceId),
                field(SAML2_SUBJECT_CONFIRMATION, saml2SubjectConfirmation.name()),
                field(REALM, realm),
                field(STS_TYPE, stsType.name())));
        if (proofTokenState != null) {
            jsonValue.add(PROOF_TOKEN_STATE, proofTokenState.toJson());
        }
        return jsonValue;
    }

    public static TokenGenerationServiceInvocationState fromJson(JsonValue json) throws TokenMarshalException {
        TokenGenerationServiceInvocationStateBuilder builder =  TokenGenerationServiceInvocationState.builder()
                .ssoTokenString(json.get(SSO_TOKEN_STRING).asString())
                .authNContextClassRef(json.get(AUTHN_CONTEXT_CLASS_REF).asString())
                .tokenType(TokenType.valueOf(TokenType.class, json.get(TOKEN_TYPE).asString()))
                .stsInstanceId(json.get(STS_INSTANCE_ID).asString())
                .realm(json.get(REALM).asString())
                .saml2SubjectConfirmation(SAML2SubjectConfirmation.valueOf(SAML2SubjectConfirmation.class, json.get(SAML2_SUBJECT_CONFIRMATION).asString()))
                .stsType(AMSTSConstants.STSType.valueOf(AMSTSConstants.STSType.class, json.get(STS_TYPE).asString()));
        JsonValue proofToken = json.get(PROOF_TOKEN_STATE);
        if (!proofToken.isNull()) {
            builder.proofTokenState(ProofTokenState.fromJson(proofToken));
        }
        return builder.build();
    }
}
