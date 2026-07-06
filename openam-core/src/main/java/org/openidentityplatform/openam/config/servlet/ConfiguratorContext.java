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
 * Copyright 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Thin per-request wrapper over the servlet request/response/session, injected into a
 * {@link SetupPage} instead of Apache Click's {@code Context.getThreadLocalContext()}.
 */
public class ConfiguratorContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public ConfiguratorContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public HttpSession getSession() {
        return request.getSession();
    }

    public Object getSessionAttribute(String name) {
        HttpSession session = request.getSession(false);
        return session == null ? null : session.getAttribute(name);
    }

    public void setSessionAttribute(String name, Object value) {
        getSession().setAttribute(name, value);
    }

    public void removeSessionAttribute(String name) {
        HttpSession session = request.getSession(false);
        if (session != null && name != null) {
            session.removeAttribute(name);
        }
    }

    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }
}
