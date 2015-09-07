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

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Set;

import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
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

    private final RestRealmValidator realmValidator;
    private final ResourceApiVersionBehaviourManager versionBehaviourManager;
    private final CoreWrapper coreWrapper;
    private final Set<String> invalidRealmNames;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param coreWrapper An instance of the CoreWrapper.
     * @param versionBehaviourManager The ResourceApiVersionBehaviourManager.
     */
    @Inject
    public XacmlRouterProvider(RestRealmValidator realmValidator, CoreWrapper coreWrapper,
            ResourceApiVersionBehaviourManager versionBehaviourManager,
            @Named("InvalidRealmNames") Set<String> invalidRealms) {
        this.realmValidator = realmValidator;
        this.versionBehaviourManager = versionBehaviourManager;
        this.coreWrapper = coreWrapper;
        this.invalidRealmNames = invalidRealms;
    }

    @Override
    public Router get() {
        RestletRealmRouter router = new RestletRealmRouter(realmValidator, coreWrapper);

        ResourceApiVersionRestlet policiesVersionRouter = new ResourceApiVersionRestlet(versionBehaviourManager);
        policiesVersionRouter.attach(version(1), wrap(XacmlService.class));
        router.attach("/policies", policiesVersionRouter);
        invalidRealmNames.add("policies");

        return router;
    }
}
