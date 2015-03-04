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
package org.forgerock.openam.rest.fluent;

import static org.apache.commons.lang.ArrayUtils.isNotEmpty;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.authz.filter.crest.AuthorizationFilters;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to chain
 */
public final class FluentTo {

    private final FluentRoute route;
    private final String version;
    private final List<Filter> filters;

    FluentTo(FluentRoute route, String version) {
        this.route = route;
        this.version = version;
        filters = new ArrayList<Filter>();
    }

    /**
     * Method to route to a specific {@link CollectionResourceProvider} for the route and
     * version represented by this class.
     *
     * @param providerClass the resource provider's class (endpoint).
     * @return a FluentRoute to the provider
     */
    public FluentVersion to(Class<? extends CollectionResourceProvider> providerClass) {
        return to(get(providerClass));
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
        return to(get(providerClass, named));
    }

    /**
     * Internal method to route the passed provider.
     *
     * @param provider
     *         the provider instance
     *
     * @return a route to the provider
     */
    private FluentVersion to(CollectionResourceProvider provider) {
        handleFiltersAndRoute(RoutingMode.STARTS_WITH, Resources.newCollection(provider));
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
        handleFiltersAndRoute(RoutingMode.EQUALS, Resources.newSingleton(provider));
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
        handleFiltersAndRoute(routingMode, requestHandler);
        return route;

    }

    /**
     * Adds a filter to be called prior to the request destination.
     *
     * @param filterClass
     *         the filters class
     *
     * @return the linking fluent class
     */
    public FluentTo through(Class<? extends Filter> filterClass) {
        Reject.ifNull(filterClass);
        filters.add(get(filterClass));
        return this;
    }

    /**
     * Adds a filter to be called prior to the request destination.
     *
     * @param filterClass
     *         the filters class
     * @param named
     *         the specific name of the filter
     *
     * @return the linking fluent class
     */
    public FluentTo through(Class<? extends Filter> filterClass, String named) {
        Reject.ifNull(filterClass);
        filters.add(get(filterClass, named));
        return this;
    }

    /**
     * Configures the route and any defined filters.
     *
     * @param routingMode
     *         the routing mode
     * @param requestHandler
     *         the requests target
     */
    private void handleFiltersAndRoute(final RoutingMode routingMode, final RequestHandler requestHandler) {
        RequestHandler targetHandler = requestHandler;

        if (isNotEmpty(filters)) {
            targetHandler = new FilterChain(targetHandler, filters);
        }

        if (isNotEmpty(route.getModules())) {
            targetHandler = AuthorizationFilters.createFilter(targetHandler, route.getModules());
        }

        route.addVersion(routingMode, version, targetHandler);
    }

    private <T> T get(Class<T> objectClass, String named) {
        return InjectorHolder.getInstance(Key.get(objectClass, Names.named(named)));
    }

    private <T> T get(Class<T> objectClass) {
        return InjectorHolder.getInstance(objectClass);
    }

}
