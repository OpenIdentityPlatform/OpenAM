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

package org.forgerock.openam.sts.tokengeneration.service;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
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
    public enum SAML2SubjectConfirmation {BEARER, SENDER_VOUCHES, HOLDER_OF_KEY};

    public static class TokenGenerationServiceInvocationStateBuilder {
        private String ssoTokenString;
        private TokenType tokenType = TokenType.SAML2;
        private String stsInstanceId;
        private SAML2SubjectConfirmation saml2SubjectConfirmation;
        private AMSTSConstants.STSType stsType = AMSTSConstants.STSType.REST;
        /*
        According to section 4.1.4.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf:
        Bearer assertions must contain a SubjectConfirmationData element that contains a Recipient attribute which
        contains the service providers assertion consumer service url.
         */
        private String spAcsUrl;

        public TokenGenerationServiceInvocationStateBuilder ssoTokenString(String ssoTokenString) {
            this.ssoTokenString = ssoTokenString;
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

        public TokenGenerationServiceInvocationStateBuilder serviceProviderAssertionConsumerServiceUrl(String spAcsUrl) {
            this.spAcsUrl = spAcsUrl;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder stsType(AMSTSConstants.STSType stsType) {
            this.stsType = stsType;
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
    private static final String TOKEN_TYPE = "tokenType";
    private static final String STS_INSTANCE_ID = "stsInstanceId";
    private static final String SAML2_SUBJECT_CONFIRMATION = "saml2SubjectConfirmation";
    private static final String SP_ACS_URL = "spAcsUrl";
    private static final String STS_TYPE = "sts_type";

    private final String ssoTokenString;
    private final TokenType tokenType;
    private final String stsInstanceId;
    private final SAML2SubjectConfirmation saml2SubjectConfirmation;
    private final String spAcsUrl;
    private final AMSTSConstants.STSType stsType;

    private TokenGenerationServiceInvocationState(TokenGenerationServiceInvocationStateBuilder builder) {
        this.ssoTokenString = builder.ssoTokenString;
        this.tokenType = builder.tokenType;
        this.stsInstanceId = builder.stsInstanceId;
        this.saml2SubjectConfirmation = builder.saml2SubjectConfirmation;
        this.spAcsUrl = builder.spAcsUrl; // no reject, as is optional, but only for non-bearer tokens
        this.stsType = builder.stsType;
        Reject.ifNull(ssoTokenString, "SSO Token String must be set");
        Reject.ifNull(tokenType, "Token Type must be set");
        Reject.ifNull(stsInstanceId, "STS instance id must be set");
        Reject.ifNull(saml2SubjectConfirmation, "SAML2 subject confirmation method must be set");
        Reject.ifNull(stsType, "sts type must be set");
        if (SAML2SubjectConfirmation.BEARER.equals(saml2SubjectConfirmation) && (spAcsUrl == null)) {
            throw new IllegalStateException("According to section 4.1.4.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf:\n" +
                    "Bearer assertions must contain a SubjectConfirmationData element that contains a Recipient attribute which\n" +
                    "contains the service providers assertion consumer service url.");
        }
    }

    public static TokenGenerationServiceInvocationStateBuilder builder() {
        return new TokenGenerationServiceInvocationStateBuilder();
    }

    public String getSsoTokenString() {
        return ssoTokenString;
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

    public String getSpAcsUrl() {
        return spAcsUrl;
    }

    public AMSTSConstants.STSType getStsType() {
        return stsType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TokenGenerationServiceInvocationState instance:").append('\n');
        sb.append('\t').append("TokenType: ").append(tokenType.name()).append('\n');
        sb.append('\t').append("STS instance id: ").append(stsInstanceId).append('\n');
        sb.append('\t').append("saml2 subject confirmation ").append(saml2SubjectConfirmation).append('\n');
        sb.append('\t').append("Service Provider ACS URL ").append(spAcsUrl).append('\n');
        sb.append('\t').append("sts type ").append(stsType).append('\n');
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
                    saml2SubjectConfirmation.equals(otherConfig.getSaml2SubjectConfirmation()) &&
                    (spAcsUrl != null ? spAcsUrl.equals(otherConfig.getSpAcsUrl()) : otherConfig.getSpAcsUrl() == null);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ssoTokenString.hashCode();
    }

    public JsonValue toJson() {
        return json(object(field(SSO_TOKEN_STRING, ssoTokenString),
                field(TOKEN_TYPE, tokenType.name()),
                field(STS_INSTANCE_ID, stsInstanceId),
                field(SAML2_SUBJECT_CONFIRMATION, saml2SubjectConfirmation.name()),
                field(SP_ACS_URL, spAcsUrl),
                field(STS_TYPE, stsType.name())));
    }

    public static TokenGenerationServiceInvocationState fromJson(JsonValue json) throws IllegalStateException {
        return TokenGenerationServiceInvocationState.builder()
                .ssoTokenString(json.get(SSO_TOKEN_STRING).asString())
                .tokenType(TokenType.valueOf(TokenType.class, json.get(TOKEN_TYPE).asString()))
                .stsInstanceId(json.get(STS_INSTANCE_ID).asString())
                .saml2SubjectConfirmation(SAML2SubjectConfirmation.valueOf(SAML2SubjectConfirmation.class, json.get(SAML2_SUBJECT_CONFIRMATION).asString()))
                .serviceProviderAssertionConsumerServiceUrl(json.get(SP_ACS_URL).asString())
                .stsType(AMSTSConstants.STSType.valueOf(AMSTSConstants.STSType.class, json.get(STS_TYPE).asString()))
                .build();
    }
}
