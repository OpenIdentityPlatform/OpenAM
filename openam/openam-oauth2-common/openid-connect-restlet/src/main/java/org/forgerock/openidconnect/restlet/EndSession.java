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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidconnect.restlet;

import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openidconnect.OpenIDConnectEndSession;
import org.restlet.Request;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import javax.inject.Inject;

/**
 * Handles requests to the OpenId Connect end session endpoint for ending OpenId Connect user sessions.
 *
 * @since 11.0.0
 */
public class EndSession extends ServerResource {

    private final OAuth2RequestFactory<Request> requestFactory;
    private final OpenIDConnectEndSession openIDConnectEndSession;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new EndSession.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param openIDConnectEndSession An instance of the OpenIDConnectEndSession.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public EndSession(OAuth2RequestFactory<Request> requestFactory, OpenIDConnectEndSession openIDConnectEndSession,
            ExceptionHandler exceptionHandler) {
        this.requestFactory = requestFactory;
        this.openIDConnectEndSession = openIDConnectEndSession;
        this.exceptionHandler = exceptionHandler;
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
        try {
            openIDConnectEndSession.endSession(idToken);
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
}
