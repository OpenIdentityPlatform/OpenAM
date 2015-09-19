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
package org.forgerock.openam.rest.fluent;

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
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.rest.resource.AuditInfoContext;
import org.forgerock.util.promise.Promise;

/**
 * This filter wrapper adds the audit context to the passed server context and delegates on the request.
 *
 * @since 13.0.0
 */
public final class AuditFilterWrapper implements Filter {

    private final Filter delegate;
    private final Component component;

    /**
     *
     * @param delegate The filter wrapped by this object.
     * @param component The component of the CREST endpoint this filter sits in front of.
     */
    public AuditFilterWrapper(Filter delegate, Component component) {
        this.delegate = delegate;
        this.component = component;
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        return delegate.filterAction(new AuditInfoContext(context, component), request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        return delegate.filterCreate(new AuditInfoContext(context, component), request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        return delegate.filterDelete(new AuditInfoContext(context, component), request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        return delegate.filterPatch(new AuditInfoContext(context, component), request, next);
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        return delegate.filterQuery(new AuditInfoContext(context, component), request, handler, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        return delegate.filterRead(new AuditInfoContext(context, component), request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        return delegate.filterUpdate(new AuditInfoContext(context, component), request, next);
    }

}
