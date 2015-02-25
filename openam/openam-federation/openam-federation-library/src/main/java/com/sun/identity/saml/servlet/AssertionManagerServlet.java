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
 * $Id: AssertionManagerServlet.java,v 1.3 2009/06/12 22:21:39 mallas Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.saml.servlet;

import org.forgerock.openam.utils.ClientUtils;

import com.sun.identity.saml.common.SAMLUtils;

import com.sun.xml.rpc.server.http.JAXRPCServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

/**
 * The class provides remote interfaces for the <code>AssertionManager</code>
 * class using JAX-RPC. Since JAX-RPC does not provide a mechanism to
 * obtain <code>HttpServletRequest</code> and <code>HttpServletResponse
 * </code>, it is currently extending Sun's implementation of <code>
 * JAXRPCServlet</code>.
 * This class uses the same security mechanism used by 
 * <code>SAMLSOAPReceiver</code> for validating the caller.
 */
public class AssertionManagerServlet extends JAXRPCServlet {

    private static String DEBUG_SUCCESS_MSG =
        "AssertionManagerServlet: processing request from a trusted server: ";

    private static String DEBUG_FAILED_MSG =
        "AssertionManagerServlet: request from untrusted site: ";

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

        String clientIP = ClientUtils.getClientIPAddress(request);

        if (SAMLSOAPReceiver.checkCaller(request, response) != null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(DEBUG_SUCCESS_MSG +
                    clientIP);
            }
            // Call JAXRPC servlet's doPost
            super.doPost(request, response);
        } else {
            // its not trusted site
            String errMsg = DEBUG_FAILED_MSG + clientIP;
            SAMLUtils.debug.error(errMsg);
            SAMLUtils.sendError(request, response, 
                    HttpServletResponse.SC_FORBIDDEN,
                    "untrustedSite",
                    SAMLUtils.bundle.getString("untrustedSite")
                    + clientIP);
            return;
        }
    }
}
