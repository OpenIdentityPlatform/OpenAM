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
 * $Id: AuthZFilter.java,v 1.3 2009/11/16 21:42:19 veiming Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.rest;

import com.sun.identity.rest.spi.IAuthorization;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AuthZFilter implements Filter {

    public void destroy() {
        RestServiceManager.getInstance().destroy();
    }

    public void init(FilterConfig config) throws ServletException {
        RestServiceManager.getInstance().initAuthZ(config);
    }

    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain)
        throws IOException, ServletException
    {
        IAuthorization auth = 
            RestServiceManager.getInstance().getAuthorizationFilter(
            (HttpServletRequest) request);
        if (auth == null) {
            ((HttpServletResponse) response).sendError(
                HttpServletResponse.SC_BAD_REQUEST);
            return;
        } else {
            auth.doFilter(request, response, chain);
        }
    }
}
