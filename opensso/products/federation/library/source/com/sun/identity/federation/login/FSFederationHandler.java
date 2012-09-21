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
 * $Id: FSFederationHandler.java,v 1.3 2008/08/29 04:57:15 exu Exp $
 *
 */

package com.sun.identity.federation.login;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.services.util.FSServiceUtils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet processes requests to initiate Federation
 * and redirects to the provider home pager URL on
 * successful federation.
 */
public class FSFederationHandler extends HttpServlet {
    
    /**
     * Processes requests for both HTTP <code>GET</code> and
     * <code>POST</code> methods. Redirects to provider URL
     * if the action is cancel else to the provider URL or
     * to the error page on error.
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
        if (action != null &&
            action.trim().equalsIgnoreCase(IFSConstants.CANCEL)) 
        {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSFederationHandler:: user pressed "
                    + "cancel proceding to lrurl");
            }
            String lrURL = request.getParameter(IFSConstants.LRURL);
            if (lrURL != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSFederationHandler::Redirecting to "
                        + lrURL);
                }
                FSUtils.forwardRequest(request, response, lrURL);
            } else {
                FSUtils.debug.error(
                    "FSFederationHandler::LRURL is null in reqest ");
            }
        } else {
            if (FSUtils.needSetLBCookieAndRedirect(request, response, false)) {
                return;
            }
            try {
                String metaAlias =
                    request.getParameter(IFSConstants.META_ALIAS);
                String provider =
                    request.getParameter(IFSConstants.SELECTEDPROVIDER);
                if (metaAlias == null || provider == null) {
                    FSUtils.debug.error("FSFederationHandler:: No MetaAlias "
                        + "in request. Redirecting to error page");
                    String errorPage = FSServiceUtils.getErrorPageURL(
                        request, null, metaAlias);
                    FSUtils.forwardRequest(request, response, errorPage);
                } else {
                    FSPostLogin postLogin = new FSPostLogin();
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSFederationHandler::selected provider is "
                            + provider);
                    }
                    String lrURL = postLogin.doFederation(request, response);
                    if (lrURL != null) {
                        if(FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSFederationHandler:: Redirecting to" + lrURL 
                                + " after postLogin" );
                        }
                        response.sendRedirect(lrURL);
                    } else {
                         FSUtils.debug.error("FSFederationHandler:: "
                             + " LRURL is null from postlogin ");
                    }
                }
            } catch (FSPostLoginException fsPostExp) {
                FSUtils.debug.error(
                    "FSFederationHandler::FSPostLogin Exception:", fsPostExp);
            }
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
        throws ServletException, java.io.IOException 
    {
        processRequest(request, response);
    }
}
