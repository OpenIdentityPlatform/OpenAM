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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;

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
    private final OAuth2UrisFactory urisFactory;

    /**
     * Constructs a new OpenIDConnectProviderConfiguration.
     *
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param urisFactory An instance of OAuth2UrisFactory.
     */
    @Inject
    public OpenIDConnectProviderConfiguration(OAuth2ProviderSettingsFactory providerSettingsFactory,
            OAuth2UrisFactory urisFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
        this.urisFactory = urisFactory;
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
    public JsonValue getConfiguration(OAuth2Request request) throws OAuth2Exception {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final OAuth2Uris uris = urisFactory.get(request);

        if (!providerSettings.exists()) {
            throw new NotFoundException("Invalid URL");
        }

        final Map<String, Object> configuration = new HashMap<>();
        configuration.put("version", providerSettings.getOpenIDConnectVersion());
        configuration.put("issuer", uris.getIssuer());
        configuration.put("authorization_endpoint", uris.getAuthorizationEndpoint());
        configuration.put("token_endpoint", uris.getTokenEndpoint());
        configuration.put("userinfo_endpoint", uris.getUserInfoEndpoint());
        configuration.put("check_session_iframe", uris.getCheckSessionEndpoint());
        configuration.put("end_session_endpoint", uris.getEndSessionEndpoint());
        configuration.put("jwks_uri", uris.getJWKSUri());
        configuration.put("registration_endpoint", uris.getClientRegistrationEndpoint());
        configuration.put("claims_supported", providerSettings.getSupportedClaims());
        configuration.put("scopes_supported", providerSettings.getSupportedScopes());
        configuration.put("response_types_supported",
                getResponseTypes(providerSettings.getAllowedResponseTypes().keySet()));
        configuration.put("subject_types_supported", providerSettings.getSupportedSubjectTypes());
        configuration.put("id_token_signing_alg_values_supported",
                providerSettings.getSupportedIDTokenSigningAlgorithms());
        configuration.put("id_token_encryption_alg_values_supported",
                providerSettings.getSupportedIDTokenEncryptionAlgorithms());
        configuration.put("id_token_encryption_enc_values_supported",
                providerSettings.getSupportedIDTokenEncryptionMethods());
        configuration.put("acr_values_supported", providerSettings.getAcrMapping().keySet());
        configuration.put("claims_parameter_supported", providerSettings.getClaimsParameterSupported());
        configuration.put("token_endpoint_auth_methods_supported", providerSettings.getEndpointAuthMethodsSupported());
        configuration.put("device_authorization_endpoint", uris.getDeviceAuthorizationEndpoint());

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
