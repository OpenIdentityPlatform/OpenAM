/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2014 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.flow.responseTypes;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.TokenResponseTypeHandler;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.oauth2.legacy.CoreToken;
import org.forgerock.openam.oauth2.legacy.LegacyAccessTokenAdapter;
import org.forgerock.openam.oauth2.provider.ResponseType;
import org.restlet.Request;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

/**
 *
 * Implements the Implicit Flow
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.2">4.2.  Implicit Grant</a>
 *
 * @deprecated Use {@link org.forgerock.oauth2.core.TokenResponseTypeHandler} instead.
 */
@Deprecated
@Singleton
public class TokenResponseType implements ResponseType {

    private final TokenResponseTypeHandler handler;
    private final OAuth2RequestFactory<Request> requestFactory;

    @Inject
    public TokenResponseType(TokenResponseTypeHandler handler,
            OAuth2RequestFactory<Request> requestFactory) {
        this.handler = handler;
        this.requestFactory = requestFactory;
    }

    public CoreToken createToken(Token accessToken, Map<String, Object> data) {

        final String tokenType = (String) data.get(OAuth2Constants.CoreTokenParams.TOKEN_TYPE);
        final Set<String> scope = (Set<String>) data.get(OAuth2Constants.CoreTokenParams.SCOPE);
        final String resourceOwnerId = (String) data.get(OAuth2Constants.CoreTokenParams.USERNAME);
        final String clientId = (String) data.get(OAuth2Constants.CoreTokenParams.CLIENT_ID);
        final String redirectUri = (String) data.get(OAuth2Constants.CoreTokenParams.REDIRECT_URI);

        try {
            final Map.Entry<String,Token> tokenEntry = handler.handle(tokenType, scope, resourceOwnerId, clientId,
                    redirectUri, null, requestFactory.create(Request.getCurrent()));

            return new LegacyAccessTokenAdapter((AccessToken) tokenEntry.getValue());

        } catch (ServerException e) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(), e.getMessage());
        }
    }

    public String getReturnLocation(){
        return OAuth2Constants.UrlLocation.FRAGMENT.toString();
    }

    public String URIParamValue(){
        return "access_token";
    }
}
