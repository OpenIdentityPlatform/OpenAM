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

package org.forgerock.oauth2.core;

import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.splitResponseType;
import static org.forgerock.oauth2.core.Utils.splitScope;

/**
 * Represents the response of the users consent to gain authorization to access a particular resource.
 *
 * @since 12.0.0
 */
public class UserConsentResponse {

    /**
     * UserConsentResponse builder for creating an UserConsentResponse for the 'authorize' endpoint.
     *
     * @since 12.0.0
     */
    public static final class UserConsentResponseBuilder {

        private boolean consentGiven;
        private boolean saveConsent;
        private String scope;
        private String state;
        private String nonce;
        private String responseType;
        private String redirectUri;
        private String clientId;
        private Map<String, Object> context;
        private AuthenticationHandler authenticationHandler;

        /**
         * Constructs a new UserConsentResponseBuilder.
         */
        private UserConsentResponseBuilder() { }

        /**
         * Adds whether the user has given consent to the builder.
         *
         * @param consentGiven {@code true} if the user has given their consent.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder consentGiven(final boolean consentGiven) {
            this.consentGiven = consentGiven;
            return this;
        }

        /**
         * Adds whether the user wishes that their consent is saved to the builder.
         *
         * @param saveConsent {@code true} if the user want their consent to be saved.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder saveConsent(final boolean saveConsent) {
            this.saveConsent = saveConsent;
            return this;
        }

        /**
         * Adds the requested scope to the builder.
         *
         * @param scope The requested scope.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Adds the state information to the builder.
         *
         * @param state The state.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder state(final String state) {
            this.state = state;
            return this;
        }

        /**
         * Adds the nonce to the builder.
         *
         * @param nonce The nonce.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder nonce(final String nonce) {
            this.nonce = nonce;
            return this;
        }

        /**
         * Adds the requested response types to the builder.
         *
         * @param responseType The requested response types.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder responseType(final String responseType) {
            this.responseType = responseType;
            return this;
        }

        /**
         * Adds the redirect URI to the builder.
         *
         * @param redirectUri The redirect URI.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder redirectUri(final String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        /**
         * Adds the client's identifier to the builder.
         *
         * @param clientId The client's identifier.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Adds the {@code Map<String, Object>} containing OAuth2 Provider implementation specific context information
         * to the builder.
         *
         * @param context The {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
         *                information.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder context(final Map<String, Object> context) {
            this.context = context;
            return this;
        }

        /**
         * Adds the authentication handler to the builder.
         *
         * @param authenticationHandler The authentication handler.
         * @return This builder instance.
         */
        public UserConsentResponseBuilder authenticationHandler(final AuthenticationHandler authenticationHandler) {
            this.authenticationHandler = authenticationHandler;
            return this;
        }

        /**
         * Builds the UserConsentResponse from the provided information.
         * <br/>
         * Validates that the builder has been given all the required data to be able to build an UserConsentResponse.
         *
         * @return An UserConsentResponse instance.
         */
        public UserConsentResponse build() {
            return new UserConsentResponse(consentGiven, saveConsent, splitScope(scope), state, nonce,
                    splitResponseType(responseType), redirectUri, clientId, context, authenticationHandler);
        }
    }

    /**
     * Creates a new builder for creating an UserConsentResponse.
     *
     * @return A new instance of the UserConsentResponseBuilder.
     */
    public static UserConsentResponseBuilder createUserConsentResponse() {
        return new UserConsentResponseBuilder();
    }

    private final boolean consentGiven;
    private final boolean saveConsent;
    private final Set<String> scope;
    private final String state;
    private final String nonce;
    private final Set<String> responseType;
    private final String redirectUri;
    private final String clientId;
    private final Map<String, Object> context;
    private final AuthenticationHandler authenticationHandler;

    /**
     * Constructs a new UserConsentResponse for the 'authorize' endpoint.
     *
     * @param consentGiven {@code true} if the user has given their consent.
     * @param saveConsent {@code true} if the user wants their consent to be saved.
     * @param scope The requested scope.
     * @param state The state information.
     * @param nonce The nonce.
     * @param responseType The requested response types.
     * @param redirectUri The redirect URI.
     * @param clientId The client's identifier.
     * @param context The {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @param authenticationHandler The authentication handler.
     */
    private UserConsentResponse(final boolean consentGiven, final boolean saveConsent, final Set<String> scope,
            final String state, final String nonce, final Set<String> responseType, final String redirectUri,
            final String clientId, final Map<String, Object> context,
            final AuthenticationHandler authenticationHandler) {
        this.consentGiven = consentGiven;
        this.saveConsent = saveConsent;
        this.scope = scope;
        this.state = state;
        this.nonce = nonce;
        this.responseType = responseType;
        this.redirectUri = redirectUri;
        this.clientId = clientId;
        this.context = context;
        this.authenticationHandler = authenticationHandler;
    }

    /**
     * Whether the user has given their consent.
     *
     * @return {@code true} if the user has given their consent.
     */
    public boolean isConsentGiven() {
        return consentGiven;
    }

    /**
     * Whether the user wants their consent to be saved.
     *
     * @return {@code true} if the user wants their consent to be saved.
     */
    public boolean isSaveConsent() {
        return saveConsent;
    }

    /**
     * Gets the requested scope.
     *
     * @return The requested scope.
     */
    public Set<String> getScope() {
        return scope;
    }

    /**
     * Gets the state information.
     *
     * @return The state information.
     */
    public String getState() {
        return state;
    }

    /**
     * Gets the nonce.
     *
     * @return The nonce.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Gets the requested response type.
     *
     * @return The requested response type.
     */
    public Set<String> getResponseType() {
        return responseType;
    }

    /**
     * Gets the redirect URI.
     *
     * @return The redirect URI.
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Gets the client's identifier.
     *
     * @return The client's identifier.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the {@code Map<String, Object>} containing OAuth2 Provider implementation specific context information.
     *
     * @return The {@code Map<String, Object>} containing OAuth2 Provider implementation specific context information.
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Gets the authentication handler.
     *
     * @return The authentication handler.
     */
    public AuthenticationHandler getAuthenticationHandler() {
        return authenticationHandler;
    }
}
