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

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.json.fluent.JsonValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for getting the configuration of the OpenId Connect provider.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenIDConnectProviderConfiguration {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new OpenIDConnectProviderConfiguration.
     *
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public OpenIDConnectProviderConfiguration(OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * Gets the OpenId configuration for the OpenId Connect provider.
     *
     * @param request The OAuth2 request.
     * @return A JsonValue representation of the OpenId configuration.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     *          or the OAuth2 provider.
     * @throws ServerException If any internal server error occurs.
     */
    public JsonValue getConfiguration(OAuth2Request request) throws UnsupportedResponseTypeException, ServerException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        final Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("version", providerSettings.getOpenIDConnectVersion());
        configuration.put("issuer", providerSettings.getOpenIDConnectIssuer());
        configuration.put("authorization_endpoint", providerSettings.getAuthorizationEndpoint());
        configuration.put("token_endpoint", providerSettings.getTokenEndpoint());
        configuration.put("userinfo_endpoint", providerSettings.getUserInfoEndpoint());
        configuration.put("check_session_iframe", providerSettings.getCheckSessionEndpoint());
        configuration.put("end_session_endpoint", providerSettings.getEndSessionEndpoint());
        configuration.put("jwks_uri", providerSettings.getJWKSUri());
        configuration.put("registration_endpoint", providerSettings.getClientRegistrationEndpoint());
        configuration.put("claims_supported", providerSettings.getSupportedClaims());
        configuration.put("response_types_supported",
                getResponseTypes(providerSettings.getAllowedResponseTypes().keySet()));
        configuration.put("subject_types_supported", providerSettings.getSupportedSubjectTypes());
        configuration.put("id_token_signing_alg_values_supported",
                providerSettings.getSupportedIDTokenSigningAlgorithms());

        return new JsonValue(configuration);
    }

    /**
     * Returns a {@code Set} of all of the possible combinations of the allowed response types.
     *
     * @param responseTypeSet The {@code Set} of the allowed response types.
     * @return A {@code Set} of the possible combinations of the allowed response types.
     */
    private Set<String> getResponseTypes(Set<String> responseTypeSet) {
        Set<String> responseTypes = new HashSet<String>(responseTypeSet);

        if (responseTypes.contains("code") && responseTypes.contains("token") &&
                responseTypes.contains("id_token")) {
            responseTypes.add("code token id_token");
        }

        if (responseTypes.contains("code") && responseTypes.contains("token")) {
            responseTypes.add("code token");
        }

        if (responseTypes.contains("code") && responseTypes.contains("id_token")) {
            responseTypes.add("code id_token");
        }

        if (responseTypes.contains("token") && responseTypes.contains("id_token")) {
            responseTypes.add("token id_token");
        }
        return responseTypes;
    }
}
