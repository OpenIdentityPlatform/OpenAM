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
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
@Path("/")
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
     * HTTP GETs are not supported.
     * <p>
     * As JAX-RS has not direct way of defining a method type as not supported, here we define a method to be called
     * on all GET requests, that will return a 405 response "Method Not Allowed" back to the client.
     *
     * @return A HTTP 405 response to be sent back to the client.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMethodNotSupported() {

        JsonValue jsonValue = JsonValue.json(
                JsonValue.object(
                        JsonValue.field("code", 405),
                        JsonValue.field("reason", "Method Not Allowed"),
                        JsonValue.field("message", "HTTP method is not allowed.")
                )
        );
        return Response.status(405).entity(jsonValue.toString()).build();
    }

    /**
     * Handles both initial and subsequent RESTful calls from clients submitting Callbacks for the authentication
     * process to continue. This is determined by checking if the POST body is empty or not. If it is empty then this
     * is initiating the authentication process otherwise it is a subsequent call submitting Callbacks.
     *
     * Initiating authentication request using the query parameters from the URL starts the login process and either
     * returns an SSOToken on successful authentication or a number of Callbacks needing to be completed before
     * authentication can proceed or an exception if any problems occurred whilst trying to authenticate.
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
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(@Context HttpHeaders headers, @Context HttpServletRequest request,
            @Context HttpServletResponse response, @QueryParam("authIndexType") String authIndexType,
            @QueryParam("authIndexValue") String authIndexValue,
            @QueryParam("sessionUpgrade") String sessionUpgradeSSOTokenId, String postBody) {

        if (!isJsonContentType(headers)) {
            return new RestAuthException(Response.Status.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type")
                    .getResponse();
        }

        JsonValue jsonBody = null;
        if (StringUtils.isNotEmpty(postBody)) {
            jsonBody = JsonValueBuilder.toJsonValue(postBody);
        }

        if (jsonBody != null && jsonBody.size() > 0) {
            //submitReqs
            return restAuthenticationHandler.continueAuthentication(headers, request, response, jsonBody,
                    sessionUpgradeSSOTokenId);
        } else {
            //initiate
            return restAuthenticationHandler.initiateAuthentication(headers, request, response, authIndexType,
                    authIndexValue, sessionUpgradeSSOTokenId);
        }
    }

    /**
     * Check if the Http Headers contains the 'application/json' content type.
     *
     * @param headers The HttpHeaders.
     * @return <code>true</code> if the Content-Type header contains 'application/json'
     */
    private boolean isJsonContentType(HttpHeaders headers) {

        for (String contentType : headers.getRequestHeader(HttpHeaders.CONTENT_TYPE)) {
            if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON)) {
                return true;
            }
        }

        return false;
    }
}
