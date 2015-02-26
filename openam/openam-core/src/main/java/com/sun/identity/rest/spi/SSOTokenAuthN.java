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
 * $Id: SSOTokenAuthN.java,v 1.2 2009/11/12 18:37:35 veiming Exp $
 */

package com.sun.identity.rest.spi;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.rest.HttpServletRequestWrapperEx;
import com.sun.identity.rest.SSOTokenPrincipal;
import com.sun.identity.rest.RestServiceManager;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class SSOTokenAuthN implements IAuthentication {

    public String[] accept() {
        String[] method = { RestServiceManager.DEFAULT_AUTHN_SCHEME };
        return method;
    }

    private void redirect(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        response.setHeader("Location",
            request.getContextPath() + "/UI/Login");
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    private boolean hasCookie(HttpServletRequest request) {
        String cookieName = SystemProperties.get("com.iplanet.am.cookie.name");
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookie.getName().equals(cookieName)) {
                return true;
            }
        }
        return false;
    }

    public void init(FilterConfig arg0) throws ServletException {

    }

    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        if (!hasCookie((HttpServletRequest)request)) {
            redirect((HttpServletRequest)request,
                (HttpServletResponse)response);
        } else {
            try {
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                SSOToken token = mgr.createSSOToken(
                    (HttpServletRequest)request);
                HttpServletRequestWrapperEx reqWrapper = new
                    HttpServletRequestWrapperEx((HttpServletRequest) request);

                reqWrapper.setUserPrincipal(new SSOTokenPrincipal(token));
                chain.doFilter(reqWrapper, response);
            } catch (SSOException e) {
                redirect((HttpServletRequest)request,
                    (HttpServletResponse)response);
            }
        }

    }

    public void destroy() {
    }

}
