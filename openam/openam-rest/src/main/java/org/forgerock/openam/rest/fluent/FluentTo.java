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

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.authz.filter.crest.AuthorizationFilters;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.SingletonResourceProvider;

/**
 * Used to chain
 */
public final class FluentTo {

    private final FluentRoute route;
    private final String version;

    FluentTo(FluentRoute route, String version) {
        this.route = route;
        this.version = version;
    }

    /**
     * Method to route to a specific {@link CollectionResourceProvider} for the route and
     * version represented by this class.
     *
     * @param providerClass the resource provider's class (endpoint).
     * @return a FluentRoute to the provider
     */
    public FluentVersion to(Class<? extends CollectionResourceProvider> providerClass) {
        return to(providerClass, null);
    }

    /**
     * Method to route to a specific {@link CollectionResourceProvider} for the route and
     * version represented by this class.
     *
     * @param providerClass the resource provider's class (endpoint).
     * @param named the specific name of the resource provider's impl.
     * @return a FluentRoute to the provider
     */
    public FluentVersion to(Class<? extends CollectionResourceProvider> providerClass, String named) {

        CollectionResourceProvider provider = get(providerClass, named);

        if (route.getModules().length > 0) {
            route.addVersion(RoutingMode.STARTS_WITH, version,
                    AuthorizationFilters.createFilter(provider, route.getModules()));
        } else {
            route.addVersion(version, provider);
        }

        return route;
    }

    /**
     * Method to route to a specific {@link SingletonResourceProvider} for the route and version
     * represented by this class.
     *
     * @param provider the resource provider (endpoint).
     * @return a FluentRoute to the provider
     */
    public FluentVersion to(SingletonResourceProvider provider) {
        if (route.getModules().length > 0) {
            route.addVersion(RoutingMode.EQUALS, version,
                    AuthorizationFilters.createFilter(provider, route.getModules()));
        } else {
            route.addVersion(version, provider);
        }

        return route;
    }

    /**
     * Method to route to a specific {@link RequestHandler} with the specified routingMode for the route and version
     * represented by this class.
     *
     * @param routingMode The RoutingMode
     * @param requestHandler The RequestHandler - the targeted endpoint
     * @return a FluentRoute to the provider
     */
    public FluentVersion to(RoutingMode routingMode, RequestHandler requestHandler) {
        if (route.getModules().length > 0) {
            route.addVersion(routingMode, version,
                    AuthorizationFilters.createFilter(requestHandler, route.getModules()));
        } else {
            route.addVersion(routingMode, version, requestHandler);
        }

        return route;

    }

    private <T> T get(Class<T> resourceClass, String named) {
        if (named == null) {
            return InjectorHolder.getInstance(resourceClass);
        } else {
            return InjectorHolder.getInstance(Key.get(resourceClass, Names.named(named)));
        }
    }

}
