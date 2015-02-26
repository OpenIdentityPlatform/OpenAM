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
 * $Id: FSAssertionManagerServlet.java,v 1.2 2008/06/25 05:46:52 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.federation.services;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.identity.federation.common.FSUtils;
import com.sun.xml.rpc.server.http.JAXRPCServlet;
import org.forgerock.openam.utils.ClientUtils;

/**
 * The class provides remote interfaces for the <code>AssertionManager</code>
 * class using JAX-RPC. Since JAX-RPC does not provide a mechanism to
 * obtain a <code>HttpServletRequest</code> and <code>HttpServletResponse
 * </code>, it is currently extending Sun's implementation of <code>
 * JAXRPCServlet</code>.
 * This classes uses the same security mechanism used by <code>SAMLSOAPReceiver
 * </code> for validating the caller.
 */
public class FSAssertionManagerServlet extends JAXRPCServlet {
    
    /**
     * Overrides <code>JAXRPCServlet</code> method to do content length 
     * checking.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException if error occurs.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(DEBUG_SUCCESS_MSG + ClientUtils.getClientIPAddress(request));
        }

        FSUtils.checkHTTPRequestLength(request);

        // Call JAXRPC servlet's doPost
        super.doPost(request, response);
    }
    
    private static String DEBUG_SUCCESS_MSG =
        "FSAssertionManagerServlet: processing request from server: ";
}
