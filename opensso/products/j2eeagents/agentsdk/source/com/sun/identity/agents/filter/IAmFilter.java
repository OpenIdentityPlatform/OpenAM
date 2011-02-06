/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: IAmFilter.java,v 1.2 2008/06/25 05:51:44 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.AgentException;

/**
 * The interface for <code>AmFilter</code>
 */
public interface IAmFilter {
    public abstract void initialize(AmFilterMode filterMode)
            throws AgentException;

    /**
     * Determines if access to the requested resource should be allowed or any
     * other corrective action needs to be taken in case the request is a 
     * special request such as a notification, or lacks the necessary 
     * credentials. The return value of <code>AmFilterResult</code> carries 
     * the necessary information regarding what action must be taken for this 
     * request including any ncessary redirects or error codes that must be 
     * sent to the client from where this request originated.
     *
     * @param request the incoming <code>HttpServletRequest</code>
     * @param response the incoming <code>HttpServletResponse</code>
     *
     * @return an <code>AmFilterResult</code> instance which indicates what
     * specific action must be taken in order to fulfill this request.
     */
    public abstract AmFilterResult isAccessAllowed(HttpServletRequest request,
            HttpServletResponse response);
    
    /**
     * This method constructs and returns an instance of
     * <code>AmFilterResult</code> which can be used to redirect the request to
     * its original destination.
     *
     * @param ctx the <code>AmFilterRequestContext</code> which carries the
     * information pertaining to the request that is currently being processed.
     *
     * @return an <code>AmFilterResult</code> that can be used to redirect the
     * request back to its destination thereby making a roundtrip before passing
     * the request to the downstream application.
     */
    public abstract AmFilterResult redirectToSelf(AmFilterRequestContext ctx);
}
