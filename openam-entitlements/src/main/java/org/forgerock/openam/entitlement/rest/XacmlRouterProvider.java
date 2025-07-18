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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.util.Set;

import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.openam.rest.service.ResourceApiVersionRestlet;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.xacml.v3.rest.XacmlService;
import org.restlet.routing.Router;

/**
 * Guice provider providing the Xacml REST Router.
 *
 * @since 13.0.0
 */
public class XacmlRouterProvider implements Provider<Router> {

    private final ResourceApiVersionBehaviourManager versionBehaviourManager;
    private final Set<String> invalidRealmNames;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param versionBehaviourManager The ResourceApiVersionBehaviourManager.
     */
    @Inject
    public XacmlRouterProvider(ResourceApiVersionBehaviourManager versionBehaviourManager,
            @Named("InvalidRealmNames") Set<String> invalidRealms) {
        this.versionBehaviourManager = versionBehaviourManager;
        this.invalidRealmNames = invalidRealms;
    }

    @Override
    public Router get() {
        RestletRealmRouter router = new RestletRealmRouter();

        ResourceApiVersionRestlet policiesVersionRouter = new ResourceApiVersionRestlet(versionBehaviourManager);
        policiesVersionRouter.attach(version(1), wrap(XacmlService.class));
        router.attach("/policies", policiesVersionRouter);
        invalidRealmNames.add("policies");

        return router;
    }
}
