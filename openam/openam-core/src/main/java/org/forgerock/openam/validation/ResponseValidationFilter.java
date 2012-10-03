/*
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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 */

package org.forgerock.openam.validation;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Filter for global validation of responses. Validates headers, including cookies and location, for banned characters
 * or sequences. Currently CRLF is the only sequence that is checked for as protection from HTTP response splitting.
 *
 * @author Jonathan Scudder
 */
public class ResponseValidationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No configuration necessary
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletResponse instanceof HttpServletResponse) {
            servletResponse = new ValidationWrapper((HttpServletResponse) servletResponse);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        // Do nothing
    }

    public class ValidationWrapper extends HttpServletResponseWrapper {

        ValidationWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addCookie(Cookie cookie) {
            if (cookie != null && cookie.getValue() != null) {
                cookie.setValue(validate(cookie.getValue()));
            }
            super.addCookie(cookie);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            super.sendRedirect(validate(location));
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, validate(value));
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, validate(value));
        }

        private String validate(String value) {
            if (value == null) {
                return null;
            }

            return value.replace("\r", "").replace("\n","");
        }
    }
}
