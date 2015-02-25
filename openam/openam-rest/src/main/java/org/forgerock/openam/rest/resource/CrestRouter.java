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
package org.forgerock.openam.rest.resource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.VersionHandler;
import org.forgerock.json.resource.VersionRouter;
import org.forgerock.openam.rest.DefaultVersionBehaviour;
import org.forgerock.openam.rest.router.VersionedRouter;

/**
 * Version-aware implementation of a RequestHandler, designed for routing to CREST resources.
 *
 * Requests which pass through routes handled by this router must have an SSOTokenContext added to their
 * ServerContext.
 *
 * @param <T> Type of this router.
 */
public class CrestRouter<T extends CrestRouter> implements RequestHandler, VersionedRouter<T> {

    private final VersionRouter router = new VersionRouter();
    private final Set<String> routes = new CopyOnWriteArraySet<String>();
    private Set<String> defaultRoutes;

    protected VersionRouter getRouter() {
        return router;
    }

    /**
     * Handles performing an action on a resource, and optionally returns an
     * associated result. The context is first transformed such that it always contains an
     * SSOTokenContext.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            router.handleAction(transformContext(context), request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Handles performing a create on a resource, and optionally returns an
     * associated result. The context is first transformed such that it always contains an
     * SSOTokenContext.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleCreate(transformContext(context), request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Handles performing a delete on a resource, and optionally returns an
     * associated result. The context is first transformed such that it always contains an
     * SSOTokenContext.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleDelete(transformContext(context), request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Handles performing a patch on a resource, and optionally returns an
     * associated result. The context is first transformed such that it always contains an
     * SSOTokenContext.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        try {
            router.handlePatch(transformContext(context), request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Handles performing a query on a resource, and optionally returns an
     * associated result. The context is first transformed such that it always contains an
     * SSOTokenContext.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            router.handleQuery(transformContext(context), request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Handles performing a read on a resource, and optionally returns an
     * associated result. The context is first transformed such that it always contains an
     * SSOTokenContext.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleRead(transformContext(context), request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Handles performing an update on a resource, and optionally returns an
     * associated result. The context is first transformed such that it always contains an
     * SSOTokenContext.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleUpdate(transformContext(context), request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Adds a route to this Router by passing the endpoint location of the resource provider.
     *
     * @param uriTemplate Endpoint location of the resource provider.
     * @return this router's internal router.
     */
    public VersionHandler addRoute(String uriTemplate) {
        routes.add(uriTemplate);
        return router.addRoute(uriTemplate);
    }

    /**
     * Adds a route to this Router by passing the endpoint location of the resource provider.
     *
     * @param mode which RoutingMode to use for the added route.
     * @param uriTemplate Endpoint location of the resource provider.
     * @return this router's internal .
     */
    public VersionHandler addRoute(RoutingMode mode, String uriTemplate) {
        routes.add(uriTemplate);
        return router.addRoute(mode, uriTemplate);
    }

    /**
     * Adds a route to this Router by passing the endpoint location and the resource provider.
     *
     * @param uriTemplate Endpoint location of the resource provider.
     * @param provider the resource provider.
     * @return this router's internal router.
     */
    public Route addRoute(String uriTemplate, SingletonResourceProvider provider) {
        routes.add(uriTemplate);
        return router.addRoute(uriTemplate, provider);
    }

    /**
     * Adds a route to this Router by passing the endpoint location and the resource provider.
     *
     * @param uriTemplate Endpoint location of the resource provider.
     * @param provider the resource provider.
     * @return this router's internal router.
     */
    public Route addRoute(String uriTemplate, CollectionResourceProvider provider) {
        routes.add(uriTemplate);
        return router.addRoute(uriTemplate, provider);
    }

    /**
     * Adds a route to this Router by passing the endpoint location of the resource provider.
     *
     * @param mode which RoutingMode to use for the added route.
     * @param uriTemplate Endpoint location of the resource provider.
     * @param handler the resource provider.
     * @return this router's internal router.
     */
    public Route addRoute(RoutingMode mode, String uriTemplate, RequestHandler handler) {
        routes.add(uriTemplate);
        return router.addRoute(mode, uriTemplate, handler);
    }

    /**
     * Returns all routes mapped by this router and this router's default router.
     *
     * @return an unmodifiableSet of all the routes pointed to by this router.
     */
    public Set<String> getRoutes() {
        HashSet<String> allRoutes = new HashSet<String>(routes);
        allRoutes.addAll(defaultRoutes);
        return Collections.unmodifiableSet(allRoutes);
    }

    /**
     * Sets the default router for this router - requests which aren't handled by this router
     * will fall through to the default router.
     *
     * @param handler A router to handle requests this router cannot.
     * @return This router.
     */
    public CrestRouter setDefaultRoute(CrestRouter handler) {
        router.setDefaultRoute(handler);
        defaultRoutes = handler.routes;
        return this;
    }

    /**
     * @return The default router for this router.
     */
    public RequestHandler getDefaultRoute() {
        return router.getDefaultRoute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T setVersioning(DefaultVersionBehaviour behaviour) {
        switch (behaviour) {
            case LATEST:
                router.setVersioningToDefaultToLatest();
                break;
            case OLDEST:
                router.setVersioningToDefaultToOldest();
                break;
            case NONE:
                router.setVersioningBehaviourToNone();
                break;
        }
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T setHeaderWarningEnabled(boolean warningEnabled) {
        router.setWarningEnabled(warningEnabled);
        return (T) this;
    }

    /**
     * <p>Creates the {@link SSOTokenContext}, if one doesn't already exist. </p>
     *
     * @param context The context.
     * @return The augmented context.
     * @throws ResourceException If the current full realm is not a valid realm.
     */
    protected ServerContext transformContext(ServerContext context) throws ResourceException {
        if (!context.containsContext(SSOTokenContext.class)) {
            return new SSOTokenContext(context);
        } else {
            return context;
        }
    }
}