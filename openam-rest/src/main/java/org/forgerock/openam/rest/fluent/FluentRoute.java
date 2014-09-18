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
package org.forgerock.openam.rest.fluent;

import java.util.ArrayList;
import java.util.List;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.VersionHandler;
import org.forgerock.openam.rest.authz.LoggingAuthzModule;
import org.forgerock.openam.rest.resource.CrestRouter;

/**
 * Class representing a Route which can be added to a FluentRouter. The FluentRouter itself is
 * responsible for creating the FluentRoute (making the API fluent), and the calling method for
 * adding these FluentRoutes to the FluentRouter adding any necessary filters/versions, etc.
 *
 * @see org.forgerock.openam.rest.RestEndpoints
 * @since 12.0.0
 */
public class FluentRoute implements FluentVersion {

    private final CrestRouter router;
    private final String uriTemplate;
    private final List<CrestAuthorizationModule> modules;
    private VersionHandler versionHandler;

    FluentRoute(CrestRouter router, String uriTemplate) {
        this.router = router;
        this.uriTemplate = uriTemplate;
        this.modules = new ArrayList<CrestAuthorizationModule>();
    }

    FluentRoute(FluentRoute route) {
        this.router = route.router;
        this.uriTemplate = route.uriTemplate;
        this.modules = route.modules;
    }
    /**
     * Add an authorization module through which the request must pass
     * if it is to reach its destination.
     *
     * @param module authorization filter for the request.
     * @param moduleReference the name of the authorization filter, referenced in logs.
     * @return this FluentRoute (fluent API).
     */
    public FluentRoute through(Class<? extends CrestAuthorizationModule> module, String moduleReference) {
        this.modules.add(new LoggingAuthzModule(get(module), moduleReference));
        return this;
    }

    /**
     * Sets the version this Route is for.
     *
     * @param version the version for this route.
     * @return a FluentTo connecting this route with this version.
     */
    public FluentTo forVersion(String version) {
        return new FluentTo(this, version);
    }

    /**
     * Return the authorization modules which must be passed for a request to be successfully routed to
     * its resource provider.
     *
     * @return An array of CrestAuthorizationModules
     */
    CrestAuthorizationModule[] getModules() {
        return modules.toArray(new CrestAuthorizationModule[modules.size()]);
    }

    /**
     * Adds a route to a version of the resource pointed at by this FluentRoute, which is
     * provided by a CollectionResourceProvider.
     *
     * @param version The version of the resource pointed to.
     * @param provider The provider of the resource.
     */
    void addVersion(String version, CollectionResourceProvider provider) {
        if (versionHandler == null) {
            versionHandler = router.addRoute(uriTemplate);
        }
        versionHandler.addVersion(version, provider);
    }

    /**
     * Adds a route to a version of the resource pointed at by this FluentRoute, which is
     * provided by a SingletonResourceProvider.
     *
     * @param version The version of the resource pointed to.
     * @param provider The provider of the resource.
     */
    void addVersion(String version, SingletonResourceProvider provider) {
        if (versionHandler == null) {
            versionHandler = router.addRoute(uriTemplate);
        }
        versionHandler.addVersion(version, provider);
    }

    /**
     * Adds a route to a version of the resource pointed at by this FluentRoute, which is
     * provided by a RequestHandler.
     *
     * @param version The version of the resource pointed to.
     * @param handler The provider of the resource.
     */
    void addVersion(RoutingMode mode, String version, RequestHandler handler) {
        if (versionHandler == null) {
            versionHandler = router.addRoute(mode, uriTemplate);
        }
        versionHandler.addVersion(version, handler);
    }

    private <T> T get(Class<T> resourceClass) {
        return InjectorHolder.getInstance(resourceClass);
    }

}
