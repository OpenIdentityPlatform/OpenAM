/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoginLogoutMapping.java,v 1.4 2008/06/25 05:41:52 qcheng Exp $
 *
 */


 
package com.sun.identity.authentication.distUI;           

import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;
import javax.servlet.*;
import javax.servlet.http.*;

/** 
 *
 * Servlet mapping to forward
 * '/login' to '/UI/Login' and
 * '/logout' to '/UI/Logout'
 *
 */
public class LoginLogoutMapping extends HttpServlet {

    // the debug file
    private static Debug debug = Debug.getInstance("amLoginLogoutMapping");

    ServletConfig config = null;
    
    /** 
     * Initializes the servlet.
     * @param config servlet config
     * @throws ServletException if it fails to get servlet context.
    */  
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.config = config;
    }

    /** Destroys the servlet.
    */  
    public void destroy() {

    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request.
     * @param response servlet response.
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void processRequest(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, java.io.IOException {

        // Check content length
        try {
            RequestUtils.checkContentLength(request);
        } catch (L10NMessageImpl e) {
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            java.util.Locale locale = localeContext.getLocale();
            if (debug.messageEnabled()) {
                debug.message("LoginLogoutMapping: " +
                    e.getL10NMessage(locale));
            }
            throw new ServletException(e.getL10NMessage(locale));
        }

        String servletPath = request.getServletPath();
        String forwardUrl = "";
        if (servletPath.equals("/login")) {
            forwardUrl = "/UI/Login";
        } else if (servletPath.equals("/logout")) {
            forwardUrl = "/UI/Logout";
        }
        RequestDispatcher dispatcher =
        config.getServletContext().getRequestDispatcher(forwardUrl);
        dispatcher.forward(request, response);
        return;         
    } 

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request.
     * @param response servlet response.
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, java.io.IOException {
        processRequest(request, response);
    } 

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request.
     * @param response servlet response.
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void doPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

}
