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

import java.util.Set;

import static org.forgerock.oauth2.core.ResponseType.DefaultResponseType.CODE;
import static org.forgerock.oauth2.core.ResponseType.DefaultResponseType.TOKEN;
import static org.forgerock.oauth2.core.Utils.splitScope;

/**
 * Represents a request to gain an authorization to access a resource.
 *
 * TODO not yet used until the Authorization Code flow and implicit flow have been updated to use the AuthorizationService.
 *
 * @since 12.0.0
 */
public class AuthorizationRequest {

    public static final class AuthorizationRequestBuilder {

        private final ResponseType responseType; //TODO how to get Grant type but still allow extension?...
        private String clientId;
        private String redirectUri;
        private String scope;
        private String state;

        private AuthorizationRequestBuilder(final ResponseType responseType) {
            this.responseType = responseType;
        }

        public AuthorizationRequestBuilder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        public AuthorizationRequestBuilder redirectUri(final String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public AuthorizationRequestBuilder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        public AuthorizationRequestBuilder state(final String state) {
            this.state = state;
            return this;
        }

        public AuthorizationRequest build() {
            //TODO verify params

            return new AuthorizationRequest(responseType, clientId, redirectUri, splitScope(scope), state);
        }
    }

    public static AuthorizationRequestBuilder createCodeAuthorizationRequest() {
        return new AuthorizationRequestBuilder(CODE);
    }

    public static AuthorizationRequestBuilder createTokenAuthorizationRequest() {
        return new AuthorizationRequestBuilder(TOKEN);
    }

    private final ResponseType responseType;
    private final String clientId;
    private final String redirectUri;
    private final Set<String> scope;
    private final String state;

    protected AuthorizationRequest(final ResponseType responseType, final String clientId, final String redirectUri,
            final Set<String> scope, final String state) {
        this.responseType = responseType;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.state = state;
    }
}
