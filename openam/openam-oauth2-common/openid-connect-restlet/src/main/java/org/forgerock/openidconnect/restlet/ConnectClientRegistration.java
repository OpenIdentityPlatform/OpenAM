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

/*
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */

package org.forgerock.openidconnect.restlet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationService;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Handles requests to the OpenId Connect client registration endpoint for registering and retrieving OpenId Connect
 * client registrations.
 *
 * @since 11.0.0
 */
public class ConnectClientRegistration extends ServerResource {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OpenIdConnectClientRegistrationService clientRegistrationService;
    private final OAuth2RequestFactory<Request> requestFactory;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new ConnectClientRegistration.
     *
     * @param clientRegistrationService An instance of the OpenIdConnectClientRegistrationService.
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public ConnectClientRegistration(OpenIdConnectClientRegistrationService clientRegistrationService,
            OAuth2RequestFactory<Request> requestFactory, ExceptionHandler exceptionHandler) {
        this.clientRegistrationService = clientRegistrationService;
        this.requestFactory = requestFactory;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Handles POST requests to the OpenId Connect client registration endpoint for creating OpenId Connect client
     * registrations.
     *
     * @param entity The representation of the client registration details.
     * @return The representation of the client registration details as created in the store.
     * @throws OAuth2RestletException If an error occurs whilst processing the client registration.
     */
    @Post
    public Representation createClient(Representation entity) throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());
        final ChallengeResponse authHeader = getRequest().getChallengeResponse();
        final String accessToken = authHeader != null ? authHeader.getRawValue() : null;

        try {
            final String deploymentUrl = getRequest().getHostRef().toString() + "/"
                    + getRequest().getResourceRef().getSegments().get(0);
            final JsonValue registration = clientRegistrationService.createRegistration(accessToken,
                    deploymentUrl, request);
            setStatus(Status.SUCCESS_CREATED);
            return new JsonRepresentation(registration.asMap());
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    /**
     * Handles GET requests to the OpenId Connect client registration endpoint for retrieving OpenId Connect client
     * registrations.
     *
     * @return The representation of the client registration details.
     * @throws OAuth2RestletException If an error occurs whilst retrieving the client registration.
     */
    @Get
    public Representation getClient() throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());
        final String clientId = request.getParameter(OAuth2Constants.OAuth2Client.CLIENT_ID);
        final String accessToken = getRequest().getChallengeResponse().getRawValue();

        try {
            final JsonValue registration = clientRegistrationService.getRegistration(clientId, accessToken, request);

            return new JsonRepresentation(registration.asMap());
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
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
