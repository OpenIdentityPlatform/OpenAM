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
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
import org.forgerock.util.Reject;

/**
 * Class encapsulating an OpenAM sessionId. Simply delegates to the corresponding model class and marshalling functionality.
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
        return new OpenAMSessionTokenMarshaller().toJson(openAMSessionToken);
    }

    public static OpenAMTokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        OpenAMSessionToken sessionToken = new OpenAMSessionTokenMarshaller().fromJson(jsonValue);
        return OpenAMTokenState.builder().sessionId(sessionToken.getSessionId()).build();
    }
}
