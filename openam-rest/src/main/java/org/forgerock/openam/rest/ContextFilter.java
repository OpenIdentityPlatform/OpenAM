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

import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;

/**
 * A Filter implementation that injects the required OpenAM contexts into the context hierarchy.
 *
 * @since 13.0.0
 */
public class ContextFilter implements Filter {

    private final SSOTokenContext.Factory ssoTokenContextFactory;

    @Inject
    public ContextFilter(SSOTokenContext.Factory ssoTokenContextFactory) {
        this.ssoTokenContextFactory = ssoTokenContextFactory;
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        return next.handleAction(addMissingContexts(context), request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        return next.handleCreate(addMissingContexts(context), request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        return next.handleDelete(addMissingContexts(context), request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        return next.handlePatch(addMissingContexts(context), request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        return next.handleQuery(addMissingContexts(context), request, handler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        return next.handleRead(addMissingContexts(context), request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        return next.handleUpdate(addMissingContexts(context), request);
    }

    private Context addMissingContexts(Context context) {
        context = addSSOTokenContext(context);
        return addLocaleContext(context);
    }

    private Context addSSOTokenContext(Context context) {
        if (!context.containsContext(SSOTokenContext.class)) {
            return ssoTokenContextFactory.create(context);
        } else {
            return context;
        }
    }

    private Context addLocaleContext(Context context) {
        if (!context.containsContext(LocaleContext.class)) {
            return new LocaleContext(context);
        } else {
            return context;
        }
    }
}
