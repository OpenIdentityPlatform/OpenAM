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
 * $Id: HeaderFilter.java,v 1.1 2009/10/18 18:41:27 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.filter;

import com.sun.identity.proxy.handler.Filter;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Headers;
import com.sun.identity.proxy.util.CIStringSet;
import java.io.IOException;

/**
 * Filters exchanges by removing headers from and adding headers to
 * the request and response.
 * <p>
 * This implementation first removes headers, then adds headers.
 * Therefore, headers can be replaced by specifying them to be both removed
 * and added.
 *
 * @author Paul C. Bryan
 */
public class HeaderFilter extends Filter
{
    /** Names of headers to remove from the request. */
    public final CIStringSet requestRemoved = new CIStringSet();

    /** Headers to add to the request. */
    public final Headers requestAdded = new Headers();

    /** Names of headers to remove from the response. */
    public final CIStringSet responseRemoved = new CIStringSet();

    /** Headers to add to the response. */
    public final Headers responseAdded = new Headers();    

    /**
     * Creates a new header filter.
     */
    public HeaderFilter() {
    }

    /**
     * Filters the exchange by removing headers from and adding headers to
     * request and response.
     */
    @Override
    public void handle(Exchange exchange) throws HandlerException, IOException {
        exchange.request.headers.remove(requestRemoved);
        exchange.request.headers.add(requestAdded);
        next.handle(exchange);
        exchange.response.headers.remove(responseRemoved);
        exchange.response.headers.add(responseAdded);
    }
}

