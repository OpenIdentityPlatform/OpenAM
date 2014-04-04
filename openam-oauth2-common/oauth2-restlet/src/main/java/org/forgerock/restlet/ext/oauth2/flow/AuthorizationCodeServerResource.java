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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.restlet.ext.oauth2.flow;

import com.google.inject.Inject;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenRequest;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.ClientCredentials;
import org.forgerock.oauth2.core.ContextHandler;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.reslet.ClientCredentialsExtractor;
import org.forgerock.openam.oauth2.OAuth2ConfigurationFactory;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import java.util.Map;

import static org.forgerock.oauth2.core.AccessTokenRequest.createAuthorizationCodeAccessTokenRequest;

/**
 * Implements the Authorization Code Flow
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.1">4.1.  Authorization Code Grant</a>
 */
public class AuthorizationCodeServerResource extends AbstractFlow {

    private final ClientCredentialsExtractor clientCredentialsExtractor;
    private final AccessTokenService accessTokenService;
    private final ContextHandler contextHandler;

    @Inject
    public AuthorizationCodeServerResource(final ClientCredentialsExtractor clientCredentialsExtractor,
            final AccessTokenService accessTokenService, final ContextHandler contextHandler) {
        this.clientCredentialsExtractor = clientCredentialsExtractor;
        this.accessTokenService = accessTokenService;
        this.contextHandler = contextHandler;
    }

    @Post()
    public Representation token(Representation entity) {

        final String code = getAttribute("code");
        final String clientId = getAttribute("client_id");
        final String redirectUri = getAttribute("redirect_uri");

        final ClientCredentials clientCredentials;
        try {
            clientCredentials = clientCredentialsExtractor.extract(getRequest());
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest(), e.getMessage());
        }

        try {
            final AccessTokenRequest accessTokenRequest = createAuthorizationCodeAccessTokenRequest()
                    .clientCredentials(clientCredentials)
                    .code(code)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .context(contextHandler.createContext(ServletUtils.getRequest(getRequest())))
                    .build();

            final AccessToken accessToken = accessTokenService.requestAccessToken(accessTokenRequest);

            return new JacksonRepresentation<Map<String, Object>>(accessToken.toMap());

        } catch (IllegalArgumentException e) {
            //TODO log
//            OAuth2Utils.DEBUG.error("AbstractFlow::Invalid parameters in request: " + sb.toString());
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest(), e.getMessage());
        } catch (InvalidGrantException e) {
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest(), e.getMessage());
        } catch (InvalidCodeException e) {
            throw OAuthProblemException.OAuthError.INVALID_CODE.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (UnauthorizedClientException e) {
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(getRequest(), e.getMessage());
        } catch (RedirectUriMismatchException e) {
            throw OAuthProblemException.OAuthError.REDIRECT_URI_MISMATCH.handle(null, e.getMessage());
        } catch (OAuth2Exception e) {
            //CATCH ALL
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(), e.getMessage());
        }
    }
}
