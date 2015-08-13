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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Filter which will audit any requests that pass through it.
 *
 * @since 13.0.0
 */
public class AuditFilter implements Filter {

    private final Debug debug;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;

    /**
     * Guiced constructor.
     *
     * @param debug Debug instance.
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory AuditEventFactory for audit event builders.
     */
    @Inject
    public AuditFilter(@Named("frRest") Debug debug, AuditEventPublisher auditEventPublisher,
                       AuditEventFactory auditEventFactory) {
        this.debug = debug;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
    }

    /**
     * Records an 'access' audit event before and after the filtered CREST resource receives an action request.
     *
     * If the 'before' audit event fails due to an error, the request is cancelled and an error response is returned.
     * If the 'after' audit event fails due to an error, the request is not cancelled as it's affects may have already
     * been applied.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param next {@inheritDoc}
     */
    @Override
    public void filterAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler, RequestHandler next) {

        AuditingResultHandler<JsonValue> auditingHandler = newAuditingResultHandler(context, request, handler);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        next.handleAction(context, request, auditingHandler);
    }

    /**
     * Records an 'access' audit event before and after the filtered CREST resource receives an create request.
     *
     * If the 'before' audit event fails due to an error, the request is cancelled and an error response is returned.
     * If the 'after' audit event fails due to an error, the request is not cancelled as it's affects may have already
     * been applied.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param next {@inheritDoc}
     */
    @Override
    public void filterCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler, RequestHandler next) {

        AuditingResultHandler<Resource> auditingHandler = newAuditingResultHandler(context, request, handler);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        next.handleCreate(context, request, auditingHandler);
    }

    /**
     * Records an 'access' audit event before and after the filtered CREST resource receives an delete request.
     *
     * If the 'before' audit event fails due to an error, the request is cancelled and an error response is returned.
     * If the 'after' audit event fails due to an error, the request is not cancelled as it's affects may have already
     * been applied.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param next {@inheritDoc}
     */
    @Override
    public void filterDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler, RequestHandler next) {

        AuditingResultHandler<Resource> auditingHandler = newAuditingResultHandler(context, request, handler);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        next.handleDelete(context, request, auditingHandler);

    }

    /**
     * Records an 'access' audit event before and after the filtered CREST resource receives an patch request.
     *
     * If the 'before' audit event fails due to an error, the request is cancelled and an error response is returned.
     * If the 'after' audit event fails due to an error, the request is not cancelled as it's affects may have already
     * been applied.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param next {@inheritDoc}
     */
    @Override
    public void filterPatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler, RequestHandler next) {

        AuditingResultHandler<Resource> auditingHandler = newAuditingResultHandler(context, request, handler);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        next.handlePatch(context, request, auditingHandler);
    }

    /**
     * Records an 'access' audit event before and after the filtered CREST resource receives an query request.
     *
     * If the 'before' audit event fails due to an error, the request is cancelled and an error response is returned.
     * If the 'after' audit event fails due to an error, the request is not cancelled as it's affects may have already
     * been applied.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param next {@inheritDoc}
     */
    @Override
    public void filterQuery(ServerContext context, QueryRequest request, QueryResourceHandler handler, RequestHandler next) {

        AuditingQueryResultHandler auditingHandler = newQueryAuditingResultHandler(context, request, handler);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        next.handleQuery(context, request, auditingHandler);
    }

    /**
     * Records an 'access' audit event before and after the filtered CREST resource receives an read request.
     *
     * If the 'before' audit event fails due to an error, the request is cancelled and an error response is returned.
     * If the 'after' audit event fails due to an error, the request is not cancelled as it's affects may have already
     * been applied.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param next {@inheritDoc}
     */
    @Override
    public void filterRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler, RequestHandler next) {

        AuditingResultHandler<Resource> auditingHandler = newAuditingResultHandler(context, request, handler);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        next.handleRead(context, request, auditingHandler);
    }

    /**
     * Records an 'access' audit event before and after the filtered CREST resource receives an update request.
     *
     * If the 'before' audit event fails due to an error, the request is cancelled and an error response is returned.
     * If the 'after' audit event fails due to an error, the request is not cancelled as it's affects may have already
     * been applied.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param next {@inheritDoc}
     */
    @Override
    public void filterUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler, RequestHandler next) {

        AuditingResultHandler<Resource> auditingHandler = newAuditingResultHandler(context, request, handler);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        next.handleUpdate(context, request, auditingHandler);
    }

    private <T> AuditingResultHandler<T> newAuditingResultHandler(ServerContext context, Request request,
                                                                  ResultHandler<T> delegate) {
        return new AuditingResultHandler<>(debug, auditEventPublisher, auditEventFactory, context, request, delegate);
    }

    private AuditingQueryResultHandler newQueryAuditingResultHandler(ServerContext context, QueryRequest request,
                                                                     QueryResultHandler delegate) {
        return new AuditingQueryResultHandler(debug, auditEventPublisher, auditEventFactory, context, request, delegate);
    }
}
