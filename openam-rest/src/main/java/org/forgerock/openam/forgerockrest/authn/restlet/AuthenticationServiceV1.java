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

package org.forgerock.openam.forgerockrest.authn.restlet;

import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.RestAuthenticationHandler;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.Reject;
import org.json.JSONException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

/**
 * Base Restlet class for server-side resources. It acts as a wrapper to a given call,
 * including the incoming {@link org.restlet.Request} and the outgoing {@link org.restlet.Response}.
 *
 * @since 12.0.0
 */
public class AuthenticationServiceV1 extends ServerResource {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String RESTLET_HEADERS_KEY = "org.restlet.http.headers";
    private static final String CACHE_CONTROL_HEADER_NAME = "Cache-Control";
    private static final String NO_CACHE_CACHE_CONTROL_HEADER = "no-cache, no-store, must-revalidate";
    private static final String PRAGMA_HEADER_NAME = "Pragma";
    private static final String PRAGMA_NO_CACHE_HEADER = "no-cache";
    private static final String EXPIRES_HEADER_NAME = "Expires";
    private static final String ALWAYS_EXPIRE_HEADER = "0";
    private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    private static final String REALM = "realm";

    private final RestAuthenticationHandler restAuthenticationHandler;

    /**
     * Constructs an instance of the AuthenticationRestService.
     *
     * @param restAuthenticationHandler An instance of the RestAuthenticationHandler.
     */
    @Inject
    public AuthenticationServiceV1(RestAuthenticationHandler restAuthenticationHandler) {
        this.restAuthenticationHandler = restAuthenticationHandler;
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
     * @param entity The Json Representation of the post body of the request.
     * @return A Json Representation of the response body. The response will contain either a JSON object containing the
     * SSOToken id from a successful authentication, a JSON object containing a number of Callbacks for the client to
     * complete and return or a JSON object containing an exception message.
     * @throws ResourceException If there is an error processing the authentication request.
     */
    @Post
    public Representation authenticate(JsonRepresentation entity) throws ResourceException {

        if (entity != null && !isSupportedMediaType(entity)) {
            if (DEBUG.errorEnabled()) {
                DEBUG.error("AuthenticationService :: Unable to handle media type request : " + entity.getMediaType());
            }
            return handleErrorResponse(
                    new Status(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type"), null);
        }

        final HttpServletRequest request = getHttpServletRequest();
        final HttpServletResponse response = getHttpServletResponse();

        final Map<String, String> urlQueryString = getUrlQueryString();
        final String sessionUpgradeSSOTokenId = urlQueryString.get("sessionUpgradeSSOTokenId");

        try {
            JsonValue jsonContent = getJsonContent(entity);
            JsonValue jsonResponse;

            if (jsonContent != null && jsonContent.size() > 0) {
                // submit requirements
                jsonResponse = restAuthenticationHandler.continueAuthentication(request, response, jsonContent,
                        sessionUpgradeSSOTokenId);
            } else {
                // initiate
                final String authIndexType = urlQueryString.get("authIndexType");
                final String authIndexValue = urlQueryString.get("authIndexValue");
                jsonResponse = restAuthenticationHandler.initiateAuthentication(request, response, authIndexType,
                        authIndexValue, sessionUpgradeSSOTokenId);
            }

            return createResponse(jsonResponse);

        } catch (RestAuthResponseException e) {
            DEBUG.message("AuthenticationService.authenticate() :: Exception from CallbackHandler", e);
            return handleErrorResponse(new Status(e.getStatusCode()), e);
        } catch (RestAuthException e) {
            DEBUG.message("AuthenticationService.authenticate() :: Rest Authentication Exception", e);
            return handleErrorResponse(Status.CLIENT_ERROR_UNAUTHORIZED, e);
        } catch (JSONException e) {
            DEBUG.message("AuthenticationService.authenticate() :: JSON parsing error", e);
            return handleErrorResponse(Status.CLIENT_ERROR_BAD_REQUEST, e);
        } catch (IOException e) {
            DEBUG.error("AuthenticationService.authenticate() :: Internal Error", e);
            return handleErrorResponse(Status.SERVER_ERROR_INTERNAL, e);
        }
    }

    private Map<String, String> getUrlQueryString() {
        return getReference().getQueryAsForm().getValuesMap();
    }

    private HttpServletResponse getHttpServletResponse() {
        return ServletUtils.getResponse(getResponse());
    }

    private boolean isSupportedMediaType(JsonRepresentation entity) {
        return MediaType.APPLICATION_JSON.equals(entity.getMediaType());
    }

    /**
     * Gets the HttpServletRequest from Restlet and wraps the HttpServletRequest with the URI realm as long as
     * the request does not contain the realm as a query parameter.
     *
     * @return The HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        final HttpServletRequest request = ServletUtils.getRequest(getRequest());

        // The request contains the realm query param then use that over any realm parsed from the URI
        final String queryParamRealm = request.getParameter(REALM);
        if (queryParamRealm != null && !queryParamRealm.isEmpty()) {
            return request;
        }

        return wrapRequest(request);
    }

    /**
     * Wraps the HttpServletRequest with the realm information used in the URI.
     *
     * @param request The HttpServletRequest.
     * @return The wrapped HttpServletRequest.
     */
    private HttpServletRequest wrapRequest(final HttpServletRequest request) {

        return new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                if (REALM.equals(name)) {
                    return (String) request.getAttribute(name);
                }
                return super.getParameter(name);
            }

            @Override
            public Map getParameterMap() {
                Map params = super.getParameterMap();
                Map p = new HashMap(params);
                p.put(REALM, request.getAttribute(REALM));
                return p;
            }

            @Override
            public Enumeration getParameterNames() {
                Set<String> names = new HashSet<String>();
                Enumeration<String> paramNames = super.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    names.add(paramNames.nextElement());
                }
                names.add(REALM);
                return Collections.enumeration(names);
            }

            @Override
            public String[] getParameterValues(String name) {
                if (REALM.equals(name)) {
                    return new String[]{(String) request.getAttribute(name)};
                }
                return super.getParameterValues(name);
            }
        };
    }

