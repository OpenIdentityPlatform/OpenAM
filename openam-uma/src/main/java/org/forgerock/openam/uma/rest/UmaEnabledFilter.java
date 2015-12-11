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

package org.forgerock.openam.uma.rest;

import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.uma.UmaProviderSettings;
import org.forgerock.openam.uma.UmaProviderSettingsFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;

/**
 * Checks that an UMA Provider has been configured for the current realm, and returns not supported if
 * it is not.
 */
public class UmaEnabledFilter implements Filter {

    private final UmaProviderSettingsFactory umaProviderSettingsFactory;

    @Inject
    public UmaEnabledFilter(UmaProviderSettingsFactory umaProviderSettingsFactory) {
        this.umaProviderSettingsFactory = umaProviderSettingsFactory;
    }

    private Promise<Void, ResourceException> enabled(Context serverContext) {
        try {
            String realm = RealmContext.getRealm(serverContext);
            UmaProviderSettings settings = umaProviderSettingsFactory.get(realm);
            if (settings.isEnabled()) {
                return newResultPromise(null);
            }
        } catch (NotFoundException ignore) { }
        return new NotSupportedException("UMA is not currently supported in this realm").asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(final Context serverContext,
            final ActionRequest request, final RequestHandler requestHandler) {
        return enabled(serverContext)
                .thenAsync(new AsyncFunction<Void, ActionResponse, ResourceException>() {
                    @Override
                    public Promise<ActionResponse, ResourceException> apply(Void value) {
                        return requestHandler.handleAction(serverContext, request);
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(final Context serverContext,
            final CreateRequest request, final RequestHandler requestHandler) {
        return enabled(serverContext)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) {
                        return requestHandler.handleCreate(serverContext, request);
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(final Context serverContext,
            final DeleteRequest request, final RequestHandler requestHandler) {
        return enabled(serverContext)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) {
                        return requestHandler.handleDelete(serverContext, request);
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(final Context serverContext,
            final PatchRequest request, final RequestHandler requestHandler) {
        return enabled(serverContext)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) {
                        return requestHandler.handlePatch(serverContext, request);
                    }
                });
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(final Context serverContext,
            final QueryRequest request, final QueryResourceHandler queryResultHandler,
            final RequestHandler requestHandler) {
        return enabled(serverContext)
                .thenAsync(new AsyncFunction<Void, QueryResponse, ResourceException>() {
                    @Override
                    public Promise<QueryResponse, ResourceException> apply(Void value) {
                        return requestHandler.handleQuery(serverContext, request, queryResultHandler);
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(final Context serverContext,
            final ReadRequest request, final RequestHandler requestHandler) {
        return enabled(serverContext)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) {
                        return requestHandler.handleRead(serverContext, request);
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(final Context serverContext,
            final UpdateRequest request, final RequestHandler requestHandler) {
        return enabled(serverContext)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) {
                        return requestHandler.handleUpdate(serverContext, request);
                    }
                });
    }
}
