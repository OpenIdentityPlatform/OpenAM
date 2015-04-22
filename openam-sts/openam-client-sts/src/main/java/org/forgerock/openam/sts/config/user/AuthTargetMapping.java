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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.config.user;


import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

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
 * This class also allows for the creation of a {@code Map<String, Object>} to be associated with a given AuthTarget. This is
 * required to provide some context to the {@code TokenAuthenticationRequestDispatcher<T>} responsible for dispatching the
 * authentication request for token-type T to the OpenAM Rest authN context. For example the dispatcher for
 * OpenIdConnectIdTokens must know what header name should reference the Id Token, a value dependant upon the value
 * configured for the OpenAM OIDC authN module.
 *
 * Note that it is likely that the rest-sts (and possibly soap-sts) will support customers plugging-in token validation of
 * custom token types. Thus this class must support a means of registering AuthTarget instances for custom token types.
 * Thus the AuthTargetMapping will store the registered AuthTarget instances in a Map keyed by a String, the String
 * obtained by the TokenTypeId#getId. The TokenType enum implements the TokenTypeId interface, so existing TokenTypes
 * can be passed as usual, a scheme which can support the end-user registration of a custom implementation of the TokenTypeId,
 * a necessary step in the validation of a custom token type.
 *
 */
public class AuthTargetMapping {
    private static final String AUTH_INDEX_TYPE = "mapping-auth-index-type";
    private static final String AUTH_INDEX_VALUE = "mapping-auth-index-value";
    private static final String CONTEXT = "mapping-context";
    private static final String COMMA = ",";
    //public visibility as referenced from RestDeploymentConfig-need to leak reference as these constants correspond
    //to AttributeSchema names in restSTS.xml.
    public static final String AUTH_TARGET_MAPPINGS = "deployment-auth-target-mappings";
    private static final Map<String, String> NULL_MAP = null;

    public static class AuthTargetMappingBuilder {
        private final Map<String, AuthTarget> mappings = new HashMap<String, AuthTarget>();

        /**
         * Associates a particular token class with authIndexType and authIndexValue values. For the associated STS
         * instance, the particular token type will be authenticated against the Rest authN context specified by the
         * authIndexType and authIndexValue.
         *
         * The context will provides state to the dispatcher of the authN request necessary
         * by the associated authN module (e.g. the name of the header referencing the OpenID Connect ID Token).

         */
        /**
         * Associates a particular token type with authIndexType and authIndexValue values. For the associated STS
         * instance, the particular token type will be authenticated against the Rest authN context specified by the
         * authIndexType and authIndexValue.
         * @param tokenTypeId the token type identifier
         * @param authIndexType the authIndexType defining the authN target
         * @param authIndexValue the authIndexValue defining the authN target
         * @param context any context necessary for the {@code TokenAuthenticationRequestDispatcher<T> } to dispatch authN
         *                requests for the token type T against the specified authN target (e.g. the name of the header
         *                referencing the token).
         * @return the builder
         */
        public AuthTargetMappingBuilder addMapping(TokenTypeId tokenTypeId, String authIndexType, String authIndexValue, Map<String,String> context)  {
            mappings.put(tokenTypeId.getId(), new AuthTarget(authIndexType, authIndexValue, context));
            return this;
        }

        /**
         * Associates a particular token type with authIndexType and authIndexValue values. For the associated STS
         * instance, the particular token type will be authenticated against the Rest authN context specified by the
         * authIndexType and authIndexValue.
         * @param tokenTypeId the token type identifier
         * @param authIndexType the authIndexType defining the authN target
         * @param authIndexValue the authIndexValue defining the authN target
         * @return the builder
         */
        public AuthTargetMappingBuilder addMapping(TokenTypeId tokenTypeId, String authIndexType, String authIndexValue)  {
            return addMapping(tokenTypeId, authIndexType, authIndexValue, NULL_MAP);
        }

        /**
         * Private visibility, as this method will only be called when marshalling from Json. The bottom line is that
         * supporting custom token types via the TokenTypeId interface does not allow for the re-constitution of a
         * TokenTypeId instance given a string identifier, as works for Enum instances.
         * @param tokenTypeId the String token type identifier
         * @param authIndexType the authIndexType defining the authN target
         * @param authIndexValue the authIndexValue defining the authN target
         * @param context any context necessary for the {@code TokenAuthenticationRequestDispatcher<T> } to dispatch authN
         *                requests for the token type T against the specified authN target (e.g. the name of the header
         *                referencing the token).
         * @return the builder
         */
        private AuthTargetMappingBuilder addMapping(String tokenTypeId, String authIndexType, String authIndexValue, Map<String,String> context) {
            mappings.put(tokenTypeId, new AuthTarget(authIndexType, authIndexValue, context));
            return this;
        }

        public AuthTargetMapping build() {
            return new AuthTargetMapping(this);
        }
    }

    public static class AuthTarget {
        private final String authIndexType;
        private final String authIndexValue;
        private final Map<String, String> context;

        AuthTarget(String authIndexType, String authIndexValue) {
            this(authIndexType, authIndexValue, null);
        }

