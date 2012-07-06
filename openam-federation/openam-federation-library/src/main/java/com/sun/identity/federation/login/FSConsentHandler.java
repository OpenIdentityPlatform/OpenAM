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
 * $Id: FSConsentHandler.java,v 1.2 2008/06/25 05:46:42 qcheng Exp $
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
 * This servlet redirects the request to the introduction
 * URL or the provider home page URL based on the
 * action in the request.
 */
public class FSConsentHandler extends HttpServlet {
    
    /** 
     * Processes requests for both HTTP <code>GET</code> and 
     * <code>POST</code> methods. Redirects to provider URL
     * if the action is cancel or to the introduction URL.
     * 
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @exception ServletException if the request could not be 
     *         handled.
     * @exception IOException if an input or output error occurs.
     */
    protected void processRequest(HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, java.io.IOException 
    {
        String action = request.getParameter(IFSConstants.USERACTION);
        String lrurl = null;
        if (action !=null && 
            action.trim().equalsIgnoreCase(IFSConstants.CANCEL)) 
        {
            FSUtils.debug.message(
                "FSConsentHandler:: user pressed cancel proceding to lrurl");
            lrurl = request.getParameter(IFSConstants.LRURL);
        } else {
            try {
                FSPostLogin postLogin = new FSPostLogin();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSConsentHandler:: selected provider is "
                        + request.getParameter(IFSConstants.COTKEY));
                }
                lrurl = postLogin.doConsentToIntro(request);
            } catch (FSPostLoginException fsPostExp) {
                FSUtils.debug.error("FSConsentHandler::FSPostLogin Exception",
                    fsPostExp);
            }
        }
        if (lrurl != null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSConsentHandler:: Redirecting to  "
                    + lrurl );
            }
            FSUtils.forwardRequest(request, response, lrurl);
        } else {
            FSUtils.debug.error(
                "FSConsentHandler:: LRURL is null in request ");
        }
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @exception ServletException if the request could not be 
     *         handled.
     * @exception IOException if an input or output error occurs.
     */
    protected void doGet(HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @exception ServletException if the request could not be
     *         handled.
     * @exception IOException if an input or output error occurs.
     */
    protected void doPost(HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        processRequest(request, response);
    }
}
