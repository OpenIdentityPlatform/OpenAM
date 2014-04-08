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

package org.forgerock.openam.oauth2.saml2.core;

import org.forgerock.oauth2.core.AccessTokenRequest;
import org.forgerock.oauth2.core.ClientCredentials;
import org.forgerock.oauth2.core.GrantType;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.splitScope;
import static org.forgerock.openam.oauth2.saml2.core.Saml2GrantType.SAML2;

/**
 *
 *
 * @since 12.0.0
 */
public final class Saml2AccessTokenRequest implements AccessTokenRequest {

    /**
     * AccessTokenRequest builder for creating an AccessTokenRequest for the OAuth2 SAML2 Extensions grant type.
     *
     * @since 12.0.0
     */
    public static class Saml2AccessTokenRequestBuilder {

        private final GrantType grantType = SAML2;
        private String scope;
        private String assertion;
        private ClientCredentials clientCredentials;
        private Map<String, Object> context;

        /**
         * Constructs a new Saml2AccessTokenRequestBuilder.
         */
        private Saml2AccessTokenRequestBuilder() { }

        /**
         * Adds the requested scope to the builder.
         *
         * @param scope The requested scope.
         * @return This builder instance.
         */
        public Saml2AccessTokenRequestBuilder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Adds the assertion to the builder.
         *
         * @param assertion The assertion.
         * @return This builder instance.
         */
        public Saml2AccessTokenRequestBuilder assertion(final String assertion) {
            this.assertion = assertion;
            return this;
        }

        /**
         * Adds the client's credentials to the builder.
         *
         * @param clientCredentials The client's credentials.
         * @return This builder instance.
         */
        public Saml2AccessTokenRequestBuilder clientCredentials(
                final ClientCredentials clientCredentials) {
            this.clientCredentials = clientCredentials;
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
        public Saml2AccessTokenRequestBuilder context(final Map<String, Object> context) {
            this.context = Collections.unmodifiableMap(context);
            return this;
        }

        /**
         * Builds the Saml2AccessTokenRequest from the provided information.
         * <br/>
         * Validates that the builder has been given all the required data to be able to build a
         * Saml2AccessTokenRequest for the OAuth2 SAML2 extension grant type.
         *
         * @return A Saml2AccessTokenRequest instance.
         */
        public Saml2AccessTokenRequest build() {
            Reject.ifNull(clientCredentials, "Client Authentication must be set.");
            Reject.ifTrue(clientCredentials.getClientId() == null, "Missing parameter, 'client_id'");
            Reject.ifTrue(clientCredentials.getClientId().isEmpty(), "Missing parameter, 'client_id'");
            Reject.ifTrue(assertion == null, "Missing parameter, 'assertion'");
            Reject.ifTrue(assertion.isEmpty(), "Missing parameter, 'assertion'");

            return new Saml2AccessTokenRequest(grantType, splitScope(scope), assertion, clientCredentials, context);
        }
    }

    /**
     * Creates a new builder for creating an AccessTokenRequest for the OAuth2 Client Credentials grant type.
     *
     * @return A new instance of the ClientCredentialsAccessTokenRequestBuilder.
     */
    public static Saml2AccessTokenRequestBuilder createSaml2AccessTokenRequestBuilder() {
        return new Saml2AccessTokenRequestBuilder();
    }

    private final GrantType grantType;
    private final Set<String> scope;
    private final String assertion;
    private final ClientCredentials clientCredentials;
    private final Map<String, Object> context;

    private Saml2AccessTokenRequest(final GrantType grantType, final Set<String> scope, final String assertion,
            final ClientCredentials clientCredentials, final Map<String, Object> context) {
        this.grantType = grantType;
        this.scope = scope;
        this.assertion = assertion;
        this.clientCredentials = clientCredentials;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrantType getGrantType() {
        return grantType;
    }

    public Set<String> getScope() {
        return scope;
    }

    public String getAssertion() {
        return assertion;
    }

    public ClientCredentials getClientCredentials() {
        return clientCredentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getContext() {
        return context;
    }
}
