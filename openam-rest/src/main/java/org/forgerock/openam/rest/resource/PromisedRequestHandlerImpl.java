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

package org.forgerock.openam.rest.resource;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;

/**
 * An implementation of the {@code PromisedRequestHandler} that wraps a {@code RequestHandler}.
 *
 * @since 13.0.0
 */
public class PromisedRequestHandlerImpl implements PromisedRequestHandler {

    private final RequestHandler handler;

    /**
     * Creates a new {@code PromisedRequestHandlerImpl} wrapping the provided {@code RequestHandler}.
     *
     * @param handler The {@code RequestHandler} instance.
     */
    public PromisedRequestHandlerImpl(RequestHandler handler) {
        this.handler = handler;
    }

    private <T> ResultHandler<T> newHandler(final PromiseImpl<T, ResourceException> promise) {
        return new ResultHandler<T>() {
            @Override
            public void handleError(ResourceException error) {
                promise.handleError(error);
            }

            @Override
            public void handleResult(T result) {
                promise.handleResult(result);
            }
        };
    }

    private QueryResultHandler newQueryHandler(
            final PromiseImpl<Pair<QueryResult, List<Resource>>, ResourceException> promise) {
        return new QueryResultHandler() {
            private final List<Resource> resources = new ArrayList<Resource>();
            @Override
            public void handleError(ResourceException error) {
                promise.handleError(error);
            }

            @Override
            public boolean handleResource(Resource resource) {
                resources.add(resource);
                return true;
            }

            @Override
            public void handleResult(QueryResult result) {
                promise.handleResult(Pair.of(result, resources));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<JsonValue, ResourceException> handleAction(ServerContext context, ActionRequest request) {
        PromiseImpl<JsonValue, ResourceException> promise = PromiseImpl.create();
        handler.handleAction(context, request, newHandler(promise));
        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> handleCreate(ServerContext context, CreateRequest request) {
        PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        handler.handleCreate(context, request, newHandler(promise));
        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> handleDelete(ServerContext context, DeleteRequest request) {
        PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        handler.handleDelete(context, request, newHandler(promise));
        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> handlePatch(ServerContext context, PatchRequest request) {
        PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        handler.handlePatch(context, request, newHandler(promise));
        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Pair<QueryResult, List<Resource>>, ResourceException> handleQuery(ServerContext context,
            QueryRequest request) {
        PromiseImpl<Pair<QueryResult, List<Resource>>, ResourceException> promise = PromiseImpl.create();
        handler.handleQuery(context, request, newQueryHandler(promise));
        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> handleRead(ServerContext context, ReadRequest request) {
        PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        handler.handleRead(context, request, newHandler(promise));
        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> handleUpdate(ServerContext context, UpdateRequest request) {
        PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        handler.handleUpdate(context, request, newHandler(promise));
        return promise;
    }
}
