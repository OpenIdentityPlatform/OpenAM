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
 * $Id: FSPreLoginHandler.java,v 1.4 2008/08/29 04:57:16 exu Exp $
 *
 */

package com.sun.identity.federation.login;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/** 
 * This servlet is invoked to determine where
 * the incoming request should be redirected to ,
 * based on the validity of the Federation Manager Session Cookie 
 * and the existance of the Federation Cookie, whether local authentication
 * needs to be done. 
 */
 
public class FSPreLoginHandler extends HttpServlet {
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @exception ServletException if the request could not be
     *         handled.
     * @exception IOException if an input or output error occurs.
     */
    protected void processRequest(HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException,IOException 
    {
        if (FSUtils.needSetLBCookieAndRedirect(request, response, false)) {
            return;
        }
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        if(metaAlias != null && metaAlias.length() > 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSPreLoginHandler::processRequest:"
                    + "Calling prelogin with metaAlias=" + metaAlias);
            }
            FSPreLogin preLogin = new FSPreLogin();
            preLogin.doPreLogin(request,response);
        } else {
            FSUtils.debug.error("FSPreLoginHandler::processRequest:No MetaAlias"
                + "in request. Cannot procced");
        }
    }
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @exception ServletException if the request could not be handled.
     * @exception IOException if an input or output error occurs.
     */
    protected void doGet(HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, java.io.IOException 
    {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @exception ServletException if the request could not be handled.
     * @exception IOException if an input or output error occurs.
     */
    protected void doPost(HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, java.io.IOException 
    {
        processRequest(request, response);
    }
}
