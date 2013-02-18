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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * JAX-RS endpoint for version 1 RESTful authentication requests.
 */
@Path("/1")
public class IdentityRestService {

    private final RestAuthenticationHandler restAuthenticationHandler;

    /**
     * Constructs an instance of the IdentityRestService.
     *
     * Used by the Jersey framework to create the instance.
     */
    public IdentityRestService() {
        restAuthenticationHandler = InjectorHolder.getInstance(RestAuthenticationHandler.class);
    }

    /**
     * Constructs an instance of the IdentityRestService.
     *
     * Used by tests to inject a mock RestAuthenticationHandler.
     *
     * @param restAuthenticationHandler An instance of the RestAuthenticationHandler.
     */
    public IdentityRestService(RestAuthenticationHandler restAuthenticationHandler) {
        this.restAuthenticationHandler = restAuthenticationHandler;
    }

    /**
     * Handles the initial RESTful call from clients to authenticate. Using the query parameters from the URL the
     * method starts the login process and either returns an SSOToken on successful authentication or a number of
     * Callbacks needing to be completed before authentication can proceed or an exception if any problems occurred
     * whilst trying to authenticate.
     *
     * @param headers The HttpHeaders of the RESTful call.
     * @param request The HttpServletRequest of the RESTful call.
     * @param authIndexType The authentication index type from the url parameters.
     * @param authIndexValue The authentication index value from the url parameters.
     * @param realm The realm to authenticate in from the url parameters.
     * @return A response to be sent back to the client. The response will contain either a JSON object containing the
     * SSOToken id from a successful authentication, a JSON object containing a number of Callbacks for the client to
     * complete and return or a JSON object containing an exception message.
     */
    @GET
    @Path("authenticate")
    @Produces("application/json")
    public Response authenticate(@Context HttpHeaders headers, @Context HttpServletRequest request,
            @QueryParam("authIndexType") String authIndexType, @QueryParam("authIndex") String authIndexValue,
            @QueryParam("realm") String realm) {

        return restAuthenticationHandler.authenticate(headers, request, realm, authIndexType, authIndexValue);
    }

    /**
     * Handles subsequent RESTful calls from clients submitting Callbacks for the authentication process to continue.
     * Using the body of the POST request the method continues the login process, submitting the given Callbacks and
     * then either returns an SSOToken on successful authentication or a number of additional Callbacks needing to be
     * completed before authentication can proceed or an exception if any problems occurred whilst trying to
     * authenticate.
     *
     * @param headers The HttpHeaders of the RESTful call.
     * @param request The HttpServletRequest of the RESTful call.
     * @param msgBody The POST body of the RESTful call.
     * @return A response to be sent back to the client. The response will contain either a JSON object containing the
     * SSOToken id from a successful authentication, a JSON object containing a number of Callbacks for the client to
     * complete and return or a JSON object containing an exception message.
     */
    @POST
    @Path("authenticate/submitReqs")
    @Consumes("application/json")
    @Produces("application/json")
    public Response submitAuthRequirements(@Context HttpHeaders headers, @Context HttpServletRequest request,
            String msgBody) {
        return restAuthenticationHandler.processAuthenticationRequirements(headers, request, msgBody);
    }
}
