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
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.VersionHandler;
import org.forgerock.json.resource.VersionRouter;
import org.forgerock.openam.rest.router.RestRealmValidator;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;

/**
 * A CREST request handler which will route to resource endpoints, dynamically handling realm URI parameters.
 *
 * @since 12.0.0
 */
public class CrestRealmRouter implements RequestHandler {

    private final RestRealmValidator realmValidator;
    private final VersionRouter router;
    private final Set<String> routes = new CopyOnWriteArraySet<String>();

    /**
     * Constructs a new RealmRouter instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     */
    public CrestRealmRouter(RestRealmValidator realmValidator) {
        this.realmValidator = realmValidator;
        this.router = new VersionRouter();
        router.addRoute(STARTS_WITH, "/{realm}", this);
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
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            router.handleAction(realmContext(context), request, handler);
        } catch (BadRequestException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleCreate(realmContext(context), request, handler);
        } catch (BadRequestException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleDelete(realmContext(context), request, handler);
        } catch (BadRequestException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        try {
            router.handlePatch(realmContext(context), request, handler);
        } catch (BadRequestException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            router.handleQuery(realmContext(context), request, handler);
        } catch (BadRequestException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleRead(realmContext(context), request, handler);
        } catch (BadRequestException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            router.handleUpdate(realmContext(context), request, handler);
        } catch (BadRequestException e) {
            handler.handleError(e);
        }
    }

    /**
     * <p>Creates a or adds to the {@link RealmContext}.</p>
     *
     * <p>If multiple realm URI parameters are present then this method will be called repeatedly and for each call
     * the last realm URI parameter will be appended to the realm on the {@code RealmContext}.</p>
     *
     * @param context The context.
     * @return The augmented context.
     * @throws BadRequestException If the current full realm is not a valid realm.
     */
    private ServerContext realmContext(ServerContext context) throws BadRequestException {

        String realm;
        if (context.containsContext(RouterContext.class)) {
            realm = context.asContext(RouterContext.class).getUriTemplateVariables().get("realm");
        } else {
            realm = "/";
        }

        RealmContext realmContext;
        if (context.containsContext(RealmContext.class)) {
            realmContext = context.asContext(RealmContext.class);
            realmContext.addSubRealm(realm);
        } else {
            realmContext = new RealmContext(new SSOTokenContext(context), realm);
        }

        // Check that the path references an existing realm
        if (!realmValidator.isRealm(realmContext.getRealm())) {
            throw new BadRequestException("Invalid realm, " + realmContext.getRealm());
        }

        return realmContext;
    }
}
