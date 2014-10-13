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

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.restlet.Request;
import org.restlet.engine.header.Header;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * Handles requests to the OAuth2 token endpoint for requesting access tokens.
 *
 * @since 12.0.0
 */
public class TokenEndpointResource extends ServerResource {

    private final OAuth2RequestFactory<Request> requestFactory;
    private final AccessTokenService accessTokenService;
    private final ExceptionHandler exceptionHandler;
    private final Set<TokenRequestHook> hooks;

    /**
     * Constructs a new instance of the TokenEndpointResource.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param accessTokenService An instance of the AccessTokenService.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public TokenEndpointResource(OAuth2RequestFactory<Request> requestFactory, AccessTokenService accessTokenService,
            ExceptionHandler exceptionHandler, Set<TokenRequestHook> hooks) {
        this.requestFactory = requestFactory;
        this.accessTokenService = accessTokenService;
        this.exceptionHandler = exceptionHandler;
        this.hooks = hooks;
    }

    /**
     * Handles POST requests to the OAuth2 token endpoint for the access token grant types (i.e. authorization_code,
     * client credentials, password).
     *
     * @param entity The entity on the request.
     * @return The body to be sent in the response to the user agent.
     * @throws OAuth2RestletException If a OAuth2 error occurs whilst processing the refresh token request.
     */
    @Post
    public Representation token(Representation entity) throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());
        try {
            final AccessToken accessToken = accessTokenService.requestAccessToken(request);

            for (TokenRequestHook hook : hooks) {
                hook.afterTokenHandling(request, getRequest(), getResponse());
            }

            return new JacksonRepresentation<Map<String, Object>>(accessToken.toMap());

        } catch (RedirectUriMismatchException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (IllegalArgumentException e) {
            throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
        } catch (ClientAuthenticationFailedException e) {
            Series<Header> responseHeaders =
                    (Series<Header>) getResponse().getAttributes().get("org.restlet.http.headers");
            if (responseHeaders == null) {
                responseHeaders = new Series(Header.class);
                getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
            }
            responseHeaders.add(new Header(e.getHeaderName(), e.getHeaderValue()));
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
        }
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 authorization request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }
}
