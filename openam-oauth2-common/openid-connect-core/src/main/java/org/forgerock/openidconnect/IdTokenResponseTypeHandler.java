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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResponseTypeHandler;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Implementation of the ResponseTypeHandler for handling OpenId Connect token response types.
 *
 * @since 12.0.0
 */
@Singleton
public abstract class IdTokenResponseTypeHandler implements ResponseTypeHandler {

    private final OpenIdConnectTokenStore tokenStore;

    /**
     * Constructs a new IdTokenResponseTypeHandler.
     *
     * @param tokenStore An instance of the OpenIdConnectTokenStore.
     */
    @Inject
    public IdTokenResponseTypeHandler(OpenIdConnectTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * {@inheritDoc}
     */
    public Map.Entry<String, Token> handle(String tokenType, Set<String> scope,
                                           String resourceOwnerId, String clientId, String redirectUri, String nonce,
                                           OAuth2Request request) throws ServerException, InvalidClientException {

        final OpenIdConnectToken openIDToken = tokenStore.createOpenIDToken(resourceOwnerId, clientId,
                clientId, nonce, getOps(request), request);

        return new AbstractMap.SimpleEntry<String, Token>("id_token", openIDToken);
    }

    /**
     * Gets the ops value for the OpenId Token.
     *
     * @param request The OAuth2 request.
     * @return The ops value.
     */
    protected abstract String getOps(OAuth2Request request);

    /**
     * {@inheritDoc}
     */
    public OAuth2Constants.UrlLocation getReturnLocation() {
        return OAuth2Constants.UrlLocation.FRAGMENT;
    }
}
