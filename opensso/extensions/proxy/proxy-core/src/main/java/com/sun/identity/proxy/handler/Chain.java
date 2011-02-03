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
 * $Id: Chain.java,v 1.4 2009/10/17 04:47:59 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.handler;

import com.sun.identity.proxy.http.Exchange;
import java.io.IOException;

/**
 * Maintains a chain of {@link Filter} objects. A chain is a specialized
 * type of handler, which knows how to add filters, and then invoke the first
 * head filter in the chain when it is invoked.
 * <p>
 * When a filter is added to the chain, it is added in front of any handler
 * and/or filter(s) already in the chain. The last filter added to the chain is
 * always the first filter invoked when the chain is invoked.
 *
 * @author Paul C. Bryan
 * @credit Paul Sandoz (influenced by the com.sun.jersey.client.filter.Filterable class)
 */
public class Chain implements Handler
{
    /** The handler at the end of the chain. */
    private final Handler root;

    /** The first handler in the chain to be invoked. */
    private Handler head;

    /**
     * Creates a new filter chain.
     *
     * @param root the handler at the end of the chain.
     */
    public Chain(Handler root) {
        this.head = this.root = root;
    }

    /**
     * Adds a filter before the existing chain of filter(s) and/or root
     * handler in the chain.
     *
     * @param filter the filter to be added in front of the existing chain.
     * @throws IllegalArgumentException if the filter to add already has a next filter.
     */
    public void addFilter(Filter filter) {
        filter.setNext(head);
        head = filter;
    }

    /**
     * Invokes the <tt>handle</tt> method of the head filter in the chain.
     */
    @Override
    public void handle(Exchange exchange) throws IOException, HandlerException {
        head.handle(exchange);
    }
}

