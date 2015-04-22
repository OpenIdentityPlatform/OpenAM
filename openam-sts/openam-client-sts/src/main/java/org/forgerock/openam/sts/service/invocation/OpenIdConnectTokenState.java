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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.util.Reject;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Class encapsulating an OpenIdConnect jws string, and will emit json which includes state necessary to act as the input_token_state
 * in the RestSTSServiceInvocationState - i.e. the jws string, and a AMSTSConstants.TOKEN_TYPE_KEY field corresponding to
 * TokenType.OPENIDCONNECT.name().
 */
public class OpenIdConnectTokenState {
    public static class OpenIdConnectTokenStateBuilder {
        private String tokenValue;

        private OpenIdConnectTokenStateBuilder() {}

        public OpenIdConnectTokenStateBuilder tokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
            return this;
        }

        public OpenIdConnectTokenState build() {
            return new OpenIdConnectTokenState(this);
        }
    }
    private final OpenIdConnectIdToken openIdConnectIdToken;

    private OpenIdConnectTokenState(OpenIdConnectTokenStateBuilder builder) {
        Reject.ifNull(builder.tokenValue, "Non-null token value must be provided.");
        Reject.ifTrue(builder.tokenValue.isEmpty(), "Non-empty token value must be provided");
        openIdConnectIdToken = new OpenIdConnectIdToken(builder.tokenValue);
    }

    public String getTokenValue() {
        return openIdConnectIdToken.getTokenValue();
    }

    public static OpenIdConnectTokenStateBuilder builder() {
        return new OpenIdConnectTokenStateBuilder();
    }

    @Override
    public String toString() {
        try {
            return toJson().toString();
        } catch (TokenMarshalException e) {
            return "Exception caught marshalling toString: " + e;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof OpenIdConnectTokenState) {
            OpenIdConnectTokenState otherTokenState = (OpenIdConnectTokenState)other;
            return openIdConnectIdToken.getTokenValue().equals(otherTokenState.getTokenValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return openIdConnectIdToken.hashCode();
    }

    public JsonValue toJson() throws TokenMarshalException {
        return json(object(
                field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.OPENIDCONNECT.name()),
                field(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY, openIdConnectIdToken.getTokenValue())));
    }

    public static OpenIdConnectTokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        try {
            return OpenIdConnectTokenState.builder()
                    .tokenValue(jsonValue.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).asString())
                    .build();
        } catch (NullPointerException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY +
                    " not set in json: " + jsonValue.toString(), e);
        }
    }
}
