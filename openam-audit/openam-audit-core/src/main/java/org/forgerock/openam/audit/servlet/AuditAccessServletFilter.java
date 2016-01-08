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
package org.forgerock.openam.audit.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.slf4j.Logger;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Servlet filter responsible for auditing access to generic HTTP endpoints.
 *
 * @since 13.0.0
 */
public class AuditAccessServletFilter implements Filter {

    private AuditConstants.Component component;

    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {
        component = AuditConstants.Component.valueOf(filterConfig.getInitParameter("auditing-component").toUpperCase());
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        AuditableHttpServletResponse auditableResponse =
                new AuditableHttpServletResponse((HttpServletResponse) response);
        Auditor auditor = Singleton.getAuditorFactory().create((HttpServletRequest) request, auditableResponse,
                component);

        Singleton.getAuditEventPublisher().tryPublish(AuditConstants.ACCESS_TOPIC, auditor.auditAccessAttempt());
        try {
            chain.doFilter(request, auditableResponse);
        } finally {
            Singleton.getAuditEventPublisher().tryPublish(AuditConstants.ACCESS_TOPIC,
                    auditor.auditAccessOutcome());
        }
    }

    @Override
    public final void destroy() {
        // do nothing
    }

    private enum Singleton {
        INSTANCE;

        private AuditorFactory auditorFactory;
        private AuditEventPublisher auditEventPublisher;
        private Logger logger;

        Singleton() {
            this.auditorFactory = InjectorHolder.getInstance(Key.get(AuditorFactory.class));
            this.auditEventPublisher = InjectorHolder.getInstance(Key.get(AuditEventPublisher.class));
        }

        private static AuditorFactory getAuditorFactory() {
            return INSTANCE.auditorFactory;
        }

        private static AuditEventPublisher getAuditEventPublisher() {
            return INSTANCE.auditEventPublisher;
        }

        private static Logger getLogger() {
            if (INSTANCE.logger == null) {
                INSTANCE.logger = InjectorHolder.getInstance(Key.get(Logger.class, Names.named("frRest")));
            }
            return INSTANCE.logger;
        }
    }
}
