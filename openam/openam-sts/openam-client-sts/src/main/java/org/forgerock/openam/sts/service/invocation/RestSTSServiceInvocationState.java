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
import org.forgerock.openam.sts.TokenMarshalException;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Class which provides a means to programmatically create a REST STS invocation.
 */
public class RestSTSServiceInvocationState {
    public static final String INPUT_TOKEN_STATE = "input_token_state";
    public static final String OUTPUT_TOKEN_STATE = "output_token_state";

    public static class RestSTSServiceInvocationStateBuilder {
        private JsonValue inputTokenState;
        private JsonValue outputTokenState;

        public RestSTSServiceInvocationStateBuilder inputTokenState(JsonValue inputTokenState) {
            this.inputTokenState = inputTokenState;
            return this;
        }

        public RestSTSServiceInvocationStateBuilder outputTokenState(JsonValue outputTokenState) {
            this.outputTokenState = outputTokenState;
            return this;
        }

        public RestSTSServiceInvocationState build() throws TokenMarshalException {
            return new RestSTSServiceInvocationState(this);
        }

    }

    private final JsonValue inputTokenState;
    private final JsonValue outputTokenState;

    private RestSTSServiceInvocationState(RestSTSServiceInvocationStateBuilder builder) throws TokenMarshalException {
        this.inputTokenState = builder.inputTokenState;
        this.outputTokenState = builder.outputTokenState;

        if ((inputTokenState ==  null) || inputTokenState.isNull()) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Input token state must be set!");
        }

        if ((outputTokenState ==  null) || outputTokenState.isNull()) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Output token state must be set!");
        }
    }

    public static RestSTSServiceInvocationStateBuilder builder() {
        return new RestSTSServiceInvocationStateBuilder();
    }

    public JsonValue getInputTokenState() {
        return inputTokenState;
    }

    public JsonValue getOutputTokenState() {
        return outputTokenState;
    }

    public JsonValue toJson() {
        return json(object(field(INPUT_TOKEN_STATE, inputTokenState), field(OUTPUT_TOKEN_STATE, outputTokenState)));
    }
    @Override
    public boolean equals(Object other) {
        if (other instanceof RestSTSServiceInvocationState) {
            RestSTSServiceInvocationState otherState = (RestSTSServiceInvocationState)other;
            return toJson().toString().equals(otherState.toJson().toString());
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

    public static RestSTSServiceInvocationState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        return builder()
                .inputTokenState(jsonValue.get(INPUT_TOKEN_STATE))
                .outputTokenState(jsonValue.get(OUTPUT_TOKEN_STATE))
                .build();
    }
}
