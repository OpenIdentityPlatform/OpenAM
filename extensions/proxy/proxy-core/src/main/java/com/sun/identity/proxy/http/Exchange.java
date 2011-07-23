/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
e * (the License). You may not use this file except in
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
 * $Id: Exchange.java,v 1.4 2009/10/14 08:56:51 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.http;

import com.sun.identity.proxy.handler.Handler;

/**
 * An HTTP request-response exchange.
 * <p>
 * It is the responsibility of the caller of the {@link Handler} to create and
 * populate the request, and responsibility of the handler to create and populate
 * the response.
 * <p>
 * If an existing response object exists in the exchange and the handler
 * intends to replace it with another response object, it must first check to
 * see if the existing response object has an entity, and if it does, must
 * call its <tt>close</tt> method in order to signal that the processing of
 * the response from a remote server is complete.
 *
 * @author Paul C. Bryan
 */
public class Exchange
{
    /** The HTTP request. */
    public Request request = null;

    /** The HTTP response. */
    public Response response = null;
}

