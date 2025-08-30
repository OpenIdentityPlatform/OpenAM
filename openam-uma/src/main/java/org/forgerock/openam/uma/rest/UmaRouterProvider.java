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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.uma.rest;

import static org.forgerock.openam.rest.audit.RestletBodyAuditor.jsonAuditor;
import static org.forgerock.openam.rest.audit.RestletBodyAuditor.noBodyAuditor;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;
import static org.forgerock.openam.uma.UmaConstants.*;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.rest.audit.RestletBodyAuditor;
import org.forgerock.openam.rest.audit.UMAAccessAuditFilter;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.uma.UmaWellKnownConfigurationEndpoint;
import org.restlet.Restlet;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;

/**
 * Guice Provider from getting the UMA HTTP router.
 *
 * @since 13.0.0
 */
public class UmaRouterProvider implements Provider<Router> {

    private final AuditEventPublisher eventPublisher;
    private final AuditEventFactory eventFactory;
    private final OAuth2RequestFactory requestFactory;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     * @param requestFactory The factory that provides access to OAuth2Request.
     */
    @Inject
    public UmaRouterProvider(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory,
            OAuth2RequestFactory requestFactory) {
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
        this.requestFactory = requestFactory;
    }

    @Override
    public Router get() {
        Router router = new RestletRealmRouter();
        router.attach("/permission_request", auditWithUmaFilter(getRestlet(PERMISSION_REQUEST_ENDPOINT),
                jsonAuditor(RESOURCE_SET_ID, SCOPES), noBodyAuditor()));
        router.attach("/authz_request", auditWithUmaFilter(getRestlet(AUTHORIZATION_REQUEST_ENDPOINT),
                noBodyAuditor(), noBodyAuditor()));
        // Well-Known Discovery
        router.attach("/.well-known/uma-configuration", auditWithUmaFilter(
                        wrap(UmaWellKnownConfigurationEndpoint.class), noBodyAuditor(), noBodyAuditor()));
        return router;
    }

    private Restlet getRestlet(String name) {
        return InjectorHolder.getInstance(Key.get(Restlet.class, Names.named(name)));
    }

    private Filter auditWithUmaFilter(Restlet restlet, RestletBodyAuditor<?> requestDetailCreator,
            RestletBodyAuditor<?> responseDetailCreator) {
        return new UMAAccessAuditFilter(restlet, eventPublisher, eventFactory, requestFactory, requestDetailCreator,
                responseDetailCreator);
    }
}
