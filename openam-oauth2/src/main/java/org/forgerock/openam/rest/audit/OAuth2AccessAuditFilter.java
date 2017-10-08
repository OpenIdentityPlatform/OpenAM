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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.rest.audit;

import static org.forgerock.openam.audit.AuditConstants.Component;

import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.restlet.Restlet;

/**
 * Responsible for logging access audit events for OAuth2 and OIDC requests.
 *
 * @since 13.0.0
 */
public class OAuth2AccessAuditFilter extends OAuth2AbstractAccessAuditFilter {

    /**
     * Create a new {@link OAuth2AccessAuditFilter} for the given restlet.
     *
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     * @param requestFactory The factory that provides access to OAuth2Request.
     */
    public OAuth2AccessAuditFilter(Restlet restlet, AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory, OAuth2RequestFactory requestFactory,
            RestletBodyAuditor<?> requestDetailCreator, RestletBodyAuditor<?> responseDetailCreator) {
        super(Component.OAUTH, restlet, auditEventPublisher, auditEventFactory, requestFactory, requestDetailCreator,
                responseDetailCreator);
    }
}
