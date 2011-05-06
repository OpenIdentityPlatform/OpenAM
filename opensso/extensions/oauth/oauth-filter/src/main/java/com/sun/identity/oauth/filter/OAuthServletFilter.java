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
 * $Id: OAuthServletFilter.java,v 1.4 2009/05/28 20:28:24 pbryan Exp $
 */

package com.sun.identity.oauth.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.signature.OAuthSignature;
import com.sun.jersey.oauth.signature.OAuthSignatureException;

public class OAuthServletFilter implements Filter {

    // TODO: time to seriously consider switching to a configuration file?
    private static final String PARAM_REALM = "realm";
    private static final String PARAM_SIGNATURE_METHOD = "signatureMethod";
    private static final String PARAM_IGNORE_PATH_PATTERN = "ignorePathPattern";
    private static final String PARAM_CONSUMER_KEY_PATTERN = "consumerKeyPattern";
    private static final String PARAM_ACCESS_TOKEN_PATTERN = "accessTokenPattern";
    private static final String PARAM_MAX_AGE = "maxAge";
    private static final String PARAM_GC_PERIOD = "gcPeriod";
    private static final String PARAM_NONCE_INDEX = "nonceIndex";

    /** Jersey client to make REST calls to token services. */
    private Client client = Client.create();

    /** Servlet filter configuration. */
    FilterConfig config = null;

    /** Manages and validates incoming nonces. */
    private NonceManager nonces;

    /** The OAuth protection realm to advertise in www-authenticate header. */
    private String realm = null;

    /** Maximum age (in milliseconds) of timestamp to accept in incoming messages. */
    private int maxAge = -1;

    /** Average requests to process between nonce garbage collection passes. */
    private int gcPeriod = -1;

    /** Value to return in www-authenticate header when 401 response returned. */
    private String wwwAuthenticateHeader = null;

    /** OAuth protocol versions that are supported. */
    private HashSet<String> versions = new HashSet<String>();

    /** Regular expression pattern for acceptable consumer key. */
    private Pattern consumerKeyPattern = null;

    /** Regular expression pattern for acceptable access token. */
    private Pattern accessTokenPattern = null;

    /** Regular expression pattern for path to ignore. */
    private Pattern ignorePathPattern = null;

    /** Attribute used to index nonces in nonce manager. */
    private String nonceIndex = null;

    /**
     * Called by the web container to indicate to the filter that it is being
     * placed into service.
     *
     * @param config passes information to filter during initialization.
     * @throws ServletException if an error occurs.
     */
    public void init(FilterConfig config) throws ServletException {

        this.config = config;

        // establish supported OAuth protocol versions
        versions.add(null);
        versions.add("1.0");

        // required initialization parameters
        realm = requiredInitParam(PARAM_REALM);
        consumerKeyPattern = pattern(requiredInitParam(PARAM_CONSUMER_KEY_PATTERN));;
        accessTokenPattern = pattern(requiredInitParam(PARAM_ACCESS_TOKEN_PATTERN));

        // optional initialization parameters (defaulted)
        maxAge = intValue(defaultInitParam(PARAM_MAX_AGE, "300000")); // 5 minutes
        gcPeriod = intValue(defaultInitParam(PARAM_GC_PERIOD, "100")); // every 100 on average
        nonceIndex = defaultInitParam(PARAM_NONCE_INDEX, "consumerKey"); // consumer index for nonces
        ignorePathPattern = pattern(defaultInitParam(PARAM_IGNORE_PATH_PATTERN, null)); // no pattern

        nonces = new NonceManager(maxAge, gcPeriod);

        // www-authenticate header for the life of the object
        wwwAuthenticateHeader = "OAuth realm=\"" + realm + "\"";
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
    ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest hsRequest = (HttpServletRequest)request;
        HttpServletResponse hsResponse = (HttpServletResponse)response;    

        try {
            filter(hsRequest, hsResponse, chain);
        }
    
        catch (BadRequestException bre) {
            hsResponse.sendError(HttpServletResponse.SC_BAD_REQUEST); // 400
        }

        catch (UnauthorizedException ue) {
            hsResponse.setHeader("WWW-Authenticate", wwwAuthenticateHeader);
            hsResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        }
    }

