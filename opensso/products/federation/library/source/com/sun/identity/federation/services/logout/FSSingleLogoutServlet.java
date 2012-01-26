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
 * $Id: FSSingleLogoutServlet.java,v 1.5 2008/12/19 06:50:47 exu Exp $
 *
 */


package com.sun.identity.federation.services.logout;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.services.util.FSServiceUtils;
import java.util.logging.Level;

/**
 * Initiates <code>ID-FF</code> Single Logout.
 */
public class FSSingleLogoutServlet extends HttpServlet {
    
    /**
     * Initiates the servlet.
     * @param config the <code>ServletConfig</code> object that contains
     *  configutation information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *              the servlet's normal operation.
     */
    public void init(ServletConfig config)
        throws ServletException 
    {
        super.init(config);
        FSUtils.debug.message("FSSingleLogoutServlet Initializing...");
    }
    
    /**
     * Handles the HTTP GET request.
     *
     * @param request an <code>HttpServletRequest</code> object that contains
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(HttpServletRequest  request,
                    HttpServletResponse response)
        throws ServletException, IOException
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     *
     * @param request an <code>HttpServletRequest</code> object that contains
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the POST request
     * @exception IOException if the request for the POST could not be handled
     */
    public void doPost(HttpServletRequest  request,
                    HttpServletResponse response)
        throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * Redirects the slo request to process logout servlet.
     * @param request an <code>HttpServletRequest</code> object that contains
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the request
     * @exception IOException if the request could not be handled
     */
    private void doGetPost(HttpServletRequest request,
                        HttpServletResponse response)
        throws ServletException, IOException
    {
        FSUtils.debug.message("FSSingleLogoutServlet doGetPost...");
        // Alias processing
        String providerAlias = request.getParameter(IFSConstants.META_ALIAS);
        if (providerAlias == null || providerAlias.length() < 1) {
            providerAlias = FSServiceUtils.getMetaAlias(request);
        }
        if (providerAlias == null || providerAlias.length() < 1) {
            FSUtils.debug.error("Unable to retrieve alias, Hosted" +
                " Provider. Cannot process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("aliasNotFound"));
            return;
        }

        request.setAttribute("logoutSource", "local");
        StringBuffer processLogout = new StringBuffer();
        processLogout.append(IFSConstants.SLO_VALUE)
            .append("/")
            .append(IFSConstants.META_ALIAS)
            .append(providerAlias);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("About to get RequestDispatcher for " +
                processLogout.toString());                                
        }
        RequestDispatcher dispatcher =
            getServletConfig().getServletContext().getRequestDispatcher(
                        processLogout.toString()) ;
        if ( dispatcher == null ) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "RequestDispatcher is null.\nUnable to find " +
                    processLogout);
            }
            FSUtils.debug.message("calling sendErrorPage ");
            FSLogoutUtil.sendErrorPage(
                request,
                response,
                providerAlias);
            return;
        }
        dispatcher.forward(request, response);
        return;
    }    
}   // FSSingleLogoutServlet
