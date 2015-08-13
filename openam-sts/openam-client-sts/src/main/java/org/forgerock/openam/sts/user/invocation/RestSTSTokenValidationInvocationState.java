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

package org.forgerock.openam.sts.user.invocation;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenMarshalException;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * An encapsulation of the state necessary to make a rest-sts token validation invocation. Note that only tokens issued
 * by the rest-sts can be the payload of a token validation invocation - i.e. OpenIdConnect and SAML2 tokens.
 */
public class RestSTSTokenValidationInvocationState {
    public static final String VALIDATED_TOKEN_STATE = "validated_token_state";

    public static class RestSTSTokenValidationInvocationStateBuilder {
        private JsonValue validatedTokenState;

        private RestSTSTokenValidationInvocationStateBuilder() {}

        /**
         *
         * @param validatedTokenState The JsonValue corresponding to the to-be-validated token state. This is state must
         *                            specify a token_type field of either SAML2 or OPENIDCONNECT, and then either a
         *                            saml2_token or oidc_id_token key referencing the to-be-validated token state.
         * @return the builder
         */
        public RestSTSTokenValidationInvocationStateBuilder validatedTokenState(JsonValue validatedTokenState) {
            this.validatedTokenState = validatedTokenState;
            return this;
        }

        /**
         *
         * @return a RestSTSTokenValidationInvocationState instance whose json representation can consume restful token
         * validation
         * @throws TokenMarshalException if token state was not set correctly
         */
        public RestSTSTokenValidationInvocationState build() throws TokenMarshalException {
            return new RestSTSTokenValidationInvocationState(this);
        }
    }

    private final JsonValue validatedTokenState;

    private RestSTSTokenValidationInvocationState(RestSTSTokenValidationInvocationStateBuilder builder) throws TokenMarshalException {
        this.validatedTokenState = builder.validatedTokenState;

        if ((validatedTokenState ==  null) || validatedTokenState.isNull()) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Validated token state must be set!");
        }
    }

    /**
     *
     * @return a builder to create the RestSTSTokenValidationInvocationState
     */
    public static RestSTSTokenValidationInvocationStateBuilder builder() {
        return new RestSTSTokenValidationInvocationStateBuilder();
    }

    /**
     *
     * @return the json representation of the to-be-validated token state
     */
    public JsonValue getValidatedTokenState() {
        return validatedTokenState;
    }

    /**
     *
     * @return the json representation of the invocation state
     */
    public JsonValue toJson() {
        return json(object(field(VALIDATED_TOKEN_STATE, validatedTokenState)));
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RestSTSTokenValidationInvocationState) {
            RestSTSTokenValidationInvocationState otherState = (RestSTSTokenValidationInvocationState)other;
            return validatedTokenState.toString().equals(otherState.validatedTokenState.toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     *
     * @param jsonValue the json representation of token validation invocation state, compliant with the json emitted by
     *                  toJson in this class
     * @return the RestSTSTokenValidationInvocationState instance corresponding to the jsonValue parameter
     * @throws TokenMarshalException if the json state was missing a required field.
     */
    public static RestSTSTokenValidationInvocationState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        return builder()
                .validatedTokenState(jsonValue.get(VALIDATED_TOKEN_STATE))
                .build();
    }
}
