/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at:
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 *
 * See the License for the specific language governing permission and
 * limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at opensso/legal/CDDLv1.0.txt. If applicable,
 * add the following below the CDDL Header, with the fields enclosed by
 * brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * $Id: EntitlementFilter.java,v 1.4 2009/05/28 20:28:51 pbryan Exp $
 */

package com.sun.identity.entitlement.filter;

import java.io.IOException;
import java.util.regex.Pattern;
import java.security.Principal;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author Paul C. Bryan <pbryan@sun.com>
 */
public class EntitlementFilter implements Filter {

    // TODO: time to seriously consider switching to a configuration file?
    private static final String PARAM_DECISION_RESOURCE = "decisionResource";
    private static final String PARAM_IGNORE_PATH_PATTERN = "ignorePathPattern";
    public static final String PARAM_REALM = "realm";

    /** Jersey client to make REST calls to token services. */
    private Client client = Client.create();

    /** Servlet filter configuration (initialized in init method). */
    private FilterConfig config = null;

    /** Web resource for entitlement decisions. */
    private WebResource decisionResource = null;

    /** OpenSSO realm to request entitlement decisions for. */
    private String realm = null;

    /** Regular expression pattern for path to ignore. */
    private Pattern ignorePathPattern = null;

    /**
     * Called by the web container to indicate to the filter that it is being
     * placed into service.
     *
     * @param config passes information to filter during initialization.
     * @throws ServletException if an error occurs.
     */
    public void init(FilterConfig config) throws ServletException {
        this.config = config;
        decisionResource = client.resource(requiredInitParam(PARAM_DECISION_RESOURCE));
        realm = defaultInitParam(PARAM_REALM, "/"); // default to root realm if not specified
        ignorePathPattern = pattern(defaultInitParam(PARAM_IGNORE_PATH_PATTERN, null)); // no pattern
    }

    /**
     * Called by the web container each time a request/response pair is passed
     * through the chain due to a client request for a resource at the end of
     * the chain.
     *
     * @param request object to provide client request information to a servlet.
     * @param response object to assist in sending a response to the client.
     * @param chain gives a view into the invocation chain of a filtered request.
     * @throws IOException if an I/O error occurs.
     * @throws ServletException if an error occurs.
     */
    public void doFilter(ServletRequest request,
    ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest hsRequest = (HttpServletRequest)request;
        HttpServletResponse hsResponse = (HttpServletResponse)response;

        // do not filter if the request path matches pattern to ignore
        if (match(ignorePathPattern, hsRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        StringBuffer url = hsRequest.getRequestURL();
        String query = hsRequest.getQueryString();
        if (query != null) {
            url.append('?').append(query);
        }

        String action = hsRequest.getMethod();
        String subject = subjectFromRequest(hsRequest);
        String resource = url.toString();

        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("realm", realm);
        params.add("action", action);
        params.add("resource", resource);

        // optional
        if (subject != null) {
            params.add("subject", subject);
        }

        String decision = "deny"; // fail safe to deny access if exception occurs
        try {
            decision = decisionResource.queryParams(params).accept("text/plain").get(String.class);
        }

        // the fail safe of "deny" stands
        catch (UniformInterfaceException uie) {
        }

        if (decision == null || !decision.equals("allow")) {
            hsResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

// TODO: find/customize client to cache decisions based-on HTTP cache-control headers

        // access to resource allowed; let request pass on
        chain.doFilter(request, response);
    }

    /**
     * Called by the web container to indicate to the filter that it is being
     * taken out of service.
     */
    public void destroy() {
    }

    private static Pattern pattern(String p) {
        if (p == null) {
            return null;
        }
        return Pattern.compile(p);
    }

    private static boolean match(Pattern pattern, String value) {
        return (pattern != null && value != null && pattern.matcher(value).matches());
    }

    private String requiredInitParam(String name) throws ServletException {
 
        String v = config.getInitParameter(name);
 
        if (v == null || v.length() == 0) {
            throw new ServletException(name + " init parameter required");
        }
 
        return v;
    }        

    private String defaultInitParam(String name, String value) {
 
        String v = config.getInitParameter(name);
 
        if (v == null || v.length() == 0) {
            v = value;
        }
 
        return v;
    }

    private String subjectFromRequest(HttpServletRequest request) {

        if (request == null) {
            return null;
        }

        Principal principal = request.getUserPrincipal();

        if (principal == null) {
            return null;
        }

        return principal.getName();
    }
}
