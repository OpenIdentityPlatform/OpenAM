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

import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.utils.StringUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

import java.util.Set;

import static org.forgerock.openam.audit.AuditConstants.USER_ID;

/**
 * Responsible for logging access audit events for all OAuth2-based filters. Common functionality is here, a filter
 * may overwrite this functionality if there is a known difference in access or outcome details for that filter.
 *
 * @since 13.0.0
 */
public abstract class OAuth2AbstractAccessAuditFilter extends AbstractRestletAccessAuditFilter {

    Set<OAuth2AuditContextProvider> providers;

    /**
     * Create a new filter for the given component and restlet.
     *
     * @param component The component for which events will be logged.
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     * @param providers
     */
    public OAuth2AbstractAccessAuditFilter(AuditConstants.Component component, Restlet restlet,
            AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory,
            Set<OAuth2AuditContextProvider> providers, RestletBodyAuditor requestDetailCreator,
            RestletBodyAuditor responseDetailCreator) {
        super(component, restlet, auditEventPublisher, auditEventFactory, requestDetailCreator, responseDetailCreator);
        this.providers = providers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUserIdForAccessAttempt(Request request) {
        String userId = super.getUserIdForAccessAttempt(request);
        if (StringUtils.isNotEmpty(userId)) {
            return userId;
        }

        putUserIdInAuditRequestContext(request);

        return super.getUserIdForAccessAttempt(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getTrackingIdsForAccessAttempt(Request request) {
        putTrackingIdsIntoAuditRequestContext(request);

        return super.getTrackingIdsForAccessAttempt(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUserIdForAccessOutcome(Request request, Response response) {
        String userId = super.getUserIdForAccessOutcome(request, response);
        if (StringUtils.isNotEmpty(userId)) {
            return userId;
        }

        putUserIdInAuditRequestContext(request);

        return super.getUserIdForAccessOutcome(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getTrackingIdsForAccessOutcome(Request request, Response response) {
        putTrackingIdsIntoAuditRequestContext(request);

        return super.getTrackingIdsForAccessOutcome(request, response);
    }

    private void putUserIdInAuditRequestContext(Request request) {
        for (OAuth2AuditContextProvider provider : providers) {
            String userId = provider.getUserId(request);
            if (userId != null) {
                AuditRequestContext.putProperty(USER_ID, userId);
                break;
            }
        }

        return;
    }

    private void putTrackingIdsIntoAuditRequestContext(Request request) {
        for (OAuth2AuditContextProvider provider : providers) {
            String trackingId = provider.getTrackingId(request);

            if (trackingId != null) {
                AuditRequestContext.putProperty(provider.getTrackingIdKey().toString(), trackingId);
            }
        }
    }
}
