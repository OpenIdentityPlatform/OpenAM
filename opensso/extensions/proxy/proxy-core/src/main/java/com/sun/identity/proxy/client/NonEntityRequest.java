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
 * $Id: NonEntityRequest.java,v 1.4 2009/10/15 07:07:54 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.client;

import com.sun.identity.proxy.http.Request;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * A request that does not enclose an entity, suitable for submission to the
 * Apache HttpComponents Client.
 *
 * @author Paul C. Bryan
 */
class NonEntityRequest extends HttpRequestBase
{
    /** The request method. */
    private String method = null;

    /**
     * Creates a new non-entity enclosing request for the specified incoming
     * proxy request.
     *
     * @param request the incoming proxy request.
     */
    public NonEntityRequest(Request request) {
        this.method = request.method;
    }

    @Override
    public String getMethod() {
        return method;
    }
}

