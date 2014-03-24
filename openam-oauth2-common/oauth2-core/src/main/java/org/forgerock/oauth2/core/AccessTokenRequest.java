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

import java.util.Set;

import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.AUTHORIZATION_CODE;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.CLIENT_CREDENTIALS;
import static org.forgerock.oauth2.core.GrantType.DefaultGrantType.PASSWORD;
import static org.forgerock.oauth2.core.Utils.splitScope;

/**
 * Represents a request to gain an OAuth2 Access Token.
 * <br/>
 * There is a builder object for each of the standard spec defined grant types.
 *
 * TODO not sure if it would be better/clearer if the AccessTokenRequest became an interface with an implementation for each grant type rather than having one class which only has some of all its variables filled out?...
 *
 * @since 12.0.0
 */
public class AccessTokenRequest {

    public static final class AuthorizationCodeAccessTokenRequestBuilder {

        private final GrantType grantType = AUTHORIZATION_CODE; //TODO how to get Grant type but still allow extension?...
        private String code;
        private String redirectUri;
        private String clientId;

        private AuthorizationCodeAccessTokenRequestBuilder() {}

        public AuthorizationCodeAccessTokenRequestBuilder code(final String code) {
            this.code = code;
            return this;
        }

        public AuthorizationCodeAccessTokenRequestBuilder redirectUri(final String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public AuthorizationCodeAccessTokenRequestBuilder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        public AccessTokenRequest build() {
            //TODO verify params

            return new AccessTokenRequest(grantType, code, redirectUri, clientId);
        }
    }

    public static AuthorizationCodeAccessTokenRequestBuilder createAuthorizationCodeAccessTokenRequest() {
        return new AuthorizationCodeAccessTokenRequestBuilder();
    }

    public static final class PasswordCredentialsAccessTokenRequestBuilder {

        private final GrantType grantType = PASSWORD;
        private ResourceOwnerAuthentication resourceOwnerAuthentication;
        private ClientAuthentication clientAuthentication;
        private String scope;

        private PasswordCredentialsAccessTokenRequestBuilder() {}

        public PasswordCredentialsAccessTokenRequestBuilder clientAuthentication(
                final ClientAuthentication clientAuthentication) {
            this.clientAuthentication = clientAuthentication;
            return this;
        }

        public PasswordCredentialsAccessTokenRequestBuilder resourceOwnerAuthentication(
                final ResourceOwnerAuthentication resourceOwnerAuthentication) {
            this.resourceOwnerAuthentication = resourceOwnerAuthentication;
            return this;
        }

        public PasswordCredentialsAccessTokenRequestBuilder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        public AccessTokenRequest build() {
            Reject.ifNull(clientAuthentication, "Client Authentication must be set.");
            Reject.ifTrue(clientAuthentication.getClientId() == null, "Missing parameter, 'client_id'");
            Reject.ifTrue(clientAuthentication.getClientId().isEmpty(), "Missing parameter, 'client_id'");
            Reject.ifNull(resourceOwnerAuthentication, "Resource Owner Authentication must be set.");
            Reject.ifTrue(resourceOwnerAuthentication.getUsername() == null, "Missing parameter, 'username'");
            Reject.ifTrue(resourceOwnerAuthentication.getUsername().isEmpty(), "Missing parameter, 'username'");
            Reject.ifTrue(resourceOwnerAuthentication.getPassword() == null, "Missing parameter, 'password'");
            Reject.ifTrue(resourceOwnerAuthentication.getPassword().length == 0, "Missing parameter, 'password'");

            return new AccessTokenRequest(grantType, clientAuthentication, resourceOwnerAuthentication, splitScope(scope));
        }
    }

    public static PasswordCredentialsAccessTokenRequestBuilder createPasswordAccessTokenRequest() {
        return new PasswordCredentialsAccessTokenRequestBuilder();
    }

    public static final class ClientCredentialsAccessTokenRequestBuilder {

        private final GrantType grantType = CLIENT_CREDENTIALS;
        private String scope;
        private ClientAuthentication clientAuthentication;

        private ClientCredentialsAccessTokenRequestBuilder() {}

        public ClientCredentialsAccessTokenRequestBuilder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        public ClientCredentialsAccessTokenRequestBuilder clientAuthentication(
                final ClientAuthentication clientAuthentication) {
            this.clientAuthentication = clientAuthentication;
            return this;
        }

        public AccessTokenRequest build() {
            Reject.ifNull(clientAuthentication, "Client Authentication must be set.");
            Reject.ifTrue(clientAuthentication.getClientId() == null, "Missing parameter, 'client_id'");
            Reject.ifTrue(clientAuthentication.getClientId().isEmpty(), "Missing parameter, 'client_id'");

            return new AccessTokenRequest(grantType, splitScope(scope), clientAuthentication);
        }
    }

    public static ClientCredentialsAccessTokenRequestBuilder createClientCredentialsAccessTokenRequest() {
        return new ClientCredentialsAccessTokenRequestBuilder();
    }

    private final GrantType grantType;
    private final String code;
    private final String redirectUri;
    private final String clientId;
    private final Set<String> scope;
    private final ClientAuthentication clientAuthentication;
    private final ResourceOwnerAuthentication resourceOwnerAuthentication;

    protected AccessTokenRequest(final GrantType grantType, final String code, final String redirectUri,
            final String clientId) {
        this.grantType = grantType;
        this.code = code;
        this.redirectUri = redirectUri;
        this.clientId = clientId;

        this.scope = null;
        this.clientAuthentication = null;
        this.resourceOwnerAuthentication = null;
    }

    protected AccessTokenRequest(final GrantType grantType, final ClientAuthentication clientAuthentication,
            final ResourceOwnerAuthentication resourceOwnerAuthentication, final Set<String> scope) {
        this.grantType = grantType;
        this.scope = scope;
        this.clientAuthentication = clientAuthentication;
        this.resourceOwnerAuthentication = resourceOwnerAuthentication;

        this.code = null;
        this.redirectUri = null;
        this.clientId = null;
    }

    protected AccessTokenRequest(final GrantType grantType, final Set<String> scope,
            final ClientAuthentication clientAuthentication) {
        this.grantType = grantType;
        this.scope = scope;
        this.clientAuthentication = clientAuthentication;

        this.code = null;
        this.redirectUri = null;
        this.clientId = null;
        this.resourceOwnerAuthentication = null;
    }

    public GrantType getGrantType() {
        return grantType;
    }

    public String getCode() {
        return code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getClientId() {
        return clientId;
    }

    public Set<String> getScope() {
        return scope;
    }

    public ClientAuthentication getClientAuthentication() {
        return clientAuthentication;
    }

    public ResourceOwnerAuthentication getResourceOwnerAuthentication() {
        return resourceOwnerAuthentication;
    }
}
