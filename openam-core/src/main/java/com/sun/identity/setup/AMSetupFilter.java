/*
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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.setup;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;

import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.shared.Constants;

/**
 * This filter brings the administrator to a configuration page where the product can be configured
 * if the product is not yet configured.
*/
public final class AMSetupFilter implements Filter {

    private static final String SETUP_URI = "/config/options.htm";
    private static final String UPGRADE_URI = "/config/upgrade/upgrade.htm";
    private static final String SETUP_PROGRESS_URI = "/setup/setSetupProgress";
    private static final String UPGRADE_PROGESS_URI = "/upgrade/setUpgradeProgress";
    private static final String CONFIGURATOR_URI = "configurator";

    private static final String AM_ENCRYPTION_PASSWORD_PROPERTY_KEY = "am.enc.pwd";
    private static final String CONFIG_STORE_DOWN_ERROR_CODE = "configstore.down";
    private static final String NOWRITE_PERMISSION_ERROR_CODE = "nowrite.permission";

    private static final Collection<String> ALLOWED_RESOURCES = CollectionUtils.asSet("SMSObjectIF", "setSetupProgress",
            "setUpgradeProgress", "/legal-notices/");
    private static final Collection<String> ALLOWED_FILE_EXTENSIONS = CollectionUtils.asSet(".ico", ".htm", ".css",
            ".js", ".jpg", ".gif", ".png");

    private AMSetupManager setupManager;
    private volatile boolean isPassthrough;

    @Override
    public void init(FilterConfig config) throws ServletException {
        // Initialise the OpenAM Time enum.
        System.out.println("Starting up OpenAM at " +
                DateFormat.getDateTimeInstance().format(Time.getCalendarInstance().getTime()));
        ServletContext servletContext = config.getServletContext();
        SystemStartupInjectorHolder startupInjectorHolder = SystemStartupInjectorHolder.getInstance();
        setupManager = startupInjectorHolder.getInstance(AMSetupManager.class);
        if (!setupManager.isConfigured()) {
            // TASK TODO figure out if and why we need to do this here
            servletContext.setAttribute(AM_ENCRYPTION_PASSWORD_PROPERTY_KEY, AMSetupUtils.getRandomString());
        }
    }

    /**
     * Redirects requests to configuration page if the product is not yet configured.
     *
     * @param req The HTTP request.
     * @param resp The HTTP response.
     * @param filterChain The filter chain.
     * @throws IOException If configuration file cannot be read.
     * @throws ServletException If there are errors in the servlet space.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain) throws IOException,
            ServletException {
        HttpServletRequest  request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        try {
            if (setupManager.isCurrentConfigurationValid()) {
                if (isSetupRequest(request.getRequestURI())) {
                    response.sendRedirect(createCleanUrl(request));
                } else {
                    filterChain.doFilter(request, response);
                }
            } else {
                if (isConfigStoreDown()) {
                    String redirectUrl = System.getProperty(Constants.CONFIG_STORE_DOWN_REDIRECT_URL);
                    if (StringUtils.isNotEmpty(redirectUrl)) {
                        response.sendRedirect(redirectUrl);
                    } else {
                        throw new ConfigurationException(CONFIG_STORE_DOWN_ERROR_CODE, null);
                    }
                } else {
                    if (isPassthrough && isRequestForAllowedResource(resourcePath(request))) {
                        filterChain.doFilter(request, response);
                    } else if (isConfiguratorRequest(request.getRequestURI())) {
                        filterChain.doFilter(request, response);
                    } else {
                        String url = createCleanUrl(request);
                        if (hasWritePermissionOnUserHomeDirectory()) {
                            url += SETUP_URI;
                        } else {
                            throw new ConfigurationException(NOWRITE_PERMISSION_ERROR_CODE,
                                    new String[] {setupManager.getUserHomeDirectory().getAbsolutePath()});
                        }
                        response.sendRedirect(url);
                        enablePassthrough();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("AMSetupFilter.doFilter", e);
        }
    }

    private String resourcePath(HttpServletRequest request) {
        StringBuilder path = new StringBuilder();
        if (request.getServletPath() != null) {
            path.append(request.getServletPath());
        }
        if (request.getPathInfo() != null) {
            path.append(request.getPathInfo());
        }
        return path.toString();
    }

    @Override
    public void destroy() {
    }

    private boolean isConfigStoreDown() {
        return setupManager.getBootStrapFileLocation() != null && !setupManager.isVersionNewer()
                && !setupManager.isUpgradeCompleted();
    }

    private boolean isSetupRequest(String requestUri) {
        return requestUri.endsWith(SETUP_URI) || requestUri.endsWith(SETUP_PROGRESS_URI)
                || requestUri.endsWith(UPGRADE_URI) || requestUri.endsWith(UPGRADE_PROGESS_URI);
    }

    private String createCleanUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }

    private boolean isRequestForAllowedResource(String servletPath) {
        for (String allowedResource : ALLOWED_RESOURCES) {
            if (servletPath.contains(allowedResource)) {
                return true;
            }
        }
        for (String allowedExtension : ALLOWED_FILE_EXTENSIONS) {
            if (servletPath.toLowerCase().endsWith(allowedExtension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConfiguratorRequest(String requestUri) {
        return requestUri.endsWith(CONFIGURATOR_URI);
    }

    private boolean hasWritePermissionOnUserHomeDirectory() {
        return setupManager.getUserHomeDirectory().canWrite();
    }

    private void enablePassthrough() {
        isPassthrough = true;
    }
}
