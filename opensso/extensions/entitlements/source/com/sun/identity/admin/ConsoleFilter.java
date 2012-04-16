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
 * $Id: ConsoleFilter.java,v 1.2 2009/07/13 19:42:41 farble1670 Exp $
 */

package com.sun.identity.admin;

import com.sun.identity.entitlement.opensso.EntitlementService;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletResponse;

public class ConsoleFilter implements Filter {
    private FilterConfig filterConfig = null;

    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!EntitlementService.toUseNewConsole()) {
            String redirect = getRedirectUrl(httpRequest);
            httpResponse.sendRedirect(redirect);
        } else {
            chain.doFilter(request, response);
        }
    }

    private String getRedirectUrl(HttpServletRequest request) {
        StringBuffer redirect = new StringBuffer();

        String scheme = request.getScheme();
        String server = request.getServerName();
        int port = request.getServerPort();
        String path = request.getContextPath();

        redirect.append(scheme);
        redirect.append("://");
        redirect.append(server);
        redirect.append(":");
        redirect.append(port);
        redirect.append(path);
        redirect.append("/console");

        return redirect.toString();
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
