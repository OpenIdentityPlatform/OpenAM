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
 * $Id: ClientConfiguratorFilter.java,v 1.7 2009/12/05 02:33:36 222713 Exp $
 *
 */
package com.sun.identity.setup;

import com.iplanet.am.util.SystemProperties;
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

/**
 * This filter brings user to a configuration page when client sample WAR
 * is not configured yeti, it will set the configuration properties if
 * the WAR is already configured.
 */
public final class ClientConfiguratorFilter implements Filter {
    private FilterConfig config;
    private ServletContext servletCtx;
    // see if the configure.jsp page is executed
    public static boolean isConfigured = false;
    private static final String SETUP_URI = "/Configurator.jsp";
    private static String configFile = null;
    private boolean passThrough = false;

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
            // pass through on Configurator  page
            if (httpRequest.getRequestURI().endsWith(SETUP_URI)) {
                passThrough = true;
                filterChain.doFilter(httpRequest, httpResponse);
            } else if (passThrough) {
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
            throw new ServletException("ClientConfiguratorFilter.doFilter", ex);
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
        configFile = System.getProperty("openssoclient.config.folder");
        if (configFile == null || configFile.length() == 0) {
            configFile = System.getProperty("user.home"); 
        }
        configFile = configFile + File.separator +
            SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR + File.separator +
            SetupClientWARSamples.getNormalizedRealPath(servletCtx) +
            "AMConfig.properties";

        File file = new File(configFile);
        if (file.exists()) {
            setAMConfigProperties(configFile);
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
     * Sets properties from AMConfig.properties
     * @param configFile path to the AMConfig.properties file
     * @throws ServletException when error occurs
     */
    private void setAMConfigProperties(String configFile) 
        throws ServletException {
        FileInputStream fileStr = null;
        try {
            fileStr = new FileInputStream(configFile);
            if (fileStr != null) {
                Properties props = new Properties();
                props.load(fileStr);
                SystemProperties.initializeProperties(props);
                isConfigured = true;
            } else {
                throw new ServletException("Unable to open: " + configFile);
            }
        } catch (FileNotFoundException fexp) {
            fexp.printStackTrace();
            throw new ServletException(fexp.getMessage());
        } catch (IOException ioexp) {
            ioexp.printStackTrace();
            throw new ServletException(ioexp.getMessage());
        } finally {
            if (fileStr != null) {
                try {
                    fileStr.close();
                } catch (IOException ioe) {
                }
            } 
        }
    }
}
