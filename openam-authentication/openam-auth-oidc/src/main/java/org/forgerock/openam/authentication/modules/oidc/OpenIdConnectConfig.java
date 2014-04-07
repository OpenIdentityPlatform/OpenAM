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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.Reject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class contains all of the module configuration state.
 */
class OpenIdConnectConfig {
    private static Debug logger = Debug.getInstance("amAuth");

    static final String RESOURCE_BUNDLE_NAME = "amAuthOpenIdConnect";
    static final String HEADER_NAME_KEY = "openam-auth-openidconnect-header-name";
    static final String ISSUER_NAME_KEY = "openam-auth-openidconnect-issuer-name";
    static final String CRYPTO_CONTEXT_TYPE_KEY = "openam-auth-openidconnect-crypto-context-type";
    static final String CRYPTO_CONTEXT_VALUE_KEY = "openam-auth-openidconnect-crypto-context-value";
    static final String PRINCIPAL_MAPPER_CLASS_KEY = "openam-auth-openidconnect-principal-mapper-class";
    static final String LOCAL_TO_JWK_ATTRIBUTE_MAPPINGS_KEY = "openam-auth-openidconnect-local-to-jwt-attribute-mappings";

    static final String CRYPTO_CONTEXT_TYPE_CONFIG_URL = ".well-known/openid-configuration_url";
    static final String CRYPTO_CONTEXT_TYPE_JWK_URL = "jwk_url";
    static final String CRYPTO_CONTEXT_TYPE_CLIENT_SECRET = "client_secret";

    static final String BUNDLE_KEY_VERIFICATION_FAILED = "verification_failed";
    static final String BUNDLE_KEY_ISSUER_MISMATCH = "issuer_mismatch";
    static final String BUNDLE_KEY_TOKEN_ISSUER_MISMATCH = "token_issuer_mismatch";
    static final String BUNDLE_KEY_JWT_PARSE_ERROR = "jwt_parse_error";
    static final String BUNDLE_KEY_MISSING_HEADER = "missing_header";
    static final String BUNDLE_KEY_JWK_NOT_LOADED = "jwk_not_loaded";
    static final String BUNDLE_KEY_PRINCIPAL_MAPPER_INSTANTIATION_ERROR = "principal_mapper_instantiation_error";
    static final String BUNDLE_KEY_PRINCIPAL_MAPPING_FAILURE = "principal_mapping_failure";
    static final String BUNDLE_KEY_NO_ATTRIBUTES_MAPPED = "no_attributes_mapped";

    private static final String EQUALS = "=";

    private final String headerName;
    private final String configuredIssuer;
    private final String cryptoContextType;
    private final String cryptoContextValue;
    private final String principalMapperClass;
    private final URL cryptoContextUrlValue;
    private final Map<String, String> localToJwkAttributeMappings;


    OpenIdConnectConfig(Map options) {
        headerName = CollectionHelper.getMapAttr(options, HEADER_NAME_KEY);
        configuredIssuer = CollectionHelper.getMapAttr(options, ISSUER_NAME_KEY);
        cryptoContextType = CollectionHelper.getMapAttr(options, CRYPTO_CONTEXT_TYPE_KEY);
        cryptoContextValue = CollectionHelper.getMapAttr(options, CRYPTO_CONTEXT_VALUE_KEY);
        principalMapperClass = CollectionHelper.getMapAttr(options, PRINCIPAL_MAPPER_CLASS_KEY);
        Set<String> configuredLocalToJwkAttributeMappings = (Set<String>)options.get(LOCAL_TO_JWK_ATTRIBUTE_MAPPINGS_KEY);
        Reject.ifNull(headerName, HEADER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(configuredIssuer, ISSUER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(cryptoContextType, CRYPTO_CONTEXT_TYPE_KEY + " must be set in LoginModule options.");
        Reject.ifNull(cryptoContextValue, CRYPTO_CONTEXT_VALUE_KEY + " must be set in LoginModule options.");
        Reject.ifNull(principalMapperClass, PRINCIPAL_MAPPER_CLASS_KEY + " must be set in LoginModule options.");
        Reject.ifNull(configuredLocalToJwkAttributeMappings, LOCAL_TO_JWK_ATTRIBUTE_MAPPINGS_KEY + " must be set in LoginModule options.");
        Reject.ifTrue(configuredLocalToJwkAttributeMappings.isEmpty(), LOCAL_TO_JWK_ATTRIBUTE_MAPPINGS_KEY + " must contain some valid mappings.");
        localToJwkAttributeMappings = parseLocalToJwkMappings(configuredLocalToJwkAttributeMappings);
        Reject.ifFalse(CRYPTO_CONTEXT_TYPE_CLIENT_SECRET.equals(cryptoContextType) ||
                CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType) ||
                CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType), "The value corresponding to key " +
                CRYPTO_CONTEXT_TYPE_KEY + " does not correspond to an expected value. Its value:" + cryptoContextType);
        if (CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType) || CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType)) {
            try {
                cryptoContextUrlValue = new URL(cryptoContextValue);
            } catch (MalformedURLException e) {
                final String message = "The crypto context value string, " + cryptoContextValue + " is not in valid URL format: " + e;
                logger.error(message, e);
                throw new IllegalArgumentException(message);
            }
        } else {
            cryptoContextUrlValue = null;
        }
    }

    /*
    This method parses out the local_attribute=jkw_attributes as they are encapsulated in the authN module configurations
    into a more usable, Map<String, String> format.
     */
    Map<String, String> parseLocalToJwkMappings(Set<String> configuredMappings) {
        Map<String, String> parsedMappings = new HashMap<String, String>();
        for (String mapping : configuredMappings) {
            if (mapping.indexOf(EQUALS) == -1) {
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(mapping, EQUALS);
            final String key = tokenizer.nextToken();
            final String value = tokenizer.nextToken();
            /*
            The ldap_attr=jwt_attr mapping is user-generated, so I want to warn about duplicate entries. In a HashMap,
            repeated insertion of the same key will over-write previous entries, but I want to warn about duplicate
            entries, and will persist the first entry.
             */
            if (!parsedMappings.containsKey(key)) {
                parsedMappings.put(key, value);
            } else {
                logger.warning("In OpenIdConnectConfig.parseLocalToJwkMappings, the user-entered attribute mappings " +
                        "contain duplicate entries. The first entry will be preserved:  " + configuredMappings);
            }
        }
        if (parsedMappings.isEmpty()) {
            throw new IllegalArgumentException("The " + LOCAL_TO_JWK_ATTRIBUTE_MAPPINGS_KEY +
                    " Set does not contain any mappings in format local_attribute=jwk_attribute.");
        }
        return Collections.unmodifiableMap(parsedMappings);
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getConfiguredIssuer() {
        return configuredIssuer;
    }

    public String getCryptoContextType() {
        return cryptoContextType;
    }

    public String getCryptoContextValue() {
        return cryptoContextValue;
    }

    public String getPrincipalMapperClass() {
        return principalMapperClass;
    }

    public URL getCryptoContextUrlValue() {
        return cryptoContextUrlValue;
    }

    public Map<String, String> getLocalToJwkAttributeMappings() {
        return localToJwkAttributeMappings;
    }
}
