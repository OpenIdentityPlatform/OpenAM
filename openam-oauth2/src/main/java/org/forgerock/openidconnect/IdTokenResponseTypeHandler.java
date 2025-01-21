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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResponseTypeHandler;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.utils.OpenAMSettings;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * Implementation of the ResponseTypeHandler for handling OpenId Connect token response types.
 *
 * @since 12.0.0
 */
@Singleton
public class IdTokenResponseTypeHandler implements ResponseTypeHandler {

    private final OpenIdConnectTokenStore tokenStore;
    private final OpenAMSettings openAMSettings;

    /**
     * Constructs a new IdTokenResponseTypeHandler.
     *
     * @param tokenStore An instance of the OpenIdConnectTokenStore.
     * @param openAMSettings An instance of the OpenAMSettings.
     */
    @Inject
    public IdTokenResponseTypeHandler(OpenIdConnectTokenStore tokenStore, OpenAMSettings openAMSettings) {
        this.tokenStore = tokenStore;
        this.openAMSettings = openAMSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map.Entry<String, Token> handle(String tokenType, Set<String> scope,
             ResourceOwner resourceOwner, String clientId, String redirectUri, String nonce, OAuth2Request request,
                                           String codeChallenge, String codeChallengeMethod)
            throws ServerException, InvalidClientException, NotFoundException {

        final OpenIdConnectToken openIDToken = tokenStore.createOpenIDToken(resourceOwner,
                clientId, clientId, nonce, getOps(request), request);

        return new AbstractMap.SimpleEntry<String, Token>("id_token", openIDToken);
    }

    /**
     * Gets the ops value for the OpenId Token.
     *
     * @param request The OAuth2 request.
     * @return The ops value.
     */
    private String getOps(OAuth2Request request) {
        final HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
        if (req.getCookies() != null) {
            final String cookieName = openAMSettings.getSSOCookieName();
            for (final Cookie cookie : req.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public OAuth2Constants.UrlLocation getReturnLocation() {
        return OAuth2Constants.UrlLocation.FRAGMENT;
    }
}
