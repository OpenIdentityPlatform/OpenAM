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

import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.forgerockrest.authn.UMAAccessAuditFilter;
import org.restlet.Request;
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

    private final AuditEventPublisher eventPublisher;
    private final AuditEventFactory eventFactory;
    private final TokenStore tokenStore;
    private final OAuth2RequestFactory<Request> requestFactory;

    /**
     * Guice injected constructor for creating a <code>RestletAccessAuditFilterFactory</code> instance.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     * @param tokenStore The helper to use for reading authentication JWTs.
     * @param requestFactory The factory for creating OAuth2Request instances.
     */
    @Inject
    public RestletAccessAuditFilterFactory(AuditEventPublisher eventPublisher,
                                           AuditEventFactory eventFactory, TokenStore tokenStore,
                                           OAuth2RequestFactory<Request> requestFactory) {
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
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
            case UMA:
                return new UMAAccessAuditFilter(restlet, eventPublisher, eventFactory, tokenStore, requestFactory);

        }

        throw new IllegalArgumentException("Filter for " + component + " does not exist.");
    }

}
