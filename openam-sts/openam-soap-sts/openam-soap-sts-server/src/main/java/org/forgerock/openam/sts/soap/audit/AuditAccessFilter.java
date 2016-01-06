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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sts.soap.audit;

import static org.forgerock.openam.audit.AuditConstants.Component.STS;

import com.google.inject.Key;

import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.servlet.AuditableHttpServletResponse;
import org.forgerock.openam.audit.servlet.Auditor;
import org.forgerock.openam.audit.servlet.AuditorFactory;
import org.forgerock.openam.sts.soap.config.SoapSTSInjectorHolder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet filter responsible for auditing access to the SOAP STS.
 *
 * @since 13.0.0
 */
public final class AuditAccessFilter implements Filter {

    private AuditorFactory auditorFactory;
    private AuditEventPublisher auditEventPublisher;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        auditorFactory = SoapSTSInjectorHolder.getInstance(Key.get(AuditorFactory.class));
        auditEventPublisher = SoapSTSInjectorHolder.getInstance(Key.get(AuditEventPublisher.class));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        AuditableHttpServletResponse auditableResponse =
                new AuditableHttpServletResponse((HttpServletResponse) response);
        Auditor auditor = auditorFactory.create((HttpServletRequest) request, auditableResponse, STS);

        auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditor.auditAccessAttempt());
        try {
            chain.doFilter(request, auditableResponse);
        } finally {
            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditor.auditAccessOutcome());
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }

}
