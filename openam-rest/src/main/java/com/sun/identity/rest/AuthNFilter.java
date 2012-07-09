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
 * $Id: AuthNFilter.java,v 1.2 2009/11/12 18:37:35 veiming Exp $
 *
 */

package com.sun.identity.rest;

import com.sun.identity.rest.spi.IAuthentication;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class AuthNFilter implements Filter {
    public void destroy() {
        RestServiceManager.getInstance().destroy();
    }

    public void init(FilterConfig config) throws ServletException {
        RestServiceManager.getInstance().initAuthN(config);
    }

    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain)
        throws IOException, ServletException
    {
        IAuthentication auth = 
            RestServiceManager.getInstance().getAuthenticationFilter(
                (HttpServletRequest) request);
        if (auth == null) {
            ((HttpServletResponse) response).setStatus(434);
        } else {
            auth.doFilter(request, response, chain);
        }
    }

}
