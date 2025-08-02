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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.core.rest.docs;

import jakarta.inject.Inject;

import org.forgerock.openam.audit.AbstractHttpAccessAuditFilter;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.Context;

/**
 * The access audit filter for the docs endpoint.
 *
 * @since 14.0.0
 */
class DocsAccessAuditFilter extends AbstractHttpAccessAuditFilter {

    /**
     * Create a new filter for the given component.
     *
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     */
    @Inject
    public DocsAccessAuditFilter(AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory) {
        super(AuditConstants.Component.DOCUMENTATION, auditEventPublisher, auditEventFactory);
    }

    @Override
    protected String getRealm(Context context) {
        if (context.containsContext(RealmContext.class)) {
            return context.asContext(RealmContext.class).getRealm().asPath();
        }
        return null;
    }

}
