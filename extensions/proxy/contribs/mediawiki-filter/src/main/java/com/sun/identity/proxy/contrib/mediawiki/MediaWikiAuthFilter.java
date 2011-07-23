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
 * $Id: MediaWikiAuthFilter.java,v 1.4 2009/10/22 01:18:20 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.contrib.mediawiki;

import com.sun.identity.proxy.auth.PasswordCredentials;
import com.sun.identity.proxy.auth.PasswordCredentialSource;
import com.sun.identity.proxy.handler.Filter;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Form;
import com.sun.identity.proxy.http.Request;
import java.io.IOException;

/**
 * A filter that performs form-based authentication for MediaWiki.
 * <p>
 * This filter intercepts requests for the login page by performing
 * authentication on behalf of the user and returing the response
 * if successful.
 * <p>
 * Different WikiMedia deployments can have different field names to
 * use to authenticate. These can be customized by setting the
 * <tt>username</tt> and <tt>password</tt> instance variables.
 * <p>
 * There is no need to add a filter to manage cookies with this filter,
 * because session cookies are simply passed back to the remote client in
 * the redirect (302) response for successful authentication.
 *
 * @author Marjo van Diem
 * @author Paul C. Bryan
 */
public class MediaWikiAuthFilter extends Filter
{
    /** The source from which to acquire username/password credentials. */
    private PasswordCredentialSource source;

    /**
     * The MediaWiki article title used for the authentication page. This
     * can vary from one installation to the next.
     * Default: Special:UserLogin (English).
     */
    public String authTitle = "Special:UserLogin";

    /**
     * Creates a new MediaWiki authentication filter.
     *
     * @param source the source from which to acquire username/password credentials.
     */
    public MediaWikiAuthFilter(PasswordCredentialSource source) {
        this.source = source;
    }

    /**
     * Handles the message exchange. Checks if authentication is required. If
     * not, passes the request unmodified downstream. If required, submits a
     * form with the credentials to authenticate the user. Returns the redirect
     * response for a successful authentication upstream.
     */
    @Override
    public void handle(Exchange exchange) throws HandlerException, IOException
    {
        // parse pertinent incoming query parameters (if any)
        Form query = Form.getQueryParams(exchange.request);
        
        // article title being requested from mediawiki
        String title = query.first("title");
        
        // article to go to after successful authentication
        String returnto = query.first("returnto");
        
        // if this query parameter is present, mediawiki is checking for cookie support
        String wpCookieCheck = query.first("wpCookieCheck");

        // if not a request for the login page, simply pass-through
        if (title == null || !(title.equalsIgnoreCase(authTitle) && wpCookieCheck == null)) {
            next.handle(exchange);
            return;
        }

        PasswordCredentials credentials = source.credentials(exchange.request);

        // no credentials is equivalent to invalid credentials
        if (credentials == null) {
            source.invalid(exchange);
            return;
        }

        // initialize new request to be submitted
        Request login = new Request();
        login.uri = exchange.request.uri; // query params will be overwritten below
        login.principal = exchange.request.principal;
        login.session = exchange.request.session;
        login.attributes.putAll(exchange.request.attributes);
        login.headers.put("Host", exchange.request.headers.first("Host"));
        login.headers.put("User-Agent", exchange.request.headers.first("User-Agent"));

        // login query parameters
        query = new Form();
        query.add("title", authTitle);
        query.add("action", "submitlogin");
        query.add("type", "login");
        query.add("returnto", returnto);
        query.toQueryParams(login);

        // login form fields
        Form form = new Form();
        form.add("wpName", credentials.username);
        form.add("wpPassword", credentials.password);
        form.toFormEntity(login); // this sets method to POST

        // overwrite the original incoming request; it'll no longer be used
        exchange.request = login;

        // pass along login request downstream to remote server
        next.handle(exchange);

        // successful authentication, so simply pass redirect response upstream
        if (exchange.response.status == 302) {
            return;
        }

        // presumably an authentication failure is due to invalid credentials
        source.invalid(exchange);
    }
}

