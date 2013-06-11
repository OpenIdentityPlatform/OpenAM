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
 * $Id: AMSetupFilter.java,v 1.12 2008/07/13 06:06:49 kevinserwin Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.setup;

import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.shared.Constants;
import java.io.File;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.openam.upgrade.UpgradeUtils;

/**
 * This filter brings administrator to a configuration page
 * where the product can be configured if the product is not
 * yet configured.
*/
public final class AMSetupFilter implements Filter {
    private FilterConfig config;
    private ServletContext servletCtx;
    private boolean initialized;
    private boolean passthrough;
    private static final String SETUPURI = "/config/options.htm";
    private static final String NOWRITE_PERMISSION = "/nowritewarning.jsp";

    private static String[] fList = { 
        ".htm", ".css", ".js", ".jpg", ".gif", ".png",".JPG", "SMSObjectIF" , "setSetupProgress",
        "setUpgradeProgress"
    }; 

    /**
     * Redirects request to configuration page if the product is not yet 
     * configured.
     *
     * @param request Servlet Request.
     * @param response Servlet Response.
     * @param filterChain Filter Chain.
     * @throws IOException if configuration file cannot be read.
     * @throws ServletException if there are errors in the servlet space.
     */
    public void doFilter(
        ServletRequest request, 
        ServletResponse response, 
        FilterChain filterChain
    ) throws IOException, ServletException 
    {
        HttpServletRequest  httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response ;
        try {
            if (AMSetupServlet.isCurrentConfigurationValid()) {
                String incomingURL = httpRequest.getRequestURI();
                if (incomingURL.endsWith("/config/options.htm")
                        || incomingURL.endsWith("/config/upgrade/upgrade.htm"))
                {
                    String url = httpRequest.getScheme() + "://" +
                        httpRequest.getServerName() + ":" +
                        httpRequest.getServerPort() +
                        httpRequest.getContextPath();
                    httpResponse.sendRedirect(url);
                } else {
                    filterChain.doFilter(httpRequest, httpResponse);
                }
            } else {
                if (AMSetupServlet.getBootStrapFile() != null && !UpgradeUtils.isVersionNewer() 
                        && !AMSetupServlet.isUpgradeCompleted()) {
                    String redirectUrl = System.getProperty(Constants.CONFIG_STORE_DOWN_REDIRECT_URL);
                    if (redirectUrl != null && redirectUrl.length() > 0) {
                        httpResponse.sendRedirect(redirectUrl);
                    } else {
                        throw new ConfigurationException("configstore.down", null);
                    }
                } else {
                    if (isPassthrough() && validateStream(httpRequest)) {
                        filterChain.doFilter(httpRequest, httpResponse);
                    } else {        
                        String incomingURL = httpRequest.getRequestURI();
                        if (incomingURL.endsWith("configurator")) {
                            filterChain.doFilter(httpRequest, httpResponse);  
                        } else {
                            String url = httpRequest.getScheme() + "://" +
                                httpRequest.getServerName() + ":" +
                                httpRequest.getServerPort() +
                                httpRequest.getContextPath();
                            if ((new File(System.getProperty("user.home"))).canWrite()){
                                url += SETUPURI;
                            } else {
                                url += NOWRITE_PERMISSION;
                            }
                            httpResponse.sendRedirect(url);
                            markPassthrough();
                        }    
                    }
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new ServletException("AMSetupFilter.doFilter", ex);
        }
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
    public void init(FilterConfig filterConfig) {
        setFilterConfig(filterConfig);
        servletCtx = filterConfig.getServletContext();
        initialized = AMSetupServlet.checkInitState(servletCtx); 
        if (!initialized) {
            //Set the encryption Key
            servletCtx.setAttribute("am.enc.pwd",
                AMSetupServlet.getRandomString());
        }
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
     * Returns <code>true</code> if the request is allowed without processing.
     *
     * @return <code>true</code> if the request is allowed without processing.
     */
    private boolean isPassthrough() {
        return passthrough;
    }

    /**
     * Sets the request for images such that they are not processed.
     */
    private void markPassthrough() {
        passthrough = true;
    }
}
