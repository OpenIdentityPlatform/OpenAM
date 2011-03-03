/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: HttpBasicAuthFilter.java,v 1.9 2009/10/18 18:41:26 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.auth;

import com.sun.identity.proxy.handler.Filter;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.CachedRequest;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Request;
import com.sun.identity.proxy.http.Response;
import com.sun.identity.proxy.io.TemporaryStorage;
import com.sun.identity.proxy.util.Base64;
import com.sun.identity.proxy.util.CIStringSet;
import com.sun.identity.proxy.util.StringUtil;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * A filter that performs HTTP basic authentication per RFC 2617.
 * <p>
 * Once an HTTP authentication challenge (status code 401) is issued from
 * the remote server, all subsequent requests to that remote server that
 * pass through the filter will include the user credentials.
 *
 * @author Paul C. Bryan
 */
public class HttpBasicAuthFilter extends Filter
{
    /** Headers that are suppressed from incoming request. */
    private static final CIStringSet SUPPRESS_REQUEST_HEADERS =
     new CIStringSet(Arrays.asList("Authorization"));

    /** Headers that are suppressed for outgoing response. */
    private static final CIStringSet SUPPRESS_RESPONSE_HEADERS =
     new CIStringSet(Arrays.asList("WWW-Authenticate"));

    /** The source from which to acquire username/password credentials. */
    private PasswordCredentialSource source;

    /** Allocates temporary records for caching incoming request entities. */
    private TemporaryStorage storage;

    /**
     * Creates a new HTTP basic authentication filter.
     *
     * @param source the source from which to acquire username/password credentials.
     * @param storage allocates temporary records for caching incoming request entities.
     */
    public HttpBasicAuthFilter(PasswordCredentialSource source, TemporaryStorage storage) {
        this.source = source;
        this.storage = storage;
    }

    /**
     * Resolves a session attribute name for the remote server specified in the
     * specified request.
     *
     * @param name the name of the attribute to resolve.
     * @return the session attribute name, fully qualified the request remote server.
     */
    private String attributeName(Request request, String name) {
        return StringUtil.join(":", this.getClass().getName(), request.uri.getScheme(),
         request.uri.getHost(), Integer.toString(request.uri.getPort()), name);
    }

    /**
     * Handles the message exchange by authenticating via HTTP basic scheme
     * once challenged for authentication. Credentials are cached in the
     * session to allow subsequent requests to automatically include
     * authentication credentials.
     */
    @Override
    public void handle(Exchange exchange) throws HandlerException, IOException
    {
        exchange.request.headers.remove(SUPPRESS_REQUEST_HEADERS);
 
        // cache the incoming request for replay
        CachedRequest cached = new CachedRequest(exchange.request, storage);

        // loop to retry for intitially retrieved (or refreshed) credentials
        for (int n = 0; n < 2; n++) {

            exchange.request = cached.rewind();

            // because credentials are sent in every request, this class caches them in the session
            String userpass = (String)exchange.request.session.get(attributeName(exchange.request, "userpass"));

            if (userpass != null) {
                exchange.request.headers.add("Authorization", "Basic " + userpass);
            }

            next.handle(exchange);

            // successful exchange from this filter's standpoint
            if (exchange.response.status != 401) {
                exchange.response.headers.remove(SUPPRESS_RESPONSE_HEADERS);
                return;
            }

            // credentials might be stale, so fetch them
            PasswordCredentials credentials = source.credentials(exchange.request);

            // no credentials is equivalent to invalid credentials
            if (credentials == null) {
                break;
            }

            // ensure conformance with specification
            if (credentials.username.indexOf(':') > 0) {
                throw new HandlerException("username must not contain a colon character");
            }

            // set in session for fetch in next iteration of this loop
            exchange.request.session.put(attributeName(exchange.request, "userpass"),
             Base64.encode((credentials.username + ":" + credentials.password).getBytes()));
        }

        // close the incoming response because it's about to be dereferenced (important!)
        if (exchange.response.entity != null) {
            exchange.response.entity.close();
        }

        // credentials were missing or invalid; let credential source handle the error
        exchange.response = new Response();
        source.invalid(exchange);
    }
}

