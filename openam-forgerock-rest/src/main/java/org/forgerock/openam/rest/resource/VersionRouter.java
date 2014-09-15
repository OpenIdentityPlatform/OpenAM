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
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.VersionHandler;
import org.forgerock.openam.rest.DefaultVersionBehaviour;
import org.forgerock.openam.rest.router.VersionedRouter;

/**
 * Non-instantiable CREST-versioned router, used as a base for routers which require versioned functionality while
 * also augmenting their actions.
 *
 * Implementing classes should choose to override:
 *
 * <ul>
 *     <li>{@link VersionRouter#getRoutes()}</li>
 *     <li>{@link VersionRouter#transformContext(org.forgerock.json.resource.ServerContext)} ()}</li>
 * </ul>
 *
 * @since 12.0.0
 */
public class VersionRouter<T extends VersionedRouter<T>>
        implements RequestHandler, VersionedRouter<VersionRouter<T>> {

    protected final org.forgerock.json.resource.VersionRouter router;
    protected final Set<String> routes = new CopyOnWriteArraySet<String>();

    public VersionRouter() {
        this.router = new org.forgerock.json.resource.VersionRouter();
    }

    /**
     * Initiates the creation of a new route, to a versioned resource, on this router for the provided URI template.
     * The route is not actually added to the router until a specific version and request handler has been specified by
     * calling #addVersion on the return {@code VersionHandler}.
     *
     * @param uriTemplate The URI template which request resource names must match.
     * @return An {@code VersionHandler} instance to add resource version routes on.
     */
    public VersionHandler addRoute(String uriTemplate) {
        routes.add(uriTemplate);
        return router.addRoute(uriTemplate);
    }

    /**
     * <p>Initiates the creation of a new route, to a versioned resource, on this router for the provided URI template.
     * The route is not actually added to the router until a specific version and request handler has been specified by
     * calling #addVersion on the return {@code VersionHandler}.</p>
     *
     * <p>Use this method when adding routes to {@link RequestHandler}s. To add routes to
     * {@link CollectionResourceProvider}s and {@link SingletonResourceProvider}s use the
     * {@link #addRoute(String)} method.</p>
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @return An {@code VersionHandler} instance to add resource version routes on.
     */
    public VersionHandler addRoute(RoutingMode mode, String uriTemplate) {
        routes.add(uriTemplate);
        return router.addRoute(mode, uriTemplate);
    }

    /**
     * Adds a new route to this router for the provided collection resource provider. New routes may be added while
     * this router is processing requests.
     *
     * @param uriTemplate The URI template which request resource names must match.
     * @param provider The collection resource provider to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public Route addRoute(String uriTemplate, CollectionResourceProvider provider) {
        routes.add(uriTemplate);
        return router.addRoute(uriTemplate, provider);
    }

    /**
     * Adds a new route to this router for the provided singleton resource provider. New routes may be added while this
     * router is processing requests.
     *
     * @param uriTemplate The URI template which request resource names must match.
     * @param provider The singleton resource provider to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public Route addRoute(String uriTemplate, SingletonResourceProvider provider) {
        routes.add(uriTemplate);
        return router.addRoute(uriTemplate, provider);
    }

    /**
     * Adds a new route to this router for the provided request handler. New routes may be added while this router is
     * processing requests.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param handler The request handler to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later.
     */
    public Route addRoute(RoutingMode mode, String uriTemplate, RequestHandler handler) {
        routes.add(uriTemplate);
        return router.addRoute(mode, uriTemplate, handler);
    }

    /**
     * Handles an action request by passing it through to the internal router.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            router.handleAction(transformContext(context), request, handler);
        } catch (BadRequestException error) {
            handler.handleError(error);
        }
    }

    /**
     * Handles a create request by passing it through to the internal router.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleCreate(transformContext(context), request, handler);
        } catch (BadRequestException error) {
            handler.handleError(error);
        }
    }

    /**
     * Handles a delete request by passing it through to the internal router.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleDelete(transformContext(context), request, handler);
        } catch (BadRequestException error) {
            handler.handleError(error);
        }
    }

    /**
     * Handles a patch request by passing it through to the internal router.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        try {
            router.handlePatch(transformContext(context), request, handler);
        } catch (BadRequestException error) {
            handler.handleError(error);
        }
    }

    /**
     * Handles a query request by passing it through to the internal router.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            router.handleQuery(transformContext(context), request, handler);
        } catch (BadRequestException error) {
            handler.handleError(error);
        }
    }

    /**
     * Handles a read request by passing it through to the internal router.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleRead(transformContext(context), request, handler);
        } catch (BadRequestException error) {
            handler.handleError(error);
        }
    }

    /**
     * Handles an update request by passing it through to the internal router.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleUpdate(transformContext(context), request, handler);
        } catch (BadRequestException error) {
            handler.handleError(error);
        }
    }

    /**
     * Default implementation performs no transformation.
     *
     * @param context The context. Cannot be null.
     * @return The augmented context.
     * @throws BadRequestException Not thrown by this impl.
     */
    protected ServerContext transformContext(ServerContext context) throws BadRequestException {
        return context;
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
     *
     * Instructs the internal {@link org.forgerock.openam.rest.resource.CrestRealmRouter} to perform the same.
     */
    @Override
    public VersionRouter<T> setVersioning(DefaultVersionBehaviour behaviour) {
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

        return this;
    }

    public VersionRouter<T> setHeaderWarningEnabled(boolean warningEnabled) {
        router.setWarningEnabled(warningEnabled);
        return this;
    }

    public VersionRouter<T> setDefaultRoute(RequestHandler handler) {
        router.setDefaultRoute(handler);

        return this;
    }
}
