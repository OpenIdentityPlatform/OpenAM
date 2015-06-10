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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.util.Reject;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Class encapsulating an OpenAM sessionId, and will emit json which includes state necessary to act as the input_token_state
 * in the RestSTSServiceInvocationState - i.e. the OpenAM session id, and a AMSTSConstants.TOKEN_TYPE_KEY field corresponding to
 * TokenType.OPENAM.name().
 */
public class OpenAMTokenState {
    public static class OpenAMTokenStateBuilder {
        private String sessionId;

        public OpenAMTokenStateBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public OpenAMTokenState build() {
            return new OpenAMTokenState(this);
        }
    }
    private final OpenAMSessionToken openAMSessionToken;

    private OpenAMTokenState(OpenAMTokenStateBuilder builder) {
        Reject.ifNull(builder.sessionId, "Non-null session id must be provided.");
        Reject.ifTrue(builder.sessionId.isEmpty(), "Non-empty session id must be provided");
        openAMSessionToken = new OpenAMSessionToken(builder.sessionId);
    }

    public String getSessionId() {
        return openAMSessionToken.getSessionId();
    }

    public static OpenAMTokenStateBuilder builder() {
        return new OpenAMTokenStateBuilder();
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
        if (other instanceof OpenAMTokenState) {
            OpenAMTokenState otherTokenState = (OpenAMTokenState)other;
            return openAMSessionToken.getSessionId().equals(otherTokenState.getSessionId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return openAMSessionToken.hashCode();
    }

    public JsonValue toJson() throws TokenMarshalException {
        return json(object(
                field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.OPENAM.name()),
                field(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID, openAMSessionToken.getSessionId())));
    }

    public static OpenAMTokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        if (!jsonValue.get(AMSTSConstants.TOKEN_TYPE_KEY).isString() ||
                !TokenType.OPENAM.name().equals(jsonValue.get(AMSTSConstants.TOKEN_TYPE_KEY).asString())) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, "passed-in jsonValue does not have " +
                    AMSTSConstants.TOKEN_TYPE_KEY + " field which matches the OpenAM token type: " + jsonValue);
        }
        final JsonValue jsonSessionId = jsonValue.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID);
        if (jsonSessionId.isNull()) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, "passed-in jsonValue does not have " +
                    AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID + " field: " + jsonValue);
        }
        final String sessionId = jsonSessionId.asString();
        if (sessionId.isEmpty()) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, "passed-in jsonValue does not have a non-empty " +
                    AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID + " field: " + jsonValue);

        }
        return OpenAMTokenState.builder().sessionId(sessionId).build();
    }
}
