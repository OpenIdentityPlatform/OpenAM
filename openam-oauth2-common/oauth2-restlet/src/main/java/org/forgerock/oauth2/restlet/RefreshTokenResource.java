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
 */

package org.forgerock.oauth2.restlet;

import static java.util.Collections.singletonList;
import static org.forgerock.oauth2.restlet.RestletConstants.*;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientAuthZHeaderException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.restlet.Request;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Handles requests to the OAuth2 token endpoint for refreshing tokens.
 *
 * @since 12.0.0
 */
public class RefreshTokenResource extends ServerResource {
    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final AccessTokenService accessTokenService;
    private final ExceptionHandler exceptionHandler;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Constructs a new RefreshTokenResource.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param accessTokenService An instance of the AccessTokenService.
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param jacksonRepresentationFactory The factory for {@code JacksonRepresentation} instances.
     */
    @Inject
    public RefreshTokenResource(OAuth2RequestFactory<?, Request> requestFactory, AccessTokenService accessTokenService,
            ExceptionHandler exceptionHandler, JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.requestFactory = requestFactory;
        this.accessTokenService = accessTokenService;
        this.exceptionHandler = exceptionHandler;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * Handles POST requests to the OAuth2 token endpoint for the refresh token grant type.
     *
     * @param entity The entity on the request.
     * @return The body to be sent in the response to the user agent.
     * @throws OAuth2RestletException If a OAuth2 error occurs whilst processing the refresh token request.
     */
    @Post
    public Representation token(Representation entity) throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());
        try {
            final AccessToken accessToken = accessTokenService.refreshToken(request);
            return jacksonRepresentationFactory.create(accessToken.toMap());
        } catch (IllegalArgumentException e) {
            throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
        } catch (InvalidClientAuthZHeaderException e) {
            getResponse().setChallengeRequests(singletonList(
                    new ChallengeRequest(
                            ChallengeScheme.valueOf(SUPPORTED_RESTLET_CHALLENGE_SCHEMES.get(e.getChallengeScheme())),
                            e.getChallengeRealm())));
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (InvalidGrantException e) {
            logger.debug("Invalid grant presented for refresh token", e);
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), "grant is invalid",
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
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
