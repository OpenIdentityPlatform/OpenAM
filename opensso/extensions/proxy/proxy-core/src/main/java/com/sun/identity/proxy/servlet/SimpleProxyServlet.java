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
 * $Id: SimpleProxyServlet.java,v 1.4 2009/10/21 00:01:45 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.servlet;

import com.sun.identity.proxy.handler.Chain;
import com.sun.identity.proxy.handler.Filter;
import com.sun.identity.proxy.client.ClientHandler;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.ServletException;

/**
 * Basic implementation of a servlet that establishes a filter
 * chain and initializes the base URI to a remote server.
 * <p>
 * This class is intended to be subclassed by a servlet that calls this
 * class' <tt>init</tt> method from its own <tt>init</tt> method, and
 * add filters to the filter chain to customize its behavior.
 * <p>
 * All of the logic to handle incoming requests, translate into proxy message
 * exchanges and dispatch to the filter chain (handler) is in this class'
 * superclass: {@link HandlerServlet}.
 *
 * @author Paul C. Bryan
 */
public class SimpleProxyServlet extends HandlerServlet
{
    /**
     * Creates a new simple proxy servlet, initializing the filter chain
     * with a root client handler.
     */
    public SimpleProxyServlet() {
        handler = new Chain(new ClientHandler());
    }

    /**
     * Initializes the simple proxy servlet with a specified remote server to
     * relay requests to.
     *
     * @param scheme the scheme name of the remote server.
     * @param host the host address of the remote server.
     * @param port the port number of the remote server, or -1 to use the scheme default port.
     * @throws ServletException if an exception occurs that prevents initialization.
     */
    public void init(String scheme, String host, int port) throws ServletException {
        try {
            base = new URI(scheme, null, host, port, null, null, null);
        }
        catch (URISyntaxException use) {
            throw new ServletException(use);
        }
    }
    
    /**
     * Adds a filter before the existing chain of filter(s) and/or root
     * handler in the chain.
     *
     * @param filter the filter to be added in front of the existing chain.
     * @throws IllegalArgumentException if the filter to add already has a next filter.
     */
    public void addFilter(Filter filter) {
        ((Chain)handler).addFilter(filter);
    }
}