    /**
     * Called by the web container to indicate to the filter that it is being
     * taken out of service.
     */
    public void destroy() {
    }

    private void filter(HttpServletRequest request, HttpServletResponse response,
    FilterChain chain) throws IOException, ServletException {

        // do not filter if the request path matches pattern to ignore
        if (match(ignorePathPattern, request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        OAuthServletRequest osr = new OAuthServletRequest(request);

        OAuthParameters params = new OAuthParameters().readRequest(osr);

        // apparently not signed with any OAuth parameters; unauthorized
        if (params.size() == 0) {
            throw new UnauthorizedException();
        }

        // get required OAuth parameters
        String consumerKey = requiredOAuthParam(params.getConsumerKey());
        String token = requiredOAuthParam(params.getToken());
        String timestamp = requiredOAuthParam(params.getTimestamp());
        String nonce = requiredOAuthParam(params.getNonce());
        String signatureMethod = requiredOAuthParam(params.getSignatureMethod());

        // enforce other supported and required OAuth parameters
        requiredOAuthParam(params.getSignature());
        supportedOAuthParam(params.getVersion(), versions);

        // consumer key or token do not match the expected patterns; signature is invalid
        if (!match(consumerKeyPattern, consumerKey) || !match(accessTokenPattern, token)) {
            throw new UnauthorizedException();
        }

        MultivaluedMap<String, String> query;
        MultivaluedMap<String, String> mvmResponse;

        // retrieve secret for consumer key
        WebResource consumerResource = client.resource(consumerKey);
        query = new MultivaluedMapImpl();
        query.add("signature_method", signatureMethod);
        mvmResponse = get(consumerResource, query);
        String consumerSecret = requiredForAuthorization(mvmResponse.getFirst("secret"));

        // retrieve subject and shared secret for access token
        WebResource tokenResource = client.resource(token);
        query = new MultivaluedMapImpl();
        query.add("subject", "1");
        query.add("shared_secret", "1");
        mvmResponse = get(tokenResource, query);
        String tokenSecret = mvmResponse.getFirst("shared_secret");
        String subject = requiredForAuthorization(mvmResponse.getFirst("subject"));

        // unsupported signature method results in 400 bad request
        if (tokenSecret == null) {
            throw new BadRequestException();
        }

        OAuthSecrets secrets = new OAuthSecrets().consumerSecret(consumerSecret).tokenSecret(tokenSecret);

        if (!verifySignature(osr, params, secrets)) {
            throw new UnauthorizedException();
        }

        // TODO: make specifying and testing this easier
        String key = (nonceIndex.equals("consumerKey") ? consumerKey : token);

        if (!nonces.verify(key, timestamp, nonce)) {
            throw new UnauthorizedException();
        }

        // chain request wrapped with overridden getUserPrincipal to next filter
        chain.doFilter(new PrincipalRequestWrapper(request, new NamedPrincipal(subject)), response);
    }

    @SuppressWarnings("unchecked")
    private static MultivaluedMap<String, String> get(WebResource resource, MultivaluedMap params)
    throws UnauthorizedException {
        String response;
        try {
            response = resource.queryParams(params).get(String.class);
        }
        catch (UniformInterfaceException uie) {
            throw new UnauthorizedException();
        }
        return UriComponent.decodeQuery(response, true);
    }

    private static String requiredForAuthorization(String value) throws UnauthorizedException {
        if (value == null || value.length() == 0) {
            throw new UnauthorizedException();
        }
        return value;
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

    private static int intValue(String value) {
        try {
            return Integer.valueOf(value);
        }
        catch (NumberFormatException nfe) {
           return -1;
        }
    }

    private static String requiredOAuthParam(String value) throws BadRequestException {
        if (value == null) {
            throw new BadRequestException();
        }
        return value;
    }

    private static String supportedOAuthParam(String value, HashSet<String> set) throws BadRequestException {
        if (!set.contains(value)) {
            throw new BadRequestException();
        }
        return value;
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

    private static boolean verifySignature(OAuthServletRequest osr,
    OAuthParameters params, OAuthSecrets secrets) throws ServletException {
        try {
            return OAuthSignature.verify(osr, params, secrets);
        }
        catch (OAuthSignatureException ose) {
            throw new ServletException(ose);
        }
    }
}
