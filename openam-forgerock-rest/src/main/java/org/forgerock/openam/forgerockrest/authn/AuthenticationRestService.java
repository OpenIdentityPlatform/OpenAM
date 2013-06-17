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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn;

import com.google.inject.Singleton;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.guice.InjectorHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * JAX-RS endpoint for version 1 RESTful authentication requests.
 */
@Singleton
@Path("/1")
public class AuthenticationRestService {

    private final RestAuthenticationHandler restAuthenticationHandler;

    /**
     * Constructs an instance of the AuthenticationRestService.
     *
     * Used by the Jersey framework to create the instance.
     */
    public AuthenticationRestService() {
        this.restAuthenticationHandler = InjectorHolder.getInstance(RestAuthenticationHandler.class);
    }

    /**
     * Constructs an instance of the AuthenticationRestService.
     *
     * Used by tests to inject a mock RestAuthenticationHandler.
     *
     * @param restAuthenticationHandler An instance of the RestAuthenticationHandler.
     */
    public AuthenticationRestService(RestAuthenticationHandler restAuthenticationHandler) {
        this.restAuthenticationHandler = restAuthenticationHandler;
    }

    /**
     * Handles the initial GET RESTful call from clients to authenticate. Using the query parameters from the URL the
     * method starts the login process and either returns an SSOToken on successful authentication or a number of
     * Callbacks needing to be completed before authentication can proceed or an exception if any problems occurred
     * whilst trying to authenticate.
     *
     * Is to be used to initiate the authentication process but NOT for zero-page login. For providing all the
     * required information for a zero-page login see the authenticate method for POST http requests.
     *
     * @param headers The HttpHeaders of the RESTful call.
     * @param request The HttpServletRequest of the RESTful call.
     * @param response The HttpServletResponse of the RESTful call.
     * @param authIndexType The authentication index type from the url parameters.
     * @param authIndexValue The authentication index value from the url parameters.
     * @param sessionUpgradeSSOTokenId The SSO Token Id of the user's current session.
     * @return A response to be sent back to the client. The response will contain either a JSON object containing the
     * SSOToken id from a successful authentication, a JSON object containing a number of Callbacks for the client to
     * complete and return or a JSON object containing an exception message.
     */
    @GET
    @Path("authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(@Context HttpHeaders headers, @Context HttpServletRequest request,
            @Context HttpServletResponse response, @QueryParam("authIndexType") String authIndexType,
            @QueryParam("authIndexValue") String authIndexValue,
            @QueryParam("sessionUpgrade") String sessionUpgradeSSOTokenId) {

        return restAuthenticationHandler.initiateAuthentication(headers, request, response, authIndexType, authIndexValue,
                sessionUpgradeSSOTokenId, HttpMethod.GET);
    }

    /**
     * Handles both initial and subsequent RESTful calls from clients submitting Callbacks for the authentication
     * process to continue. This is determined by checking if the POST body is empty or not. If it is empty then this
     * is initiating the authentication process otherwise it is a subsequent call submitting Callbacks.
     *
     * Using the body of the POST request the method continues the login process, submitting the given Callbacks and
     * then either returns an SSOToken on successful authentication or a number of additional Callbacks needing to be
     * completed before authentication can proceed or an exception if any problems occurred whilst trying to
     * authenticate.
     *
     * @param headers The HttpHeaders of the RESTful call.
     * @param request The HttpServletRequest of the RESTful call.
     * @param response The HttpServletResponse of the RESTfull call.
     * @param authIndexType The authentication index type from the url parameters.
     * @param authIndexValue The authentication index value from the url parameters.
     * @param sessionUpgradeSSOTokenId The SSO Token Id of the user's current session.
     * @param postBody The body of the POST request.
     * @return A response to be sent back to the client. The response will contain either a JSON object containing the
     * SSOToken id from a successful authentication, a JSON object containing a number of Callbacks for the client to
     * complete and return or a JSON object containing an exception message.
     */
    @POST
    @Path("authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(@Context HttpHeaders headers, @Context HttpServletRequest request,
            @Context HttpServletResponse response, @QueryParam("authIndexType") String authIndexType,
            @QueryParam("authIndexValue") String authIndexValue,
            @QueryParam("sessionUpgrade") String sessionUpgradeSSOTokenId, String postBody) {

        if (postBody != null && !"".equals(postBody)) {
            //submitReqs
            return restAuthenticationHandler.continueAuthentication(headers, request, response, postBody,
                    sessionUpgradeSSOTokenId);
        } else {
            //initiate
            return restAuthenticationHandler.initiateAuthentication(headers, request, response, authIndexType, authIndexValue,
                    sessionUpgradeSSOTokenId, HttpMethod.POST);
        }
    }
}
