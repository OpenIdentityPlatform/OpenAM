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

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.fluent.JsonValue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Encapsulates invocation-specific state necessary to generate an OpenIdConnect Id token. An instance of this class
 * will be encapsulated in the TokenGenerationServiceInvocationState when an OpenIdConnect token is generated.
 */
public class OpenIdConnectTokenGenerationState {
    public static class OpenIdConnectTokenGenerationStateBuilder {
        private String authenticationContextClassReference;
        private Set<String> authenticationMethodReferences;
        private long authenticationTimeInSeconds;
        private String nonce;

        private OpenIdConnectTokenGenerationStateBuilder() {
            authenticationMethodReferences = new HashSet<>();
        }

        public OpenIdConnectTokenGenerationStateBuilder authenticationContextClassReference(String authenticationContextClassReference) {
            this.authenticationContextClassReference = authenticationContextClassReference;
            return this;
        }

        public OpenIdConnectTokenGenerationStateBuilder authenticationMethodReferences(Set<String> authenticationMethodReferences) {
            if (authenticationMethodReferences != null) {
                this.authenticationMethodReferences.addAll(authenticationMethodReferences);
            }
            return this;
        }

        public OpenIdConnectTokenGenerationStateBuilder addAuthenticationMethodReference(String authenticationMethodReference) {
            if (authenticationMethodReference != null) {
                this.authenticationMethodReferences.add(authenticationMethodReference);
            }
            return this;
        }

        public OpenIdConnectTokenGenerationStateBuilder authenticationTimeInSeconds(long authenticationTime) {
            this.authenticationTimeInSeconds = authenticationTime;
            return this;
        }

        public OpenIdConnectTokenGenerationStateBuilder nonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public OpenIdConnectTokenGenerationState build() {
            return new OpenIdConnectTokenGenerationState(this);
        }
    }
    private static final String AUTHENTICATION_CONTEXT_CLASS_REFERENCE = "authenticationContextClassReference";
    private static final String AUTHENTICATION_METHOD_REFERENCES = "authenticationMethodReferences";
    private static final String AUTHENTICATION_TIME = "authenticationTimeInSeconds";
    private static final String NONCE = "nonce";

    private final String authenticationContextClassReference;
    private final Set<String> authenticationModeReferences;
    private final long authenticationTimeInSeconds;
    private final String nonce;

    private OpenIdConnectTokenGenerationState(OpenIdConnectTokenGenerationStateBuilder builder) {
        this.authenticationContextClassReference = builder.authenticationContextClassReference;
        this.authenticationModeReferences = Collections.unmodifiableSet(builder.authenticationMethodReferences);
        this.authenticationTimeInSeconds = builder.authenticationTimeInSeconds;
        this.nonce = builder.nonce;
        //all of these fields are optional
    }

    public static OpenIdConnectTokenGenerationStateBuilder builder() {
        return new OpenIdConnectTokenGenerationStateBuilder();
    }

    public String getAuthenticationContextClassReference() {
        return authenticationContextClassReference;
    }

    public Set<String> getAuthenticationModeReferences() {
        return authenticationModeReferences;
    }

    public long getAuthenticationTimeInSeconds() {
        return authenticationTimeInSeconds;
    }

    public String getNonce() {
        return nonce;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OpenIdConnectTokenGenerationState)) {
            return false;
        }
        OpenIdConnectTokenGenerationState otherState = (OpenIdConnectTokenGenerationState)other;
        return Objects.equal(authenticationContextClassReference, otherState.authenticationContextClassReference) &&
                Objects.equal(authenticationModeReferences, otherState.authenticationModeReferences) &&
                Objects.equal(nonce, otherState.nonce) &&
                authenticationTimeInSeconds == otherState.authenticationTimeInSeconds;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public JsonValue toJson() {
        return json(object(
                field(AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authenticationContextClassReference),
                field(AUTHENTICATION_METHOD_REFERENCES, authenticationModeReferences),
                field(NONCE, nonce),
                field(AUTHENTICATION_TIME, authenticationTimeInSeconds)));
    }

    public static OpenIdConnectTokenGenerationState fromJson(JsonValue json) {
       if (json == null) {
           return null;
       }
       return OpenIdConnectTokenGenerationState.builder()
               .authenticationMethodReferences(json.get(AUTHENTICATION_METHOD_REFERENCES).isSet() ?
                       json.get(AUTHENTICATION_METHOD_REFERENCES).asSet(String.class) : null)
               .authenticationContextClassReference(json.get(AUTHENTICATION_CONTEXT_CLASS_REFERENCE).isString() ?
                       json.get(AUTHENTICATION_CONTEXT_CLASS_REFERENCE).asString() : null)
               .authenticationTimeInSeconds(json.get(AUTHENTICATION_TIME).isNumber() ? json.get(AUTHENTICATION_TIME).asLong() : 0)
               .nonce(json.get(NONCE).asString())
               .build();
    }
}
