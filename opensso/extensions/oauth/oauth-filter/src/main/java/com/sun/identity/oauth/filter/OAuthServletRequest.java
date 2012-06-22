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
 * $Id: OAuthServletRequest.java,v 1.1 2009/05/26 22:17:46 pbryan Exp $
 */

package com.sun.identity.oauth.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.jersey.oauth.signature.OAuthRequest;

/**
 * @author Paul C. Bryan <pbryan@sun.com>
 */
class OAuthServletRequest implements OAuthRequest
{
    /** TODO: Description. */
    private HttpServletRequest request;

    /**
     * TODO: Description.
     *
     * @param request TODO.
     */
    public OAuthServletRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns the name of the HTTP method with which this request was made,
     * for example, GET, POST, or PUT.
     *
     * @return the name of the method with which this request was made.
     */
    public String getRequestMethod() {
        return request.getMethod();
    }

    /**
     * Returns the URL of the request, including protocol, server name,
     * optional port number, and server path.
     *
     * @return the request URL.
     */
    public String getRequestURL() {
        return request.getRequestURL().toString();
    }

    /**
     * Returns an {@link Iterator} of {@link String} objects containing the
     * names of the parameters contained in the request.
     *
     * @return the names of the parameters.
     */
    public Set<String> getParameterNames() {
        return new HashSet(Collections.list(request.getParameterNames()));
    }

    /**
     * Returns an {@link List} of {@link String} objects containing the
     * values of the specified request parameter, or null if the parameter does
     * not exist. For HTTP requests, parameters are contained in the query
     * string and/or posted form data.
     *
     * @param name the name of the parameter.
     * @return the values of the parameter.
     */
    public List<String> getParameterValues(String name) {
        return Arrays.asList(request.getParameterValues(name));
    }

    /**
     * Returns the value(s) of the specified request header. If the request did
     * not include a header of the specified name, this method returns null.
     *
     * @param name the header name.
     * @return the value(s) of the requested header, or null if none exist.
     */
    public List<String> getHeaderValues(String name) {
        return Collections.list(request.getHeaders(name));
    }

    /**
     * Adds a header with the given name and value.
     *
     * @param name the name of the header.
     * @param value the header value.
     * @throws IllegalStateException if this method cannot be implemented.
     */
    public void addHeaderValue(String name, String value) throws IllegalStateException {
        throw new IllegalStateException("OAuthServletRequest modification not supported");
    }
}
