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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.audit.AuditException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.slf4j.Logger;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Servlet filter responsible for auditing access to the SOAP STS.
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
        Auditor auditor = Singleton.getAuditorFactory().create((HttpServletRequest) request, auditableResponse);

        try {
            Singleton.getAuditEventPublisher().publish(AuditConstants.ACCESS_TOPIC,
                    auditor.auditAccessAttempt(component));
            try {
                chain.doFilter(request, auditableResponse);
            } finally {
                Singleton.getAuditEventPublisher().tryPublish(AuditConstants.ACCESS_TOPIC,
                        auditor.auditAccessOutcome(component));
            }
        } catch (AuditException e) {
            getLogger().error("Failed to publish audit event: {}", e.getMessage(), e);
        }
    }

    @Override
    public final void destroy() {
        // do nothing
    }

    /**
     * Gets the instance of the provider {@literal key}.
     *
     * @param key The key that defines the class to get.
     * @param <T> The type of class defined by the key.
     * @return A non-null instance of the class defined by the key.
     */
    protected <T> T getInstance(Key<T> key) {
        return InjectorHolder.getInstance(key);
    }

    /**
     * Gets the {@code Logger} instance.
     *
     * @return The logger.
     */
    protected Logger getLogger() {
        return getInstance(Key.get(Logger.class, Names.named("frRest")));
    }

    private enum Singleton {
        INSTANCE;

        private AuditorFactory auditorFactory;
        private AuditEventPublisher auditEventPublisher;

        Singleton() {
            this.auditorFactory = getInstance(Key.get(AuditorFactory.class));
            this.auditEventPublisher = getInstance(Key.get(AuditEventPublisher.class));
        }

        private static AuditorFactory getAuditorFactory() {
            return INSTANCE.auditorFactory;
        }

        private static AuditEventPublisher getAuditEventPublisher() {
            return INSTANCE.auditEventPublisher;
        }

        private <T> T getInstance(Key<T> key) {
            return InjectorHolder.getInstance(key);
        }
    }
}
