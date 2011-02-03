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
 * $Id: Handler.java,v 1.3 2009/10/14 08:56:50 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.handler;

import com.sun.identity.proxy.http.Exchange;
import java.io.IOException;

/**
 * Defines the interface that all message exchange handlers implement.
 *
 * A handler is a class that processes the incoming request, and provides
 * an outgoing response.
 *
 * @author Paul C. Bryan
 */
public interface Handler
{
    /**
     * Called to request the handler respond to the request.
     * <p>
     * A handler that doesn't hand-off an exchange to another handler
     * downstream is responsible for creating the response object.
     * <p>
     * If an existing response object exists in the exchange and the handler
     * intends to replace it with another response object, it must first check
     * to see if the existing response object has an entity, and if it does,
     * must call its <tt>close</tt> method in order to signal that the
     * processing of the response from a remote server is complete.
     *
     * @param exchange the message exchange to handle.
     * @throws HandlerException if an exception occurs that prevents handling the exchange.
     * @throws IOException if an I/O exception occurs.
     */
    public void handle(Exchange exchange) throws HandlerException, IOException;
}

