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

import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.splitResponseType;
import static org.forgerock.oauth2.core.Utils.splitScope;

/**
 * Represents a request to gain an authorization to access a resource.
 *
 * @since 12.0.0
 */
public class AuthorizationRequest {

    /**
     * AuthorizationRequest builder for creating an AuthorizationRequest for the 'authorize' endpoint.
     *
     * @since 12.0.0
     */
    public static final class AuthorizationRequestBuilder {

        private String responseType;
        private String clientId;
        private String redirectUri;
        private String scope;
        private String state;
        private String prompt;
        private String locale;
        private ResourceOwnerAuthorizationCodeAuthenticationHandler authenticationHandler;
        private Map<String, Object> context;

        /**
         * Constructs a new AuthorizationCodeAccessTokenRequestBuilder.
         */
        private AuthorizationRequestBuilder() { }

        /**
         * Adds the client's identifier to the builder.
         *
         * @param clientId The client's identifier.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Adds the redirect uri to the builder.
         *
         * @param redirectUri The redirect uri.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder redirectUri(final String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        /**
         * Adds the requested scope to the builder.
         *
         * @param scope The requested scope.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Adds the state to the builder.
         *
         * @param state The state.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder state(final String state) {
            this.state = state;
            return this;
        }

        /**
         * Adds the requested response types to the builder.
         *
         * @param responseType The requested response types.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder responseType(final String responseType) {
            this.responseType = responseType;
            return this;
        }

        /**
         * Adds the OpenID prompt parameter to the builder.
         *
         * @param prompt The OpenID prompt parameter.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder prompt(final String prompt) { //TODO actually OpenID Connect...
            this.prompt = prompt;
            return this;
        }

        /**
         * Adds the locale to the builder.
         *
         * @param locale The locale.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder locale(final String locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Adds the authentication handler to the builder.
         *
         * @param authenticationHandler The authentication handler.
         * @return This builder instance.
         */
        public AuthorizationRequestBuilder authenticationHandler(
                final ResourceOwnerAuthorizationCodeAuthenticationHandler authenticationHandler) {
            this.authenticationHandler = authenticationHandler;
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
        public AuthorizationRequestBuilder context(final Map<String, Object> context) {
            this.context = Collections.unmodifiableMap(context); //TODO document will be made unmodifiable
            return this;
        }

        /**
         * Builds the AuthorizationRequest from the provided information.
         * <br/>
         * Validates that the builder has been given all the required data to be able to build an AuthorizationRequest.
         *
         * @return An AuthorizationRequest instance.
         * @throws BadRequestException If the OpenId prompt parameter is invalid.
         */
        public AuthorizationRequest build() throws BadRequestException {
            Reject.ifTrue(clientId == null, "Missing parameter, 'client_id'");
            Reject.ifTrue(clientId.isEmpty(), "Missing parameter, 'client_id'");
            Reject.ifTrue(responseType == null, "Missing parameter, 'response_type'");
            Reject.ifTrue(responseType.isEmpty(), "Missing parameter, 'response_type'");

            final OpenIDPromptParameter promptParam = new OpenIDPromptParameter(prompt);
            if (!promptParam.isValid()) {
                throw new BadRequestException("Prompt parameter is invalid");
            }

            return new AuthorizationRequest(splitResponseType(responseType), clientId, redirectUri, splitScope(scope),
                    state, promptParam, locale, authenticationHandler, context);
        }
    }

    /**
     * Creates a new builder for creating an AuthorizationRequest.
     *
     * @return A new instance of the AuthorizationRequestBuilder.
     */
    public static AuthorizationRequestBuilder createCodeAuthorizationRequest() {
        return new AuthorizationRequestBuilder();
    }

    private final Set<String> responseType;
    private final String clientId;
    private final String redirectUri;
    private final Set<String> scope;
    private final String state;
    private final OpenIDPromptParameter prompt;
    private final String locale;
    private final ResourceOwnerAuthorizationCodeAuthenticationHandler authenticationHandler;
    private final Map<String, Object> context;

    /**
     * Constructs a new AuthorizationRequest for the 'authorize' endpoint.
     *
     * @param responseType The requested response types.
     * @param clientId The client's identifier.
     * @param redirectUri The redirect uri.
     * @param scope The requested scope.
     * @param state The state.
     * @param prompt The OpenID prompt parameter.
     * @param locale The locale.
     * @param authenticationHandler The authentication handler.
     * @param context The {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     */
    private AuthorizationRequest(final Set<String> responseType, final String clientId, final String redirectUri,
            final Set<String> scope, final String state, final OpenIDPromptParameter prompt, final String locale,
            final ResourceOwnerAuthorizationCodeAuthenticationHandler authenticationHandler,
            final Map<String, Object> context) {
        this.responseType = responseType;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.state = state;
        this.prompt = prompt;
        this.locale = locale;
        this.authenticationHandler = authenticationHandler;
        this.context = context;
    }

    /**
     * Gets the requested response types.
     *
     * @return The requested response types.
     */
    public Set<String> getResponseType() {
        return responseType;
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
     * Gets the redirect URI.
     *
     * @return The redirect URI.
     */
    public String getRedirectUri() {
        return redirectUri;
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
     * Gets the state.
     *
     * @return The state.
     */
    public String getState() {
        return state;
    }

    /**
     * Gets the OpenID prompt parameter.
     *
     * @return The OpenID prompt parameter.
     */
    public OpenIDPromptParameter getPrompt() {
        return prompt;
    }

    /**
     * Gets the locale.
     *
     * @return The locale.
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Gets the Authentication Handler.
     *
     * @return The Authentication Handler.
     */
    public ResourceOwnerAuthorizationCodeAuthenticationHandler getAuthenticationHandler() {
        return authenticationHandler;
    }

    /**
     * Gets the {@code Map<String, Object>} containing OAuth2 Provider implementation specific context information.
     *
     * @return The {@code Map<String, Object>} containing OAuth2 Provider implementation specific context information.
     */
    public Map<String, Object> getContext() {
        return context;
    }
}
