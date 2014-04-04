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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.restlet.ext.oauth2.flow;

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
import org.forgerock.oauth2.core.ResourceOwnerPasswordAuthenticationHandler;
import org.forgerock.oauth2.reslet.ResourceOwnerPasswordCredentialsExtractor;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.reslet.ClientCredentialsExtractor;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import javax.inject.Inject;
import java.util.Map;

import static org.forgerock.oauth2.core.AccessTokenRequest.createPasswordAccessTokenRequest;

/**
 * Implements the Resource Owner Password Credentials Flow
 *
 * @see <a
 *      href="http://tools.ietf.org/html/rfc6749#section-4.3>4.3.
 *      Resource Owner Password Credentials Grant</a>
 */
public class PasswordServerResource extends AbstractFlow {

    private final ClientCredentialsExtractor clientCredentialsExtractor;
    private final ResourceOwnerPasswordCredentialsExtractor resourceOwnerCredentialsExtractor;
    private final AccessTokenService accessTokenService;
    private final ContextHandler contextHandler;

    @Inject
    public PasswordServerResource(final ClientCredentialsExtractor clientCredentialsExtractor,
            final ResourceOwnerPasswordCredentialsExtractor resourceOwnerCredentialsExtractor,
            final AccessTokenService accessTokenService, final ContextHandler contextHandler) {
        this.clientCredentialsExtractor = clientCredentialsExtractor;
        this.resourceOwnerCredentialsExtractor = resourceOwnerCredentialsExtractor;
        this.accessTokenService = accessTokenService;
        this.contextHandler = contextHandler;
    }

    @Post                             //TODO document that this param is required even though not used as otherwise Restlet complains about incorrect content-type
    public Representation represent(final Representation entity) {

        final String scope = getAttribute("scope");

        final ClientCredentials clientCredentials;
        try {
            clientCredentials = clientCredentialsExtractor.extract(getRequest());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null, "Client authentication failed");
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(null, "Client authentication failed");
        }

        final ResourceOwnerPasswordAuthenticationHandler authenticationHandler;
//        try {
            authenticationHandler = resourceOwnerCredentialsExtractor.extract(getRequest());
//        } catch (InvalidClientException e) {
//                throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest());
//        } catch (InvalidRequestException e) {
//                throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest());
//        }

        try {
            final AccessTokenRequest accessTokenRequest = createPasswordAccessTokenRequest()
                    .clientCredentials(clientCredentials)
                    .authenticationHandler(authenticationHandler)
                    .scope(scope)
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
        } catch (UnauthorizedClientException e) {
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(getRequest(), e.getMessage());
        } catch (InvalidGrantException e) {
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InvalidCodeException e) {
            throw OAuthProblemException.OAuthError.INVALID_CODE.handle(getRequest(), e.getMessage());
        } catch (OAuth2Exception e) {
            //CATCH ALL
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(), e.getMessage());
        }
    }
}
