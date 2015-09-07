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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.rest;

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

import com.sun.identity.sm.InvalidRealmNameManager;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.forgerockrest.XacmlService;
import org.forgerock.openam.rest.audit.RestletAccessAuditFilterFactory;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.service.ResourceApiVersionRestlet;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.restlet.Restlet;

/**
 * Singleton class which contains both the routers for CREST resources and Restlet service endpoints.
 *
 * @since 12.0.0
 */
@Singleton
public class RestEndpoints {

    private final RestRealmValidator realmValidator;
    private final ResourceApiVersionBehaviourManager versionBehaviourManager;
    private final CoreWrapper coreWrapper;
    private final Restlet xacmlServiceRouter;
    private final RestletAccessAuditFilterFactory restletAuditFactory;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param coreWrapper An instance of the CoreWrapper.
     * @param restletAuditFactory An instance of the RestletAccessAuditFilterFactory.
     * @param versionBehaviourManager The ResourceApiVersionBehaviourManager.
     */
    @Inject
    public RestEndpoints(RestRealmValidator realmValidator, CoreWrapper coreWrapper,
                         RestletAccessAuditFilterFactory restletAuditFactory,
                         ResourceApiVersionBehaviourManager versionBehaviourManager) {
        this(realmValidator, coreWrapper, restletAuditFactory, versionBehaviourManager,
                InvalidRealmNameManager.getInvalidRealmNames());
    }

    RestEndpoints(RestRealmValidator realmValidator, CoreWrapper coreWrapper,
                  RestletAccessAuditFilterFactory restletAuditFactory,
                  ResourceApiVersionBehaviourManager versionBehaviourManager, Set<String> invalidRealmNames) {
        this.realmValidator = realmValidator;
        this.versionBehaviourManager = versionBehaviourManager;
        this.coreWrapper = coreWrapper;
        this.restletAuditFactory = restletAuditFactory;

        this.xacmlServiceRouter = createXACMLServiceRouter(invalidRealmNames);
    }

    /**
     * Gets the XACML restlet service router.
     * @return The router.
     */
    public Restlet getXACMLServiceRouter() {
        return xacmlServiceRouter;
    }

    /**
     * Constructs a new {@link Restlet} with routes to each of the Restlet service endpoints.
     *
     * @return A {@code ServiceRouter}.
     */
    private Restlet createXACMLServiceRouter(final Set<String> invalidRealmNames) {

        RestletRealmRouter router = new RestletRealmRouter(realmValidator, coreWrapper);

        ResourceApiVersionRestlet policiesVersionRouter = new ResourceApiVersionRestlet(versionBehaviourManager);
        policiesVersionRouter.attach(version(1), wrap(XacmlService.class));
        router.attach("/policies", policiesVersionRouter);
        invalidRealmNames.add("policies");

        return router;
    }
}
