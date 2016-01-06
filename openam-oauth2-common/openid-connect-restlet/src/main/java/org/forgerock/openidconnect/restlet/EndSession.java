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
 */

package org.forgerock.openidconnect.restlet;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.RelativeRedirectUriException;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.OpenIDConnectEndSession;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;

import javax.inject.Inject;

import java.net.URI;

/**
 * Handles requests to the OpenId Connect end session endpoint for ending OpenId Connect user sessions.
 *
 * @since 11.0.0
 */
public class EndSession extends ServerResource {

    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final OpenIDConnectEndSession openIDConnectEndSession;
    private final ExceptionHandler exceptionHandler;
    private final ClientRegistrationStore clientRegistrationStore;

    /**
     * Constructs a new EndSession.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param openIDConnectEndSession An instance of the OpenIDConnectEndSession.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public EndSession(OAuth2RequestFactory<?, Request> requestFactory, OpenIDConnectEndSession openIDConnectEndSession,
            ExceptionHandler exceptionHandler, ClientRegistrationStore clientRegistrationStore) {
        this.requestFactory = requestFactory;
        this.openIDConnectEndSession = openIDConnectEndSession;
        this.exceptionHandler = exceptionHandler;
        this.clientRegistrationStore = clientRegistrationStore;
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
        try {
            openIDConnectEndSession.endSession(idToken);
            if (StringUtils.isNotEmpty(redirectUri)) {
                return handleRedirect(request, idToken, redirectUri);
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

    private Representation handleRedirect(OAuth2Request request, String idToken, String redirectUri)
            throws RedirectUriMismatchException, InvalidClientException, 
            RelativeRedirectUriException, NotFoundException {

        validateRedirect(request, idToken, redirectUri);
        Response response = getResponse();
        new Redirector(getContext(), new Reference(redirectUri).toString(), Redirector.MODE_CLIENT_FOUND).
                handle(getRequest(), response);
        return response == null ? null : response.getEntity();
    }

    private void validateRedirect(OAuth2Request request, String idToken, String redirectUri)
            throws InvalidClientException, RedirectUriMismatchException, 
            RelativeRedirectUriException, NotFoundException {

        SignedJwt jwt = new JwtReconstruction().reconstructJwt(idToken, SignedJwt.class);
        JwtClaimsSet claims = jwt.getClaimsSet();
        String clientId = (String) claims.getClaim(OAuth2Constants.JWTTokenParams.AZP);
        ClientRegistration client = clientRegistrationStore.get(clientId, request);
        URI requestedUri = URI.create(redirectUri);

        if (!requestedUri.isAbsolute()) {
            throw new RelativeRedirectUriException();
        }
        if (!client.getPostLogoutRedirectUris().contains(requestedUri)) {
            throw new RedirectUriMismatchException();
        }
    }

}
