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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025-2026 3A Systems LLC.
 */

package org.forgerock.openidconnect.restlet;

import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.RelativeRedirectUriException;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.IdTokenHintValidator;
import org.forgerock.openidconnect.IdTokenHintValidator.VerifiedIdTokenHint;
import org.forgerock.openidconnect.OpenIDConnectEndSession;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.net.URI;

/**
 * Handles requests to the OpenId Connect end session endpoint for ending OpenId Connect user sessions.
 *
 * @since 11.0.0
 */
public class EndSession extends ServerResource {
    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final OAuth2RequestFactory requestFactory;
    private final OpenIDConnectEndSession openIDConnectEndSession;
    private final ExceptionHandler exceptionHandler;
    private final IdTokenHintValidator idTokenHintValidator;

    /**
     * Constructs a new EndSession.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param openIDConnectEndSession An instance of the OpenIDConnectEndSession.
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param idTokenHintValidator An instance of the IdTokenHintValidator.
     */
    @Inject
    public EndSession(OAuth2RequestFactory requestFactory, OpenIDConnectEndSession openIDConnectEndSession,
            ExceptionHandler exceptionHandler, IdTokenHintValidator idTokenHintValidator) {
        this.requestFactory = requestFactory;
        this.openIDConnectEndSession = openIDConnectEndSession;
        this.exceptionHandler = exceptionHandler;
        this.idTokenHintValidator = idTokenHintValidator;
    }

    /**
     * Handles GET requests to the OpenId Connect end session endpoint for ending OpenId Connect user sessions.
     *
     * @return The OpenId Connect token of the session that has ended.
     * @throws OAuth2RestletException If an error occurs whilst ending the users session.
     */
    @Get
    public Representation endSession() throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());
        final String idToken = request.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT);
        final String redirectUri = request.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI);
        final String state = request.getParameter(OAuth2Constants.Params.STATE);

        try {
            // GHSA-6f8c-crwq-jqm3: verify the hint before anything is read out of it. Both of the
            // decisions below - which session to destroy, and whose registered redirect URIs are
            // acceptable - used to be taken from claims of an unverified JWT, which the caller
            // could write freely.
            final VerifiedIdTokenHint hint = idTokenHintValidator.validate(request, idToken);

            try {
                openIDConnectEndSession.endSession(request, hint.getJwt());
            } catch (ServerException e) {
                this.logger.warn("Error while removing session, possibly already timed out. Skipping...", e);
            }

            if (StringUtils.isNotEmpty(redirectUri)) {
                return handleRedirect(hint.getClientRegistration(), redirectUri, state);
            }
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
        return null;
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

    private Representation handleRedirect(ClientRegistration client, String redirectUri, String state)
            throws RedirectUriMismatchException, RelativeRedirectUriException {

        validateRedirect(client, redirectUri);
        Response response = getResponse();

	Reference redirectUrlWithState = new Reference(redirectUri);
	if (state != null && !state.isEmpty()) {
		redirectUrlWithState.addQueryParameter(OAuth2Constants.Params.STATE, state);
	}

        new Redirector(getContext(), redirectUrlWithState.toString(), Redirector.MODE_CLIENT_FOUND).
                handle(getRequest(), response);
        return response == null ? null : response.getEntity();
    }

    private void validateRedirect(ClientRegistration client, String redirectUri)
            throws RedirectUriMismatchException, RelativeRedirectUriException {

        final URI requestedUri;
        try {
            requestedUri = URI.create(redirectUri);
        } catch (IllegalArgumentException e) {
            // A post_logout_redirect_uri that is not a URI at all matches nothing the client
            // registered. Left to propagate this would reach doCatch as a server error, on an
            // unauthenticated endpoint, after the session has already been ended.
            logger.warn("The post_logout_redirect_uri supplied to the endSession endpoint is not a URI");
            logger.debug("The post_logout_redirect_uri supplied to the endSession endpoint is not a URI", e);
            throw new RedirectUriMismatchException();
        }

        if (!requestedUri.isAbsolute()) {
            throw new RelativeRedirectUriException();
        }
        if (!client.getPostLogoutRedirectUris().contains(requestedUri)) {
            throw new RedirectUriMismatchException();
        }
    }

}
