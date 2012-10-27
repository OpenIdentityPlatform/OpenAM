/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DistAuthConfiguratorFilter.java,v 1.4 2008/06/25 05:40:27 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.distauth.setup;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;
import org.forgerock.openam.distauth.DistAuthConfiguratorHelper;

/**
 * This filter brings user to a configuration page when DistAuth WAR
 * is not configured yet, it will set the configuration properties if
 * the WAR is already configured.
 */
public final class DistAuthConfiguratorFilter implements Filter {
    private FilterConfig config;
    private ServletContext servletCtx;
    // see if the configure.jsp page is executed
    public static boolean isConfigured = false;
    private static final String SETUP_URI = "/distAuthConfigurator.jsp";
    private boolean passThrough = false;
    private static String[] fList = { 
        ".htm", ".css", ".js", ".jpg", ".gif", ".png" 
    };    

    /**
     * Redirects request to configuration page if the product is not yet
     * configured.
     *
     * @param request Servlet Request.
     * @param response Servlet Response.
     * @param filterChain Filter Chain.
     * @throws ServletException if there are errors in the servlet space.
     */
    public void doFilter(
        ServletRequest request, 
        ServletResponse response, 
        FilterChain filterChain
    ) throws IOException, ServletException {
        HttpServletRequest  httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response ;
        try {
            // pass through on distAuthConfigurator.jsp page
            if (httpRequest.getRequestURI().endsWith(SETUP_URI)) {
                passThrough = true;
                filterChain.doFilter(httpRequest, httpResponse);
            } else if (passThrough && validateStream(httpRequest)) {
                filterChain.doFilter(httpRequest, httpResponse);
            } else if (!isConfigured) {
                String url = httpRequest.getScheme() + "://" +
                     httpRequest.getServerName() + ":" +
                     httpRequest.getServerPort() +
                     httpRequest.getContextPath() + SETUP_URI;
                httpResponse.sendRedirect(url);
                passThrough = true;
            } else {
                filterChain.doFilter(httpRequest, httpResponse);
            }
        } catch(Exception ex) {
            throw new ServletException("DistAuthConfiguratorFilter.doFilter", ex);
        }
    }

    /**
     * Destroy the filter config on sever shutdowm 
     */
    public void destroy() {
        config = null;
    }
    
    /**
     * Initializes the filter.
     *
     * @param filterConfig Filter Configuration.
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        setFilterConfig(filterConfig);
        servletCtx = filterConfig.getServletContext();
        isConfigured = DistAuthConfiguratorHelper.initialiseDistAuth(servletCtx);
    }
    
    /**
     * Initializes the filter configuration.
     *
     * @param fconfig Filter Configuration.
     */
    public void setFilterConfig(FilterConfig fconfig) {
        config = fconfig;
    }
    
    /**
     * Returns <code>true</code> if the request for resources.
     *
     * @param httpRequest HTTP Servlet request.
     * @return <code>true</code> if the request for resources.
     */
    private boolean validateStream(HttpServletRequest httpRequest) {
        String uri =  httpRequest.getRequestURI();
        boolean ok = false;
        for (int i = 0; (i < fList.length) && !ok; i++) {
            ok = (uri.indexOf(fList[i]) != -1);
        }
        return ok;     
    }    
}
