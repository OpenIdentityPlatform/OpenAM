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
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.openam.oauth2;

import jakarta.inject.Inject;

import org.forgerock.openam.audit.AbstractHttpAccessAuditFilter;
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.Context;

/**
 * Access audit filter for the OAuth2 endpoints that CHF serves: component {@code OAuth}, realm from the
 * {@link RealmContext}. The user ID and the tracking IDs are those the audit request context already holds; unlike
 * the Restlet {@code OAuth2AccessAuditFilter}, this filter does not read them from a session cookie or from OAuth2
 * tokens.
 */
public class OAuth2HttpAccessAuditFilter extends AbstractHttpAccessAuditFilter {

    /**
     * Creates the filter.
     *
     * @param auditEventPublisher The publisher of the audit events.
     * @param auditEventFactory The factory of the audit events.
     */
    @Inject
    public OAuth2HttpAccessAuditFilter(AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory) {
        super(Component.OAUTH, auditEventPublisher, auditEventFactory);
    }

    @Override
    protected String getRealm(Context context) {
        return context.containsContext(RealmContext.class)
                ? context.asContext(RealmContext.class).getRealm().asPath()
                : null;
    }
}
