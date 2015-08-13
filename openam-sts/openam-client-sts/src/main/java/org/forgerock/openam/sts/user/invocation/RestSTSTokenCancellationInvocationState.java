package org.forgerock.openam.sts.user.invocation;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenMarshalException;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * Class which encapsulates the concerns of dispatching a token cancellation request referencing a rest-sts issued token
 */
public class RestSTSTokenCancellationInvocationState {
    public static final String CANCELLED_TOKEN_STATE = "cancelled_token_state";

    public static class RestSTSTokenCancellationInvocationStateBuilder {
        private JsonValue cancelledTokenState;

        private RestSTSTokenCancellationInvocationStateBuilder() {}

        /**
         *
         * @param cancelledTokenState The JsonValue corresponding to the to-be-cancelled token state. This is state must
         *                            specify a token_type field of either SAML2 or OPENIDCONNECT, and then either a
         *                            saml2_token or oidc_id_token key referencing the to-be-cancelled token state.
         * @return the builder
         */
        public RestSTSTokenCancellationInvocationStateBuilder cancelledTokenState(JsonValue cancelledTokenState) {
            this.cancelledTokenState = cancelledTokenState;
            return this;
        }

        /**
         *
         * @return a RestSTSTokenCancelledInvocationState instance whose json representation can consume restful token
         * cancellation
         * @throws TokenMarshalException if token state was not set correctly
         */
        public RestSTSTokenCancellationInvocationState build() throws TokenMarshalException {
            return new RestSTSTokenCancellationInvocationState(this);
        }
    }

    private final JsonValue cancelledTokenState;

    private RestSTSTokenCancellationInvocationState(RestSTSTokenCancellationInvocationStateBuilder builder) throws TokenMarshalException {
        this.cancelledTokenState = builder.cancelledTokenState;

        if ((cancelledTokenState ==  null) || cancelledTokenState.isNull()) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Validated token state must be set!");
        }
    }

    /**
     *
     * @return a builder to create the RestSTSTokenCancellationInvocationState
     */
    public static RestSTSTokenCancellationInvocationStateBuilder builder() {
        return new RestSTSTokenCancellationInvocationStateBuilder();
    }

    /**
     *
     * @return the json representation of the to-be-cancelled token state
     */
    public JsonValue getCancelledTokenState() {
        return cancelledTokenState;
    }

    /**
     *
     * @return the json representation of the invocation state
     */
    public JsonValue toJson() {
        return json(object(field(CANCELLED_TOKEN_STATE, cancelledTokenState)));
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RestSTSTokenCancellationInvocationState) {
            RestSTSTokenCancellationInvocationState otherState = (RestSTSTokenCancellationInvocationState)other;
            return cancelledTokenState.toString().equals(otherState.cancelledTokenState.toString());
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
     * @param jsonValue the json representation of token cancellation invocation state, compliant with the json emitted by
     *                  toJson in this class
     * @return the RestSTSTokenCancellationInvocationState instance corresponding to the jsonValue parameter
     * @throws TokenMarshalException if the json state was missing a required field.
     */
    public static RestSTSTokenCancellationInvocationState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        return builder()
                .cancelledTokenState(jsonValue.get(CANCELLED_TOKEN_STATE))
                .build();
    }
}
