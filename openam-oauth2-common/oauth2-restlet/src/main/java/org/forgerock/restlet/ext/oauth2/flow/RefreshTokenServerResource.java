/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All rights reserved.
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
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.restlet.ext.oauth2.flow;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.ClientCredentials;
import org.forgerock.oauth2.core.ContextHandler;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.RefreshTokenRequest;
import org.forgerock.oauth2.reslet.ClientCredentialsExtractor;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import javax.inject.Inject;
import java.util.Map;

import static org.forgerock.oauth2.core.RefreshTokenRequest.createRefreshTokenRequest;
import static org.forgerock.oauth2.reslet.RestletUtils.getParameter;

/**
 * Implements the Refresh Token Flow
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-6">6. Refreshing an Access Token</a>
 */
public class RefreshTokenServerResource extends AbstractFlow {

    private final ClientCredentialsExtractor clientCredentialsExtractor;
    private final AccessTokenService accessTokenService;
    private final ContextHandler contextHandler;

    @Inject
    public RefreshTokenServerResource(final ClientCredentialsExtractor clientCredentialsExtractor,
            final AccessTokenService accessTokenService, final ContextHandler contextHandler) {
        this.clientCredentialsExtractor = clientCredentialsExtractor;
        this.accessTokenService = accessTokenService;
        this.contextHandler = contextHandler;
    }

    @Post()
    public Representation represent(Representation entity) {

        final String refreshToken = getParameter(getRequest(), "refresh_token");
        final String scope = getParameter(getRequest(), "scope");

        final ClientCredentials clientCredentials;
        try {
            clientCredentials = clientCredentialsExtractor.extract(getRequest());
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest(), e.getMessage());
        }

        try {
            final RefreshTokenRequest refreshTokenRequest = createRefreshTokenRequest()
                    .refreshToken(refreshToken)
                    .scope(scope)
                    .clientCredentials(clientCredentials)
                    .context(contextHandler.createContext(ServletUtils.getRequest(getRequest())))
                    .build();

            final AccessToken accessToken = accessTokenService.refreshToken(refreshTokenRequest);

            return new JacksonRepresentation<Map<String, Object>>(accessToken.toMap());

        } catch (IllegalArgumentException e) {
            //TODO log
//            OAuth2Utils.DEBUG.error("AbstractFlow::Invalid parameters in request: " + sb.toString());
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (ExpiredTokenException e) {
            throw OAuthProblemException.OAuthError.EXPIRED_TOKEN.handle(getRequest(), e.getMessage());
        }
    }
}
