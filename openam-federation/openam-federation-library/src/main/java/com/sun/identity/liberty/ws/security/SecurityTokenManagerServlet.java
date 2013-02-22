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
 * $Id: SecurityTokenManagerServlet.java,v 1.2 2008/06/25 05:47:21 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.liberty.ws.security;

import com.sun.identity.federation.common.FSUtils;
import org.forgerock.openam.utils.ClientUtils;

import com.sun.xml.rpc.server.http.JAXRPCServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

/**
 * This class provides remote interfaces for the
 * <code>SecurityTokenManager</code> class using JAX-RPC. Since JAX-RPC does not 
 * provide a mechanism to obtain <code>HttpServletRequest</code> and 
 * <code>HttpServletResponse </code>, it is currently extending Sun's 
 * implementation of <code>JAXRPCServlet</code>.
 * This classes uses the same security mechanism used by <code>SAMLSOAPReceiver
 * </code> for validating the caller.
 */
public class SecurityTokenManagerServlet extends JAXRPCServlet {
    
     private static String DEBUG_SUCCESS_MSG =
          "SecurityTokenManagerServlet: processing request from server: ";
     
    /**
     * Overrides JAXRPCServlet's doPost method to perform the
     * security check on the caller. The logic is implemented
     * in SAMLSOAPReceiver.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @throws ServletException if there is an error.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException {
        if (SecurityTokenManager.debug.messageEnabled()) {
            SecurityTokenManager.debug.message(DEBUG_SUCCESS_MSG +
            ClientUtils.getClientIPAddress(request));
        }

        FSUtils.checkHTTPRequestLength(request);

        // Call JAXRPC servlet's doPost
        super.doPost(request, response);
    }
}
