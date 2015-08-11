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

import static org.forgerock.openam.audit.AuditConstants.Component;

import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.forgerockrest.authn.AuthIdHelper;
import org.forgerock.openam.forgerockrest.authn.AuthenticationAccessAuditFilter;
import org.restlet.Restlet;
import org.restlet.routing.Filter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Factory to assist with the creation of audit filters for restlet access.
 *
 * @since 13.0.0
 */
@Singleton
public final class RestletAccessAuditFilterFactory {

    private final AuthIdHelper authIdHelper;
    private final AuditEventPublisher eventPublisher;
    private final AuditEventFactory eventFactory;

    /**
     * Guice injected constructor for creating a <code>RestletAccessAuditFilterFactory</code> instance.
     *
     * @param authIdHelper The helper to use for reading authentication JWTs.
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     */
    @Inject
    public RestletAccessAuditFilterFactory(AuthIdHelper authIdHelper, AuditEventPublisher eventPublisher,
            AuditEventFactory eventFactory) {
        this.authIdHelper = authIdHelper;
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
    }

    /**
     * Create a new {@link Filter} for the given restlet and component for auditing access to the restlet.
     *
     * @param component The component represented by the restlet.
     * @param restlet The restlet for which auditing will be done.
     * @return an instance of {@link Filter}
     */
    public Filter createFilter(Component component, Restlet restlet) {
        switch (component) {
            case AUTHENTICATION:
                return new AuthenticationAccessAuditFilter(restlet, authIdHelper, eventPublisher, eventFactory);
        }

        throw new IllegalArgumentException("Filter for " + component + " does not exist.");
    }

}
