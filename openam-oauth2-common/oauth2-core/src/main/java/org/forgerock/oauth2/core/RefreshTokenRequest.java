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

import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.splitScope;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.REFRESH_TOKEN;

/**
 * Represents a request to refresh an access token.
 *
 * @since 12.0.0
 */
public class RefreshTokenRequest {

    /**
     * RefreshTokenRequest builder for creating an RefreshTokenRequest for the 'token' endpoint.
     *
     * @since 12.0.0
     */
    public static final class RefreshTokenRequestBuilder {

        private GrantType grantType;
        private String refreshToken;
        private String scope;
        private ClientCredentials clientCredentials;
        private Map<String, Object> context;

        /**
         * Constructs a new RefreshTokenRequestBuilder.
         */
        private RefreshTokenRequestBuilder() { }

        /**
         * Adds the grant type to the builder.
         *
         * @param grantType The grant type.
         * @return This builder instance.
         */
        private RefreshTokenRequestBuilder grantType(final GrantType grantType) {
            this.grantType = grantType;
            return this;
        }

        /**
         * Adds the refresh token to the builder.
         *
         * @param refreshToken The refresh token.
         * @return This builder instance.
         */
        public RefreshTokenRequestBuilder refreshToken(final String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        /**
         * Adds the requested scope to the builder.
         *
         * @param scope The requested scope.
         * @return This builder instance.
         */
        public RefreshTokenRequestBuilder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Adds the client's credentials to the builder.
         *
         * @param clientCredentials The client's credentials.
         * @return This builder instance.
         */
        public RefreshTokenRequestBuilder clientCredentials(final ClientCredentials clientCredentials) {
            this.clientCredentials = clientCredentials;
            return this;
        }

        /**
         * Adds the {@code Map<String, Object>} containing OAuth2 Provider implementation specific context information
         * to the builder.
         * <br/>
         * The context will be wrapped in an unmodifiable map.
         *
         * @param context The {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
         *                information.
         * @return This builder instance.
         */
        public RefreshTokenRequestBuilder context(final Map<String, Object> context) {
            this.context = Collections.unmodifiableMap(context);
            return this;
        }

        /**
         * Builds the RefreshTokenRequest from the provided information.
         * <br/>
         * Validates that the builder has been given all the required data to be able to build an RefreshTokenRequest.
         *
         * @return An RefreshTokenRequest instance.
         */
        public RefreshTokenRequest build() {
            Reject.ifTrue(refreshToken == null, "Missing parameter, 'refresh_token'");
            Reject.ifTrue(refreshToken.isEmpty(), "Missing parameter, 'refresh_token'");

            return new RefreshTokenRequest(grantType, refreshToken, splitScope(scope), clientCredentials, context);
        }
    }

    /**
     * Creates a new builder for creating an RefreshTokenRequest.
     *
     * @return A new instance of the RefreshTokenRequestBuilder.
     */
    public static RefreshTokenRequestBuilder createRefreshTokenRequest() {
        return new RefreshTokenRequestBuilder().grantType(REFRESH_TOKEN);
    }

    private final GrantType grantType;
    private final String refreshToken;
    private final Set<String> scope;
    private final ClientCredentials clientCredentials;
    private final Map<String, Object> context;

    /**
     * Constructs a new RefreshTokenRequest for the 'token' endpoint.
     *
     * @param grantType The grant type.
     * @param refreshToken The refresh token.
     * @param scope The requested scope.
     * @param clientCredentials The client's credentials.
     * @param context The {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     */
    private RefreshTokenRequest(final GrantType grantType, final String refreshToken, final Set<String> scope,
            final ClientCredentials clientCredentials, final Map<String, Object> context) {
        this.grantType = grantType;
        this.refreshToken = refreshToken;
        this.scope = scope;
        this.clientCredentials = clientCredentials;
        this.context = context;
    }

    /**
     * Gets the grant type.
     *
     * @return The grant type.
     */
    public GrantType getGrantType() {
        return grantType;
    }

    /**
     * Gets the client's credentials.
     *
     * @return The client's credentials.
     */
    public ClientCredentials getClientCredentials() {
        return clientCredentials;
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
     * Gets the refresh token.
     *
     * @return The refresh token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Gets the requested scope.
     *
     * @return The requested scope.
     */
    public Set<String> getScope() {
        return scope;
    }
}
