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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.audit.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * {@link HttpServletResponse} decorator used for capturing response status and message.
 *
 * @since 13.0.0
 */
public class AuditableHttpServletResponse extends HttpServletResponseWrapper {

    private static final int LOWEST_ERROR_CODE = SC_BAD_REQUEST;
    private int statusCode = SC_OK;
    private String message = "";

    /**
     * Constructs a new AuditableHttpServletResponse.
     *
     * @param response The {@code HttpServletResponse} to audit.
     */
    public AuditableHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    /**
     * Determines if the response is successful.
     *
     * @return {@code true} if the response is successful, {@code false} otherwise.
     */
    public boolean hasSuccessStatusCode() {
        return statusCode < LOWEST_ERROR_CODE;
    }

    /**
     * Gets the HTTP response status code.
     *
     * @return The response HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the HTTP response message.
     *
     * @return The response message.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public void sendError(int sc) throws IOException {
        super.sendError(sc);
        this.statusCode = sc;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        super.sendError(sc, msg);
        this.statusCode = sc;
        this.message = msg;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(location);
        this.statusCode = SC_MOVED_TEMPORARILY;
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        this.statusCode = sc;
    }

    @Override
    public void setStatus(int sc, String sm) {
        super.setStatus(sc, sm);
        this.statusCode = sc;
        this.message = sm;
    }
}