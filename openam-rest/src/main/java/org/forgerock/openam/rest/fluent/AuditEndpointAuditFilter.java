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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

import javax.inject.Inject;

/**
 * This filter wrapper adds the audit context to the passed server context and delegates on the request.
 *
 * @since 13.0.0
 */
public final class AuditEndpointAuditFilter implements Filter {

    private final AuditFilter delegate;

    /**
     * Guiced constructor.
     *
     * @param auditFilter AuditFilter to which auditing of read and query operations is delegated.
     */
    @Inject
    public AuditEndpointAuditFilter(AuditFilter auditFilter) {
        this.delegate = auditFilter;
    }

    @Override
    public void filterAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler, RequestHandler next) {
        delegate.filterAction(context, request, handler, next);
    }

    @Override
    public void filterCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler, RequestHandler next) {
        // skip delegate to avoid auditing calls to log audit events
        next.handleCreate(context, request, handler);
    }

    @Override
    public void filterDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler, RequestHandler next) {
        // audit service does not support delete; therefore, any calls to delete should be audited.
        delegate.filterDelete(context, request, handler, next);
    }

    @Override
    public void filterPatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler, RequestHandler next) {
        // audit service does not support patch; therefore, any calls to patch should be audited.
        delegate.filterPatch(context, request, handler, next);
    }

    @Override
    public void filterQuery(ServerContext context, QueryRequest request, QueryResultHandler handler, RequestHandler next) {
        delegate.filterQuery(context, request, handler, next);
    }

    @Override
    public void filterRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler, RequestHandler next) {
        delegate.filterRead(context, request, handler, next);
    }

    @Override
    public void filterUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler, RequestHandler next) {
        // audit service does not support update; therefore, any calls to update should be audited.
        delegate.filterUpdate(context, request, handler, next);
    }

}