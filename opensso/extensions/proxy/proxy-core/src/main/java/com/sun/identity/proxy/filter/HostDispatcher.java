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
 * $Id: HostDispatcher.java,v 1.3 2009/10/18 22:23:59 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.filter;

import com.sun.identity.proxy.handler.Handler;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * Dispatches to handlers based on the content of the <tt>Host</tt> request
 * header. This can be useful when more than one remote server uses the same
 * cookie, and that cookie needs to be managed by a filter rather than being
 * relayed to the remote client. In this case, two filter chains would be
 * configured to terminate to the same cookie filter and client handler.
 * <p>
 * Note: It's generally better to use the container's virtual host function
 * rather than using this class to dispatch to different handlers. For example,
 * if one handler were to misbehave, it could affect all handlers exposed
 * through a single proxy servlet. There is a better chance that the container
 * can encapsulate the problem of the misbehaving servlet, leaving all other
 * servlets operational.
 * <p>
 * This class maps regular expression patterns to handlers to dispatch to.
 * Regular expressions are evaluated against the <tt>Host</tt> header in the
 * incoming request. Per RFC 2616, this header contains the host name and
 * optionally port number. Prior to matching, this class converts the host
 * header value to lower case. If no host header exists in the request, then
 * an empty string value is used for evaluation.
 * <p>
 * In the <tt>handle</tt> method, regular expression patterns are evaluated in
 * the order they were added to this map. If no matching handler is found, a
 * {@link HandlerException} will be thrown. Therefore, it is advisable to add a
 * catch-all pattern to dispatch to a default handler.
 * <p>
 * Example:
 * <pre>
 * HostDispatcher dispatcher = new HostDispatcher();
 * ...
 * dispatcher.put(Pattern.compile("^(www\\.)?example1\\.com(:80)?$"), chain1);
 * dispatcher.put(Pattern.compile("^example2\\.com(:80)?$"), chain2);
 * dispatcher.put(Pattern.compile(""), errorHandler);
 * ...
 * dispatcher.handle(exchange);
 * </pre>
 * In the above example, the first expression allows an optional www. prefix;
 * the first and second expressions both support the inclusion of port number.
 * If the host doesn't match the first two expressions, the exchange is
 * dispatched to the error chain.
 *
 * @author Paul C. Bryan
 */
public class HostDispatcher extends LinkedHashMap<Pattern, Handler> implements Handler
{
    /**
     * Creates a new host dispatcher.
     */
    public HostDispatcher() {
    }

    /**
     * Handles an exchange by dispatching to the handler mapped to the
     * regular expression that first matches the request's host header.
     *
     */
    @Override
    public void handle(Exchange exchange) throws HandlerException, IOException {
        String host = exchange.request.headers.first("Host");
        if (host == null) {
            host = "";
        }
        host = host.toLowerCase();
        for (Pattern pattern : keySet()) {
            if (pattern.matcher(host).find()) {
                get(pattern).handle(exchange);
                return;
            }
        }
        throw new HandlerException("no matching handler found for host");
    }
}
