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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
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
    public static class TokenGenerationServiceInvocationStateBuilder {
        private String ssoTokenString;
        private TokenType tokenType;
        private String stsInstanceId;
        private AMSTSConstants.STSType stsType;
        private String realm = "/"; //default value
        private SAML2TokenGenerationState saml2TokenGenerationState;
        private OpenIdConnectTokenGenerationState openIdConnectTokenGenerationState;

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

        public TokenGenerationServiceInvocationStateBuilder stsType(AMSTSConstants.STSType stsType) {
            this.stsType = stsType;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder realm(String realm) {
            this.realm = realm;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder saml2GenerationState(SAML2TokenGenerationState saml2TokenGenerationState) {
            this.saml2TokenGenerationState = saml2TokenGenerationState;
            return this;
        }

        public TokenGenerationServiceInvocationStateBuilder openIdConnectTokenGenerationState(OpenIdConnectTokenGenerationState openIdConnectTokenGenerationState) {
            this.openIdConnectTokenGenerationState = openIdConnectTokenGenerationState;
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
    private static final String STS_TYPE = "stsType";
    private static final String REALM = "realm";
    private static final String SAML2_GENERATION_STATE = "saml2TokenGenerationState";
    private static final String OPENID_CONNECT_GENERATION_STATE = "openIdConnectTokenGenerationState";

    private final String ssoTokenString;
    private final TokenType tokenType;
    private final String stsInstanceId;
    private final AMSTSConstants.STSType stsType;
    private final String realm;
    private final SAML2TokenGenerationState saml2TokenGenerationState;
    private final OpenIdConnectTokenGenerationState openIdConnectTokenGenerationState;

    private TokenGenerationServiceInvocationState(TokenGenerationServiceInvocationStateBuilder builder) {
        this.ssoTokenString = builder.ssoTokenString;
        this.tokenType = builder.tokenType;
        this.stsInstanceId = builder.stsInstanceId;
        this.stsType = builder.stsType;
        this.realm = builder.realm;
        this.saml2TokenGenerationState = builder.saml2TokenGenerationState;
        this.openIdConnectTokenGenerationState = builder.openIdConnectTokenGenerationState;
        Reject.ifNull(ssoTokenString, "SSO Token String must be set");
        Reject.ifNull(tokenType, "Token Type must be set");
        Reject.ifNull(stsInstanceId, "STS instance id must be set");
        Reject.ifNull(stsType, "sts type must be set");
        Reject.ifNull(realm, "realm must be set");

        if (TokenType.SAML2.equals(tokenType) && saml2TokenGenerationState == null) {
            throw new IllegalArgumentException("If a SAML2 token is to be issued, then the saml2TokenGenerationState must be set. ");
        }
        if (TokenType.OPENIDCONNECT.equals(tokenType) && openIdConnectTokenGenerationState == null) {
            throw new IllegalArgumentException("If an OpenIdConnect token is to be issued, then the opendIdConnectTokenGenerationState must be set. ");
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

    public AMSTSConstants.STSType getStsType() {
        return stsType;
    }

    public String getRealm() {
        return realm;
    }

    public SAML2TokenGenerationState getSaml2TokenGenerationState() {
        return saml2TokenGenerationState;
    }

    public OpenIdConnectTokenGenerationState getOpenIdConnectTokenGenerationState() {
        return openIdConnectTokenGenerationState;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TokenGenerationServiceInvocationState instance:").append('\n');
        sb.append('\t').append("TokenType: ").append(tokenType.name()).append('\n');
        sb.append('\t').append("STS instance id: ").append(stsInstanceId).append('\n');
        sb.append('\t').append("sts type ").append(stsType).append('\n');
        sb.append('\t').append("realm ").append(realm).append('\n');
        sb.append('\t').append("saml2TokenGenerationState ").append(saml2TokenGenerationState).append('\n');
        sb.append('\t').append("openIdConnectTokenGenerationState ").append(openIdConnectTokenGenerationState).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof TokenGenerationServiceInvocationState) {
            TokenGenerationServiceInvocationState oc = (TokenGenerationServiceInvocationState)other;
            return ssoTokenString.equals(oc.getSsoTokenString()) &&
                    tokenType.equals(oc.getTokenType()) &&
                    stsInstanceId.equals(oc.getStsInstanceId()) &&
                    stsType.equals(oc.getStsType()) &&
                    Objects.equal(saml2TokenGenerationState, oc.getSaml2TokenGenerationState()) &&
                    Objects.equal(openIdConnectTokenGenerationState, oc.getOpenIdConnectTokenGenerationState()) &&
                    realm.equals(oc.getRealm());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ssoTokenString.hashCode();
    }

    public JsonValue toJson() {
        return json(object(
                field(SSO_TOKEN_STRING, ssoTokenString),
                field(TOKEN_TYPE, tokenType.name()),
                field(STS_INSTANCE_ID, stsInstanceId),
                field(REALM, realm),
                field(SAML2_GENERATION_STATE, saml2TokenGenerationState != null ? saml2TokenGenerationState.toJson() : null),
                field(OPENID_CONNECT_GENERATION_STATE, openIdConnectTokenGenerationState != null ? openIdConnectTokenGenerationState.toJson() : null),
                field(STS_TYPE, stsType.name())));
    }

    public static TokenGenerationServiceInvocationState fromJson(JsonValue json) throws TokenMarshalException {
        TokenGenerationServiceInvocationStateBuilder builder =  TokenGenerationServiceInvocationState.builder()
                .ssoTokenString(json.get(SSO_TOKEN_STRING).asString())
                .tokenType(TokenType.valueOf(TokenType.class, json.get(TOKEN_TYPE).asString()))
                .stsInstanceId(json.get(STS_INSTANCE_ID).asString())
                .realm(json.get(REALM).asString())
                .saml2GenerationState(!json.get(SAML2_GENERATION_STATE).isNull()
                        ? SAML2TokenGenerationState.fromJson(json.get(SAML2_GENERATION_STATE)) : null)
                .openIdConnectTokenGenerationState(!json.get(OPENID_CONNECT_GENERATION_STATE).isNull()
                        ? OpenIdConnectTokenGenerationState.fromJson(json.get(OPENID_CONNECT_GENERATION_STATE)) : null)
                .stsType(AMSTSConstants.STSType.valueOf(AMSTSConstants.STSType.class, json.get(STS_TYPE).asString()));
        return builder.build();
    }
}
