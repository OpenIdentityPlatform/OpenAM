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
 */

package org.forgerock.openam.audit;

import org.forgerock.openam.shared.audit.context.AuditRequestContext;
import org.forgerock.openam.shared.audit.context.TransactionId;
import org.forgerock.openam.utils.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
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
            String transactionIdHeader = ((HttpServletRequest) request).getHeader(TransactionId.HTTP_HEADER);
            if (StringUtils.isNotBlank(transactionIdHeader)) {
                AuditRequestContext.set(
                        new AuditRequestContext(new TransactionId(transactionIdHeader)));
            }
        }
    }

    void clearRequestContext() {
        AuditRequestContext.clear();
    }

}
