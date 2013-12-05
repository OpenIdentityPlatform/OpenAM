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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.filter;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.forgerockrest.RestDispatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class that matches a given HttpServletRequest against a set of endpoints.
 * <p>
 * Matches the Http Method used and the uri of the request and if a matching endpoint is found
 * then the QueryParamMatchers of that endpoint are matched against the requests query string.
 * <p>
 * All of the endpoints QueryParamMatchers must be matched against the requests query string for the request
 * to match against the endpoint.
 */
public class EndpointMatcher {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private final RestDispatcher restDispatcher;
    private final String rootPath;

    private final List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * Constructs a new EndpointMatcher.
     *
     * @param rootPath The path at which the filter is registered at.
     * @param restDispatcher An instance of the RestDispatcher.
     */
    public EndpointMatcher(String rootPath, RestDispatcher restDispatcher) {
        this.restDispatcher = restDispatcher;
        this.rootPath = rootPath;
    }

    /**
     * Matches the given HttpServletRequest against the configured endpoints.
     *
     * @param request The HttpServletRequest to match against the endpoints.
     * @return <code>true</code> if the request matches against one of the predefined endpoints.
     */
    public boolean match(HttpServletRequest request) {

        String path = getRequestPath(request);

        try {
            Map<String, String> details = restDispatcher.getRequestDetails(path);
            if (details.get("resourceId") != null) {
                path = details.get("resourceName") + "/" + details.get("resourceId");
            } else {
                path = details.get("resourceName");
            }
        } catch (NotFoundException e) {
            // Endpoint not found so return false
            DEBUG.message("Resource " + request.getPathInfo() + " not found.");
            return false;
        }

        Endpoint requestEndpoint = new Endpoint(path, request.getMethod());
        if (endpoints.contains(requestEndpoint)) {

            int index = endpoints.indexOf(requestEndpoint);
            Endpoint endpoint = endpoints.get(index);

            return matchQueryParameters(request, endpoint);
        }

        return false;
    }

    /**
     * Pulls the request's path from the request.
     *
     * @param request The HttpServletRequest to match against the endpoints.
     * @return The URI without the context path.
     */
    private String getRequestPath(HttpServletRequest request) {

        String path = request.getPathInfo();

        if (path != null) {
            return path;
        }

        String contextPath = request.getContextPath();

        String requestURI = request.getRequestURI();

        return requestURI.substring(contextPath.length()).replace(rootPath, "");
    }

    /**
     * Matches the requests query string against the  endpoints QueryParamMatchers.
     *
     * @param request The HttpServletRequest to match against the endpoints.
     * @param endpoint The endpoint to match the query string against.
     * @return <code>true</code> if the requests query string matches against each of the QueryParamMatchers.
     */
    private boolean matchQueryParameters(HttpServletRequest request, Endpoint endpoint) {

        Set<QueryParameterMatcher> queryParamMatchers = endpoint.getQueryParamMatchers();

        boolean matches = queryParamMatchers.isEmpty();
        for (QueryParameterMatcher queryParamMatcher : queryParamMatchers) {
            if (queryParamMatcher.match(request.getQueryString())) {
                matches = true;
                break;
            }
        }

        return matches;
    }

    /**
     * Adds an endpoint to the matcher.
     *
     * @param uri The URI of the endpoint.
     * @param httpMethod The HTTP method for the endpoint.
     */
    public void endpoint(String uri, String httpMethod) {
        endpoint(uri, httpMethod, null);
    }

    /**
     * Adds an endpoint to the matcher, with a given query parameter key and a set of acceptable values.
     *
     * @param uri The URI of the endpoint.
     * @param httpMethod The HTTP method for the endpoint.
     * @param queryParam The query parameter key that must be present and match one of the given values.
     * @param queryParamValues An array of possible values for the query paramter key.
     */
    public void endpoint(String uri, String httpMethod, String queryParam, String... queryParamValues) {
        Endpoint ePoint = new Endpoint(uri, httpMethod, queryParam, queryParamValues);
        endpoints.add(ePoint);
    }
}
