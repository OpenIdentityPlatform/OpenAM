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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.forgerock.json.resource.VersionSelector;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.DefaultVersionBehaviour;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.router.VersionedRouter;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

/**
 * A router for Restlet service endpoints which allows for new routes to be attached for particular URI templates and
 * versions of an endpoint.
 *
 * @since 12.0.0
 */
public class ServiceRouter extends Restlet implements VersionedRouter<ServiceRouter> {

    private final RestletRealmRouter router;
    private final VersionSelector versionSelector;
    private final Set<String> routes = new CopyOnWriteArraySet<String>();
    private final Set<VersionRouter> routers = new HashSet<VersionRouter>();
    private DefaultVersionBehaviour defaultVersioningBehaviour = DefaultVersionBehaviour.LATEST;

    /**
     * Constructs a new ServerRouter.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param versionSelector An instance of the VersionSelector.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    public ServiceRouter(RestRealmValidator realmValidator, VersionSelector versionSelector, CoreWrapper coreWrapper) {
        this.router = new RestletRealmRouter(realmValidator, coreWrapper);
        this.versionSelector = versionSelector;
    }

    /**
     * Adds a new versioned route based on the provided URI template.
     *
     * @param uriTemplate The URI template.
     * @return A new {@link VersionRouter} that will allow for adding version routes for the endpoint.
     */
    public VersionRouter addRoute(String uriTemplate) {
        routes.add(uriTemplate);
        VersionRouter versionRouter = new VersionRouter(versionSelector);
        setVersionBehaviour(versionRouter);
        routers.add(versionRouter);
        router.attach(uriTemplate, new RestletWrapper(versionRouter));
        return versionRouter;
    }

    /**
     * Sets the version behaviour for a single router.
     * @param versionRouter The router being set.
     */
    private void setVersionBehaviour(VersionRouter versionRouter) {
        switch (defaultVersioningBehaviour) {
            case LATEST: {
                versionRouter.defaultToLatest();
                break;
            }
            case OLDEST: {
                versionRouter.defaultToOldest();
                break;
            }
            default: {
                versionRouter.noDefault();
            }
        }
    }

    /**
     * Adds a new unversioned route based on the provided URI template.
     *
     * @param uriTemplate The URI template.
     * @param endpoint The endpoint.
     * @return This router.
     */
    public ServiceRouter addRoute(String uriTemplate, Restlet endpoint) {
        routes.add(uriTemplate);
        router.attach(uriTemplate, endpoint);
        return this;
    }

    /**
     * Sets the behaviour of the version routing process when the requested version is {@code null}.
     *
     * @see VersionRouter#defaultToLatest()
     * @see VersionSelector#defaultToLatest()
     */
    public ServiceRouter setVersioning(DefaultVersionBehaviour behaviour) {
        defaultVersioningBehaviour = behaviour;
        for (VersionRouter router : routers) {
            setVersionBehaviour(router);
        }
        return this;
    }

    @Override
    public ServiceRouter setHeaderWarningEnabled(boolean warningEnabled) {
        for (VersionRouter router : routers) {
            router.setHeaderWarning(warningEnabled);
        }

        return this;
    }

    /**
     * Gets the URI templates for all of the attached routes.
     *
     * @return The route URI templates.
     */
    public Set<String> getRoutes() {
        return Collections.unmodifiableSet(routes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(Request request, Response response) {
        router.handle(request, response);
    }

    /**
     * Simple Restlet delegate to call the VersionRouter.
     */
    private static final class RestletWrapper extends Restlet {

        private final VersionRouter router;

        private RestletWrapper(VersionRouter router) {
            this.router = router;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handle(Request request, Response response) {
            router.handle(request, response);
        }
    }

}
