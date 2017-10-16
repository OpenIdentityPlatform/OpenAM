/*
 * Copyright 2013-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */

package org.forgerock.openam.xui;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchemaManager;
import com.google.common.annotations.VisibleForTesting;
import org.forgerock.guice.core.InjectorHolder;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;

/**
 * XUIFilter class is a servlet Filter for filtering incoming requests to OpenAM and redirecting them
 * to XUI or classic UI by inspecting the attribute openam-xui-interface-enabled in the iPlanetAMAuthService
 * service.
 *
 * @author Travis
 */
public class XUIFilter implements Filter {

    private String xuiBasePath;
    private static final String LOGIN_PATH = "#login/";
    private static final String LOGOUT_PATH = "#logout/";
    private static final String PROFILE_PAGE_PATH = "#profile/";
    protected volatile boolean initialized;
    private ServiceSchemaManager scm = null;
    private XUIState xuiState;
    
    private final Debug DEBUG = Debug.getInstance("Configuration");

    public XUIFilter() {}

    @VisibleForTesting XUIFilter(XUIState xuiState) {
        this.xuiState = xuiState;
    }

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) {
        if (xuiState == null) {
            xuiState = InjectorHolder.getInstance(XUIState.class);
        }
        ServletContext ctx = filterConfig.getServletContext();
        xuiBasePath = ctx.getContextPath() + "/XUI";
    }

    /**
     * {@inheritDoc}
     */
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain)
            throws IOException, ServletException {

        if (!(servletResponse instanceof HttpServletResponse) || !(servletRequest instanceof HttpServletRequest)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        if (xuiState.isXUIEnabled() && request.getRequestURI() != null) {
            String query = request.getQueryString();
            if (request.getRequestURI().contains("UI/Logout")) {
                sendRedirect(response, xuiBasePath, query, LOGOUT_PATH);
            } else if (request.getRequestURI().contains("idm/EndUser")) {
                sendRedirect(response, xuiBasePath, query, PROFILE_PAGE_PATH);
            } else {
                String compositeAdvice = request.getParameter(Constants.COMPOSITE_ADVICE);
                if (compositeAdvice != null) {
                    try {
                        query = mapCompositeAdviceFromRequest(request);
                    } catch (EncodingException e) {
                        DEBUG.error("XUIFilter.doFilter::  failed to encode composite_advice : " + compositeAdvice, e);
                    }
                }
                sendRedirect(response, xuiBasePath, query, LOGIN_PATH);
            }
        } else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    private void sendRedirect(HttpServletResponse response, String xuiBasePath, String queryString, String xuiHash)
            throws IOException {
        if (queryString == null) {
            queryString = "";
        } else {
            queryString = "?" + queryString;
        }
        response.sendRedirect(xuiBasePath + queryString + xuiHash);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        xuiState.destroy();
    }

    private String mapCompositeAdviceFromRequest(HttpServletRequest request)
            throws ServletException, EncodingException {
        Map<String, String[]> parameterNames = request.getParameterMap();
        String queryString = "";

        if (parameterNames != null) {
            for (Map.Entry<String, String[]> entry : parameterNames.entrySet()) {
                String paramName = entry.getKey();
                String[] paramValues = entry.getValue();
                if (paramName != null && !paramName.equalsIgnoreCase(Constants.COMPOSITE_ADVICE)) {
                    try {
                        if (paramValues != null) {
                            for (String paramValue : paramValues) {
                                if (!queryString.isEmpty()) {
                                    queryString += "&";
                                }
                                queryString += paramName + "=" + ESAPI.encoder().encodeForURL(paramValue);
                            }
                        }
                    } catch (EncodingException e) {
                        DEBUG.message("XUIFilter.doFilter::  failed to encode " + paramName + " : " + paramValues);
                    }
                }
            }
        }

        String compositeAdvice = ESAPI.encoder().encodeForURL(request.getParameter(Constants.COMPOSITE_ADVICE));
        String authIndexType  = "authIndexType=composite_advice";
        String authIndexValue = "authIndexValue=" + compositeAdvice;
        if (queryString.isEmpty()) {
            return authIndexType + "&" + authIndexValue;
        } else {
            return queryString + "&" + authIndexType + "&" + authIndexValue;
        }
    }
}