        AuthTarget(String authIndexType, String authIndexValue, Map<String, String> context) {
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

        public Map<String, String> getContext() { return context; }

        /*
        Method called in the context of marshalling to a Map<String,Object>, necessary for SMS persistence.
         */
        public String toSmsString() {
            StringBuilder valueBuilder = new StringBuilder(authIndexType).append(AMSTSConstants.PIPE).append(authIndexValue);
            if (context != null) {
                valueBuilder.append(AMSTSConstants.PIPE);
                int count = 0;
                for (Map.Entry<String, String> entry : context.entrySet()) {
                    if (count > 0) {
                        valueBuilder.append(COMMA);
                    }
                    valueBuilder.append(entry.getKey()).append(AMSTSConstants.EQUALS).append(entry.getValue());
                    count++;
                }
            }
            return valueBuilder.toString();
        }

        @Override
        public String toString() {
            return toSmsString();
        }

        /*
        Called in the context of marshalling from the Map<String, Object> representation, back to the AuthTarget representation.
        module;module_name  OR
        service;service_name;context_key1=context_value1,context_key_2=context_value2,

        No effort was made to catch NoSuchElementException, thrown from StringTokenizer#nextToken(), as the
        strings parsed in this method will only be generated by the toSmsString method in this class, and thus should
        be predictable.
         */
        static AuthTarget fromSmsString(String stringAuthTarget) {
            StringTokenizer topLevelTokenizer = new StringTokenizer(stringAuthTarget, AMSTSConstants.PIPE);
            String authIndexType = topLevelTokenizer.nextToken();
            String authIndexValue = topLevelTokenizer.nextToken();
            if (topLevelTokenizer.hasMoreTokens()) {
                Map<String, String> contextMap = new HashMap<String,String>();
                String contextToken = topLevelTokenizer.nextToken();
                StringTokenizer contextTokenizer = new StringTokenizer(contextToken, COMMA);
                while (contextTokenizer.hasMoreTokens()) {
                    StringTokenizer entryTokenizer = new StringTokenizer(contextTokenizer.nextToken(), AMSTSConstants.EQUALS);
                    contextMap.put(entryTokenizer.nextToken(), entryTokenizer.nextToken());
                }
                return new AuthTarget(authIndexType, authIndexValue, contextMap);
            } else {
                return new AuthTarget(authIndexType, authIndexValue);
            }
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
                        json.get(AUTH_INDEX_VALUE).asString(), MapMarshallUtils.objectValueToStringValueMap(json.get(CONTEXT).asMap()));
            }
        }

    }
    private final Map<String, AuthTarget> mappings;

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
    public AuthTarget getAuthTargetMapping(TokenTypeId tokenTypeId) {
        return mappings.get(tokenTypeId.getId());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, AuthTarget> entry : mappings.entrySet()) {
            builder.append(entry.getKey()).append(AMSTSConstants.PIPE).append(entry.getValue().toString()).append('\n');
        }
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
        for (Map.Entry<String, AuthTarget> entry : mappings.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toJson());
        }
        return jsonMappings;
    }

    public static AuthTargetMapping fromJson(JsonValue json) throws IllegalStateException {
        AuthTargetMappingBuilder builder = AuthTargetMapping.builder();
        if (!json.isMap()) {
            throw new IllegalArgumentException("JsonValue passed to AuthTargetMapping.fromJson not map. The json: "
                    + json.toString());
        }
        Map<String, Object> map = json.asMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            AuthTarget target = AuthTarget.fromJson(new JsonValue(entry.getValue()));
            builder.addMapping(entry.getKey(), target.getAuthIndexType(),
                    target.getAuthIndexValue(), target.getContext());
        }
        return builder.build();
    }

    public Map<String, Set<String>> marshalToAttributeMap() {
        HashSet<String> values = new HashSet<String>();
        HashMap<String, Set<String>> attributes = new HashMap<String, Set<String>>();
        attributes.put(AUTH_TARGET_MAPPINGS, values);
        for (Map.Entry<String, AuthTarget> entry : mappings.entrySet()) {
                values.add(entry.getKey() + AMSTSConstants.PIPE + entry.getValue().toSmsString());
        }
        return attributes;
    }

    public static AuthTargetMapping marshalFromAttributeMap(Map<String, Set<String>> attributes) {
        Set<String> authTargetMappings = attributes.get(AUTH_TARGET_MAPPINGS);
        if (authTargetMappings != null) {
            AuthTargetMappingBuilder builder = AuthTargetMapping.builder();
            for (String entry : authTargetMappings) {
                TokenType tokenType = TokenType.valueOf(entry.substring(0, entry.indexOf(AMSTSConstants.PIPE)));
                AuthTarget authTarget = AuthTarget.fromSmsString(entry.substring(entry.indexOf(AMSTSConstants.PIPE) + 1));
                builder.addMapping(tokenType, authTarget.getAuthIndexType(),
                        authTarget.getAuthIndexValue(), authTarget.getContext());
            }
            return builder.build();
        } else {
            throw new IllegalStateException("No value in attribute map corresponding to key " +
                    AUTH_TARGET_MAPPINGS);
        }
    }
}
