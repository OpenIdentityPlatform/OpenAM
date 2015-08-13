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

package org.forgerock.openam.sts.user.invocation;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.util.Reject;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * Class encapsulating an SAML2 token string, and will emit json which includes state necessary to act as the to-be-validated
 * token state in the RestSTSTokenValidationInvocation state - i.e. the SAML2 assertion string, and a
 * AMSTSConstants.TOKEN_TYPE_KEY field corresponding to TokenType.SAML2.name().
 */
public class SAML2TokenState {
    public static class SAML2TokenStateBuilder {
        private String saml2TokenValue;

        private SAML2TokenStateBuilder() {}

        public SAML2TokenStateBuilder tokenValue(String tokenValue) {
            this.saml2TokenValue = tokenValue;
            return this;
        }

        public SAML2TokenState build() {
            return new SAML2TokenState(this);
        }
    }
    private final String saml2TokenValue;

    private SAML2TokenState(SAML2TokenStateBuilder builder) {
        Reject.ifNull(builder.saml2TokenValue, "Non-null SAML2 token value must be provided.");
        Reject.ifTrue(builder.saml2TokenValue.isEmpty(), "Non-empty SAML2 token value must be provided");
        this.saml2TokenValue = builder.saml2TokenValue;
    }

    public String getSAML2TokenValue() {
        return saml2TokenValue;
    }

    public static SAML2TokenStateBuilder builder() {
        return new SAML2TokenStateBuilder();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SAML2TokenState) {
            SAML2TokenState otherTokenState = (SAML2TokenState)other;
            return saml2TokenValue.equals(otherTokenState.getSAML2TokenValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return saml2TokenValue.hashCode();
    }

    public JsonValue toJson() {
        return json(object(
                field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.SAML2.name()),
                field(AMSTSConstants.SAML2_TOKEN_KEY, saml2TokenValue)));
    }

    public static SAML2TokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        try {
            return SAML2TokenState.builder()
                    .tokenValue(jsonValue.get(AMSTSConstants.SAML2_TOKEN_KEY).asString())
                    .build();
        } catch (NullPointerException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, AMSTSConstants.SAML2_TOKEN_KEY +
                    " not set in json: " + jsonValue.toString(), e);
        }
    }
}
