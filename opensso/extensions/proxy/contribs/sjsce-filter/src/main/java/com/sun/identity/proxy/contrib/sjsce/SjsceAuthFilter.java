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
 * $Id: SjsceAuthFilter.java,v 1.2 2009/10/22 01:18:21 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.contrib.sjsce;

import com.sun.identity.proxy.auth.PasswordCredentials;
import com.sun.identity.proxy.auth.PasswordCredentialSource;
import com.sun.identity.proxy.handler.Filter;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Form;
import com.sun.identity.proxy.http.Request;
import com.sun.identity.proxy.util.URIUtil;
import java.io.IOException;
import java.net.URI;

/**
 * A filter that performs form-based authentication with Sun Java System
 * Calendar Express.
 * <p>
 * There is no need to add a filter to manage cookies with this filter,
 * because cookies are simply passed back to the remote client in the redirect
 * (302) response for successful authentication.
 *
 * @author Paul C. Bryan
 */
public class SjsceAuthFilter extends Filter
{
    /** The source from which to acquire username/password credentials. */
    private PasswordCredentialSource source;

    /**
     * Creates a new Sun Java System Calendar Express authentication filter.
     *
     * @param source the source from which to acquire username/password credentials.
     */
    public SjsceAuthFilter(PasswordCredentialSource source) {
        this.source = source;
    }

    @Override
    public void handle(Exchange exchange) throws HandlerException, IOException
    {
        // if not a request for the login page, simply pass-through
        if (!exchange.request.uri.normalize().getPath().equals("/")) {
            next.handle(exchange);
            return;
        }

        PasswordCredentials credentials = source.credentials(exchange.request);

        // no credentials is equivalent to invalid credentials
        if (credentials == null) {
            source.invalid(exchange);
            return;
        }

        // new request to be submitted
        Request login = new Request();
        login.uri = URIUtil.newPath(exchange.request.uri, "/login.wcap");
        login.principal = exchange.request.principal;
        login.session = exchange.request.session;
        login.attributes = exchange.request.attributes;
        login.headers.put("Host", exchange.request.headers.first("Host"));

        // login form fields
        Form form = new Form();
        form.add("user", credentials.username);
        form.add("password", credentials.password);
        form.add("fmt-out", "text/html");
        form.toFormEntity(login); // this sets method to POST

        // overwrite the original incoming request; it's no longer needed
        exchange.request = login;

        // pass along login request downstream to remote server
        next.handle(exchange);

        // redirect to non-login-page is success: return response as-is
        if (exchange.response.status == 302
        && !URI.create(exchange.response.headers.first("Location")).getPath().equals("/")) {
            return;
        }

        // presumably an authentication failure is due to invalid credentials
        source.invalid(exchange);
    }
}
