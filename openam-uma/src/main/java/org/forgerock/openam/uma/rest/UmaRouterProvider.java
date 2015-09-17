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

package org.forgerock.openam.uma.rest;

import static org.forgerock.openam.audit.AuditConstants.Component.UMA;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;
import static org.forgerock.openam.uma.UmaConstants.AUTHORIZATION_REQUEST_ENDPOINT;
import static org.forgerock.openam.uma.UmaConstants.PERMISSION_REQUEST_ENDPOINT;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.audit.RestletAccessAuditFilterFactory;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.uma.UmaExceptionFilter;
import org.forgerock.openam.uma.UmaWellKnownConfigurationEndpoint;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Guice Provider from getting the UMA HTTP router.
 *
 * @since 13.0.0
 */
public class UmaRouterProvider implements Provider<Router> {

    private final RestRealmValidator realmValidator;
    private final CoreWrapper coreWrapper;
    private final RestletAccessAuditFilterFactory restletAuditFactory;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param coreWrapper An instance of the CoreWrapper.
     * @param restletAuditFactory An instance of the RestletAccessAuditFilterFactory.
     */
    @Inject
    public UmaRouterProvider(RestRealmValidator realmValidator, CoreWrapper coreWrapper,
            RestletAccessAuditFilterFactory restletAuditFactory) {
        this.realmValidator = realmValidator;
        this.coreWrapper = coreWrapper;
        this.restletAuditFactory = restletAuditFactory;
    }

    @Override
    public Router get() {
        Router router = new RestletRealmRouter(realmValidator, coreWrapper);
        router.attach("/permission_request", getRestlet(PERMISSION_REQUEST_ENDPOINT));
        router.attach("/authz_request", getRestlet(AUTHORIZATION_REQUEST_ENDPOINT));
        // Well-Known Discovery
        router.attach("/.well-known/uma-configuration",
                new UmaExceptionFilter(wrap(UmaWellKnownConfigurationEndpoint.class)));
//        router.attach("/permission_request",
//                restletAuditFactory.createFilter(UMA, getRestlet(PERMISSION_REQUEST_ENDPOINT)));
//        router.attach("/authz_request",
//                restletAuditFactory.createFilter(UMA, getRestlet(AUTHORIZATION_REQUEST_ENDPOINT)));
//        // Well-Known Discovery
//        router.attach("/.well-known/uma-configuration",
//                restletAuditFactory.createFilter(UMA,
//                        new UmaExceptionFilter(wrap(UmaWellKnownConfigurationEndpoint.class))));
        return router;
    }

    private Restlet getRestlet(String name) {
        return InjectorHolder.getInstance(Key.get(Restlet.class, Names.named(name)));
    }
}