    /**
     * Creates a JsonValue from the Restlet Json Representation of the request post body.
     *
     * @param representation The Json Representation.
     * @return A JsonValue of the request posy body.
     * @throws JSONException If there is a problem parsing the Json Representation.
     */
    private JsonValue getJsonContent(final JsonRepresentation representation) throws JSONException {

        if (representation == null) {
            return null;
        }

        final String jsonString = representation.getJsonObject().toString();
        JsonValue jsonContent = null;
        if (StringUtils.isNotEmpty(jsonString)) {
            jsonContent = JsonValueBuilder.toJsonValue(jsonString);
        }

        return jsonContent;
    }

    /**
     * Creates a Restlet response representation from the given JsonValue.
     *
     * @param jsonResponse The Json response body.
     * @return a Restlet response representation.
     * @throws IOException If there is a problem creating the Json Representation.
     */
    private Representation createResponse(final JsonValue jsonResponse) throws IOException {

        addResponseHeader(CACHE_CONTROL_HEADER_NAME, NO_CACHE_CACHE_CONTROL_HEADER);
        addResponseHeader(PRAGMA_HEADER_NAME, PRAGMA_NO_CACHE_HEADER);
        addResponseHeader(EXPIRES_HEADER_NAME, ALWAYS_EXPIRE_HEADER);
        addResponseHeader(CONTENT_TYPE_HEADER_NAME, MediaType.APPLICATION_JSON.getName());

        final ObjectMapper mapper = JsonValueBuilder.getObjectMapper();
        return new JacksonRepresentation<Map>(mapper.readValue(jsonResponse.toString(), Map.class));
    }

    /**
     * Adds a response header to the response.
     *
     * @param key The Http Header name.
     * @param value The header value.
     */
    @SuppressWarnings("unchecked")
    protected void addResponseHeader(final String key, final String value) {
        Series<Header> headers = (Series<Header>) getResponse().getAttributes().get(RESTLET_HEADERS_KEY);
        if (headers == null) {
            headers = new Series(Header.class);
            getResponse().getAttributes().put(RESTLET_HEADERS_KEY, headers);
        }
        headers.add(new Header(key, value));
    }

    /**
     * Processes the given Exception into a Restlet response representation or wrap it into
     * a ResourceException, which will be thrown.
     *
     * @param status The status to set the response to.
     * @param exception The Exception to be handled.
     * @return The Restlet Response Representation.
     * @throws ResourceException If the given exception is wrapped in a ResourceException.
     */
    protected Representation handleErrorResponse(Status status, Exception exception) throws ResourceException {
        Reject.ifNull(status);
        Representation representation = null;
        final Map<String, Object> rep = new HashMap<String, Object>();

        if (exception instanceof RestAuthResponseException) {
            final RestAuthResponseException authResponseException = (RestAuthResponseException)exception;
            for (final String key : authResponseException.getResponseHeaders().keySet()) {
                addResponseHeader(key, authResponseException.getResponseHeaders().get(key));
            }
            representation = new JacksonRepresentation<Map>(authResponseException.getJsonResponse().asMap());

        } else if (exception instanceof RestAuthException) {
            final RestAuthException authException = (RestAuthException)exception;
            if (authException.getFailureUrl() != null) {
                rep.put("failureUrl", authException.getFailureUrl());
            }
            rep.put("errorMessage", exception.getMessage());

        } else if (exception == null) {
            rep.put("errorMessage", status.getDescription());
        } else {
            rep.put("errorMessage", exception.getMessage());
        }

        if (representation == null) {
            representation = new JsonRepresentation(rep);
        }
        getResponse().setStatus(status);

        return representation;
    }
}
