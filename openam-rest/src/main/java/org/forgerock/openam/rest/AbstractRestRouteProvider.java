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

package org.forgerock.openam.rest;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * An abstract implementation of the {@link RestRouteProvider} that provides
 * methods for adding resource (CREST) endpoints and service (CHF REST)
 * endpoints on either the root router or realm router.
 *
 * @since 13.0.0
 */
public abstract class AbstractRestRouteProvider implements RestRouteProvider {

    private ResourceRouter rootResourceRouter;
    private ResourceRouter realmResourceRouter;
    private ServiceRouter rootServiceRouter;
    private ServiceRouter realmServiceRouter;

    @Override
    public final void addRoutes(ResourceRouter rootResourceRouter, ResourceRouter realmResourceRouter,
                                ResourceRouter internalRouter, ServiceRouter rootServiceRouter,
                                ServiceRouter realmServiceRouter) {
        addResourceRoutes(rootResourceRouter, realmResourceRouter);
        addServiceRoutes(rootServiceRouter, realmServiceRouter);
        addInternalRoutes(internalRouter);
    }

    /**
     * Add resource (CREST) route registrations to the provided routers.
     *
     * @param rootRouter The root CREST router.
     * @param realmRouter The realm CREST router.
     */
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {

    }

    /**
     * Add service (CHF REST) route registrations to the provided routers
     *
     * @param rootRouter The root CHF REST router.
     * @param realmRouter The realm CHF REST router.
     */
    public void addServiceRoutes(ServiceRouter rootRouter, ServiceRouter realmRouter) {

    }

    /**
     * Add a CREST resource route to the internal router.
     *
     * @param internalRouter
     *         the internal router
     */
    public void addInternalRoutes(ResourceRouter internalRouter) {
    }

    protected ResourceRouter getRootResourceRouter() {
        return rootResourceRouter;
    }

    protected ResourceRouter getRealmResourceRouter() {
        return realmResourceRouter;
    }

    protected ServiceRouter getRootServiceRouter() {
        return rootServiceRouter;
    }

    protected ServiceRouter getRealmServiceRouter() {
        return realmServiceRouter;
    }

    @Inject
    void setResourceRootRouter(@Named("RootResourceRouter") ResourceRouter rootRouter) {
        this.rootResourceRouter = rootRouter;
    }

    @Inject
    void setResourceRealmRouter(@Named("RealmResourceRouter") ResourceRouter realmRouter) {
        this.realmResourceRouter = realmRouter;
    }

    @Inject
    void setServiceRootRouter(@Named("RootServiceRouter") ServiceRouter rootRouter) {
        this.rootServiceRouter = rootRouter;
    }

    @Inject
    void setServiceRealmRouter(@Named("RealmServiceRouter") ServiceRouter realmRouter) {
        this.realmServiceRouter = realmRouter;
    }
}
