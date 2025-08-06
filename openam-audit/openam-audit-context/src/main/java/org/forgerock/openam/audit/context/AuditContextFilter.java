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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.audit.context;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.header.TransactionIdHeader;
import org.forgerock.services.TransactionId;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Servlet filter for initializing thread local RequestContext when request handling begins
 * and discarding it when handling completes.
 *
 * @since 13.0.0
 */
public class AuditContextFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        setRequestContext(request);
        try {
            chain.doFilter(request, response);
        } finally {
            clearRequestContext();
        }

    }

    @Override
    public void destroy() {
        // do nothing
    }

    private void setRequestContext(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            TransactionIdConfiguration configuration = InjectorHolder.getInstance(TransactionIdConfiguration.class);
            if (configuration.trustHttpTransactionHeader()) {
                String transactionIdHeader = ((HttpServletRequest) request).getHeader(TransactionIdHeader.NAME);
                if (transactionIdHeader != null && !transactionIdHeader.trim().isEmpty()) {
                    AuditRequestContext.set(
                            new AuditRequestContext(new TransactionId(transactionIdHeader)));
                }
            }
        }
    }

    void clearRequestContext() {
        AuditRequestContext.clear();
    }

}
