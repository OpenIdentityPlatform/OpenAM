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

package org.forgerock.openam.sts.config.user;


import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;

import java.security.cert.X509Certificate;
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
 * This class also allows for the creation of a Map<String, Object> to be associated with a given AuthTarget. This is
 * required to provide some context to the TokenAuthenticationRequestDispatcher<T> responsible for dispatching the
 * authentication request for token-type T to the OpenAM Rest authN context. For example the dispatcher for
 * OpenIdConnectIdTokens must know what header name should reference the Id Token, a value dependant upon the value
 * configured for the OpenAM OIDC authN module.
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
        private final Map<Class<?>, AuthTarget> mappings = new HashMap<Class<?>, AuthTarget>();

        /**
         * Associates a particular token class with authIndexType and authIndexValue values. For the associated STS
         * instance, the particular token type will be authenticated against the Rest authN context specified by the
         * authIndexType and authIndexValue.
         *
         * The context will provide state to the dispatcher of the authN request necessary
         * by the associated authN module (e.g. the name of the header referencing the OpenID Connect ID Token).

         */
        private AuthTargetMappingBuilder addMapping(Class<?> tokenClass, String authIndexType, String authIndexValue, Map<String,String> context)  {
            mappings.put(tokenClass, new AuthTarget(authIndexType, authIndexValue, context));
            return this;
        }

        /**
         * Associates a particular token class with authIndexType and authIndexValue values. For the associated STS
         * instance, the particular token type will be authenticated against the Rest authN context specified by the
         * authIndexType and authIndexValue.
         *
         * This builder method was added to facilitate the creation of the AuthTargetMapping context from UI elements, where
         * the specification of a class is more problematic. The bottom line is that there must be correlation between
         * the classes bound to the AuthenticationHandler<T> and the TokenAuthenticationRequestDispatcher<T> types and those
         * referenced here. <T> is always represented as a class, so I can't define these mappings using the TokenType enum
         * instances, but I'd really like to be able to get to this mapping using TokenType values, as they can be easily
         * marshaled from the String context present in UI elements.
         *
         * And note that ultimately, some of the bound types will be dictated by the web-services-security context, as
         * the token validators that will be plugged-into the SOAP context will have to be defined in terms of the
         * web-service-security types. At the moment, this is only the org.apache.ws.security.message.token.UsernameToken class,
         * and the X509Certificate[] class.
         * In other words, I could not simply define a UsernameToken class in the model package, as I've done for the
         * OpenIdConnectIdToken class, but rather must re-use the type defined in wss4j, as this is ultimately the type
         * will will be passed to the token validation context plugged in the the soap-sts security-policy-enforcement
         * context.
         *
         * And note that the TokenType instance parameters will only correspond to types of tokens validated by the
         * STS as part of token transformations, AND those which may consume specific elements of the REST authN context.
         * This means that the OPENAM type will not be specified, as there is no customizable REST authN to be consumed
         * to validate a OpenAM session token, as this token can be validated globally - i.e. there are no realm-specific, or
         * custom, authN modules available to validate an OpenAM session token.
         */
        public AuthTargetMappingBuilder addMapping(TokenType tokenType, String authIndexType, String authIndexValue)  {
            switch (tokenType) {
                case OPENIDCONNECT:
                    return addMapping(OpenIdConnectIdToken.class, authIndexType, authIndexValue, NULL_MAP);
                case USERNAME:
                    return addMapping(UsernameToken.class, authIndexType, authIndexValue, NULL_MAP);
                case X509:
                    return addMapping(X509Certificate[].class, authIndexType, authIndexValue, NULL_MAP);
                default:
                    throw new IllegalArgumentException("Illegal TokenType provided to AuthTargetMappingBuilder.addMapping: "
                            + tokenType);
            }
        }

        public AuthTargetMappingBuilder addMapping(TokenType tokenType, String authIndexType, String authIndexValue, Map<String,String> context)  {
            switch (tokenType) {
                case OPENIDCONNECT:
                    return addMapping(OpenIdConnectIdToken.class, authIndexType, authIndexValue, context);
                case USERNAME:
                    return addMapping(UsernameToken.class, authIndexType, authIndexValue, context);
                case X509:
                    return addMapping(X509Certificate[].class, authIndexType, authIndexValue, context);
                default:
                    throw new IllegalArgumentException("Illegal TokenType provided to AuthTargetMappingBuilder.addMapping: "
                            + tokenType);
            }
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
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Class<?>, AuthTarget> entry : mappings.entrySet()) {
            builder.append(entry.getKey().getName()).append(AMSTSConstants.PIPE).append(entry.getValue().toString()).append('\n');
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
        for (Map.Entry<Class<?>, AuthTarget> entry : mappings.entrySet()) {
            map.put(entry.getKey().getName(), entry.getValue().toJson());
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

    public Map<String, Set<String>> marshalToAttributeMap() {
        HashSet<String> values = new HashSet<String>();
        HashMap<String, Set<String>> attributes = new HashMap<String, Set<String>>();
        attributes.put(AUTH_TARGET_MAPPINGS, values);
        for (Map.Entry<Class<?>, AuthTarget> entry : mappings.entrySet()) {
            try {
                values.add(mapClassToTokenType(Class.forName(entry.getKey().getName())) + AMSTSConstants.PIPE + entry.getValue().toSmsString());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("In AuthTargetMapping#marshalToAttributeMap, Could not find class " +
                        "corresponding to TokenType string " + entry.getKey().getName());
            }
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

    /*
    Externally, AuthTargetMapping instances are added by specifying the TokenType subject to the mapping. However, these
    mappings are represented internally as a class, as the validation context is parameterized by class. When I re-create
    these mapping instances from the attribute map, I need to go back from the class representation to the TokenType
    representation.
     */
    private static TokenType mapClassToTokenType(Class<?> clazz) {
        if (UsernameToken.class.equals(clazz)) {
            return TokenType.USERNAME;
        } else if (OpenIdConnectIdToken.class.equals(clazz)) {
            return TokenType.OPENIDCONNECT;
        } else if (X509Certificate[].class.equals(clazz)) {
            return TokenType.X509;
        } else {
            throw new IllegalArgumentException("The class to be mapped to a TokenType, " + clazz + " is unexpected.");
        }
    }
}
