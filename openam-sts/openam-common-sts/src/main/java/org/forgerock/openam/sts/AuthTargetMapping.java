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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;


import org.forgerock.json.fluent.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Each deployed STS instance will be configured with a mapping which specifies the rest authN authIndexType and authIndexValue
 * against which a particular token type will be validated.
 * An instance of this class will be harvested from the UI elements configuring STS instances.
 *
 * See the org.forgerock.openam.forgerockrest.authn.core.AuthIndexType class for valid values for the authIndexType
 * below. The authIndexValue is the actual name of the service, module, etc. (for an authIndexType of "service" the
 * authIndexValue could be "ldapService").
 *
 * This class also allows for the creation of a Map<String, Object> to be associated with a given AuthTarget. This is
 * required to provide some context to the TokenAuthenticationRequestDispatcher<T> responsible for dispatching the
 * authentication request for token-type T to the OpenAM Rest authN context. For example the dispatcher for
 * OpenIdConnectIdTokens must know what header name should reference the Id Token, a value dependant upon the value
 * configured for the OpenAM OIDC authN module.
 */
public class AuthTargetMapping {
    public static final String AUTH_INDEX_TYPE = "authIndexType";
    public static final String AUTH_INDEX_VALUE = "authIndexValue";
    public static final String CONTEXT = "context";
    public static final String COLON = " : ";
    public static final String SEMICOLON = " ; ";

    public static class AuthTargetMappingBuilder {
        private final Map<Class<?>, AuthTarget> mappings = new HashMap<Class<?>, AuthTarget>();

        /**
         * Associates a particular token class with authIndexType and authIndexValue values. For the associated STS
         * instance, the particular token type will be authenticated against the Rest authN context specified by the
         * authIndexType and authIndexValue.
         */
        public AuthTargetMappingBuilder addMapping(Class<?> tokenClass, String authIndexType, String authIndexValue)  {
            mappings.put(tokenClass, new AuthTarget(authIndexType, authIndexValue));
            return this;
        }

        /**
         * Associates a particular token class with authIndexType and authIndexValue values. For the associated STS
         * instance, the particular token type will be authenticated against the Rest authN context specified by the
         * authIndexType and authIndexValue. The context will provide state to the dispatcher of the authN request necessary
         * by the associated authN module (e.g. the name of the header referencing the OpenID Connect ID Token).
         */
        public AuthTargetMappingBuilder addMapping(Class<?> tokenClass, String authIndexType, String authIndexValue, Map<String, Object> context)  {
            mappings.put(tokenClass, new AuthTarget(authIndexType, authIndexValue, context));
            return this;
        }

        public AuthTargetMapping build() {
            return new AuthTargetMapping(this);
        }
    }

    public static class AuthTarget {
        private final String authIndexType;
        private final String authIndexValue;
        private final Map<String, Object> context;

        AuthTarget(String authIndexType, String authIndexValue) {
            this(authIndexType, authIndexValue, null);
        }

        AuthTarget(String authIndexType, String authIndexValue, Map<String, Object> context) {
            if ((authIndexType == null) || (authIndexValue == null)) {
                throw new IllegalArgumentException(AUTH_INDEX_TYPE + " or " + AUTH_INDEX_VALUE + " were null!");
            }
            this.authIndexType = authIndexType;
            this.authIndexValue = authIndexValue;
            if (context != null) {
                this.context = Collections.unmodifiableMap(context);
            } else {
                this.context = null;
            }
        }

        public String getAuthIndexType() {
            return authIndexType;
        }

        public String getAuthIndexValue() {
            return authIndexValue;
        }

        public Map<String, Object> getContext() { return context; }

        @Override
        public String toString() {
            return AUTH_INDEX_TYPE + COLON + authIndexType + SEMICOLON + AUTH_INDEX_VALUE + COLON + authIndexValue +
                    SEMICOLON + CONTEXT + COLON +(context != null ? context.toString() : null);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof AuthTarget) {
                AuthTarget otherTarget = (AuthTarget)other;
                if ((context == null) && (otherTarget.context == null)) {
                    return authIndexType.equals(otherTarget.getAuthIndexType()) &&
                        authIndexValue.equals(otherTarget.getAuthIndexValue());
                } else if ((context != null) && (otherTarget.context != null)) {
                    return authIndexType.equals(otherTarget.getAuthIndexType()) &&
                            authIndexValue.equals(otherTarget.getAuthIndexValue()) &&
                            context.equals(otherTarget.context);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            //does not have to include the map as the authIndexType and authIndex value should be unique
            return (authIndexType + authIndexValue).hashCode();
        }

        JsonValue toJson() {
            if (context == null) {
                return json(object(field(AUTH_INDEX_TYPE, authIndexType), field(AUTH_INDEX_VALUE, authIndexValue)));
            } else {
                return json(object(field(AUTH_INDEX_TYPE, authIndexType), field(AUTH_INDEX_VALUE, authIndexValue),
                        field(CONTEXT, context)));
            }
        }

        static AuthTarget fromJson(JsonValue json) {
            if (json.get(CONTEXT) == null) {
                return new AuthTarget(json.get(AUTH_INDEX_TYPE).asString(), json.get(AUTH_INDEX_VALUE).asString());
            } else {
                return new AuthTarget(json.get(AUTH_INDEX_TYPE).asString(),
                        json.get(AUTH_INDEX_VALUE).asString(), json.get(CONTEXT).asMap());
            }
        }

    }
    private final Map<Class<?>, AuthTarget> mappings;

    private AuthTargetMapping(AuthTargetMappingBuilder builder) {
        this.mappings = Collections.unmodifiableMap(builder.mappings);
    }

    public static AuthTargetMappingBuilder builder() {
        return new AuthTargetMappingBuilder();
    }

    /*
      If a mapping is not present, null will be returned. This will allow the caller (e.g. the AuthenticationUriProvider)
      to know when to decorate the URI.
     */
    public AuthTarget getAuthTargetMapping(Class<?> tokenClass) {
        return mappings.get(tokenClass);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("AuthTargetMapping instance with mappings:\n");
        builder.append(mappings);
        return builder.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AuthTargetMapping) {
            AuthTargetMapping otherMapping = (AuthTargetMapping)other;
            return mappings.equals(otherMapping.mappings);
        }
        return false;
    }

    public JsonValue toJson() {
        JsonValue jsonMappings = new JsonValue(new HashMap<String, Object>());
        Map<String, Object> map = jsonMappings.asMap();
        for (Map.Entry<Class<?>, AuthTarget> entry : mappings.entrySet()) {
            map.put(entry.getKey().getName(), entry.getValue().toJson());
        }
        return jsonMappings;
    }

    public static AuthTargetMapping fromJson(JsonValue json) throws IllegalStateException {
        AuthTargetMappingBuilder builder = AuthTargetMapping.builder();
        if (!json.isMap()) {
            throw new IllegalArgumentException("JsonValue passed to AuthTargetMapping.fromJson not map!");
        }
        Map<String, Object> map = json.asMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            try {
                AuthTarget target = AuthTarget.fromJson(new JsonValue(entry.getValue()));
                builder.addMapping(Class.forName(entry.getKey()), target.getAuthIndexType(),
                        target.getAuthIndexValue(), target.getContext());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not fine class in AuthTargetMapping.fromJson corresponding to key: "
                        + entry.getKey());
            }
        }
        return builder.build();
    }
}
