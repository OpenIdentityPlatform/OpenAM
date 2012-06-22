/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthFilter.java,v 1.5 2009/07/31 21:53:47 farble1670 Exp $
 */

package com.sun.identity.admin;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletResponse;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

public class AuthFilter implements Filter {
    private static String LOGIN_PATH = "/UI/Login";

    private FilterConfig filterConfig = null;

    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isAuthenticated(httpRequest)) {
            chain.doFilter(request, response);
        } else {
            String redirect = getRedirectUrl(httpRequest);
            httpResponse.sendRedirect(redirect);
        }
    }

    private String getRedirectUrl(HttpServletRequest request) {
        String gotoUrl = getGotoUrl(request);
        String loginUrl = getLoginUrl(request);

        return loginUrl + "?goto=" + gotoUrl;
    }

    private String getLoginUrl(HttpServletRequest request) {
        StringBuffer loginUrl = new StringBuffer();

        String scheme = request.getScheme();
        String server = request.getServerName();
        int port = request.getServerPort();
        String path = request.getContextPath();

        loginUrl.append(scheme);
        loginUrl.append("://");
        loginUrl.append(server);
        loginUrl.append(":");
        loginUrl.append(port);
        loginUrl.append(path);

        loginUrl.append(LOGIN_PATH);

        return loginUrl.toString();
    }

    private String getGotoUrl(HttpServletRequest request) {
        String s = request.getRequestURL().toString();
        String qs = request.getQueryString();
        if (qs != null && qs.length() > 0) {
            s += "?" + qs;
        }

        return s;
    }

    private boolean isAuthenticated(HttpServletRequest httpRequest) {
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(httpRequest);

            return manager.isValidToken(ssoToken);
        } catch (SSOException ssoe) {
            return false;
        }
    }

    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
    }

    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
}
