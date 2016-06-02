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
package org.forgerock.openam.rest.audit;

import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

import java.util.Set;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getAllAvailableTrackingIds;
import static org.forgerock.openam.audit.AuditConstants.Component;
import static org.forgerock.openam.audit.AuditConstants.USER_ID;

/**
 * Responsible for logging access audit events for UMA requests.
 *
 * @since 13.0.0
 */
public class UMAAccessAuditFilter extends OAuth2AbstractAccessAuditFilter {

    /**
     * Create a new {@link UMAAccessAuditFilter} filter for the given restlet.
     *
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     * @param providers The OAuth2 audit context providers, responsible for finding details which can be audit
     */
    public UMAAccessAuditFilter(Restlet restlet, AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory, Set<OAuth2AuditContextProvider> providers,
            RestletBodyAuditor<?> requestDetailCreator, RestletBodyAuditor<?> responseDetailCreator) {
        super(Component.OAUTH, restlet, auditEventPublisher, auditEventFactory, providers, requestDetailCreator,
                responseDetailCreator);
    }

    /**
     * {@inheritDoc}
     *
     * We are not expecting any user id information on an UMA response, and so just return anything already in the
     * {@link AuditRequestContext}.
     */
    @Override
    protected String getUserIdForAccessOutcome(Request request, Response response) {
        String userId = AuditRequestContext.getProperty(USER_ID);
        return userId == null ? "" : userId;
    }

    /**
     * {@inheritDoc}
     *
     * We are not expecting any context id information on an UMA response, and so just return anything already in the
     * {@link AuditRequestContext}.
     */
    @Override
    protected Set<String> getTrackingIdsForAccessOutcome(Request request, Response response) {
        return getAllAvailableTrackingIds();
    }
}
