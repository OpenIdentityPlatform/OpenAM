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

import static org.forgerock.util.promise.Promises.newExceptionPromise;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.http.Context;
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
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Response;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

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
     * @param next {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {

        final AuditingResultHandler auditingHandler = newAuditingResultHandler(context, request);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            return newExceptionPromise(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

        return auditResponse(next.handleAction(context, request), auditingHandler);
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
     * @param next {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {

        AuditingResultHandler auditingHandler = newAuditingResultHandler(context, request);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            return newExceptionPromise(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

        return auditResponse(next.handleCreate(context, request), auditingHandler);
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
     * @param next {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {

        AuditingResultHandler auditingHandler = newAuditingResultHandler(context, request);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            return newExceptionPromise(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

        return auditResponse(next.handleDelete(context, request), auditingHandler);
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
     * @param next {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {

        AuditingResultHandler auditingHandler = newAuditingResultHandler(context, request);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            return newExceptionPromise(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

        return auditResponse(next.handlePatch(context, request), auditingHandler);
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
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {

        AuditingResultHandler auditingHandler = newAuditingResultHandler(context, request);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            return newExceptionPromise(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

        return auditResponse(next.handleQuery(context, request, handler), auditingHandler);
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
     * @param next {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {

        AuditingResultHandler auditingHandler = newAuditingResultHandler(context, request);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            return newExceptionPromise(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

        return auditResponse(next.handleRead(context, request), auditingHandler);
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
     * @param next {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {

        AuditingResultHandler auditingHandler = newAuditingResultHandler(context, request);
        try {
            auditingHandler.auditAccessAttempt();
        } catch (AuditException e) {
            return newExceptionPromise(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

        return auditResponse(next.handleUpdate(context, request), auditingHandler);
    }

    private <T extends Response> Promise<T, ResourceException> auditResponse(Promise<T, ResourceException> promise,
            final AuditingResultHandler auditingHandler) {
        return promise
                .thenOnResult(new ResultHandler<Response>() {
                    @Override
                    public void handleResult(Response response) {
                        auditingHandler.auditAccessSuccess();
                    }
                })
                .thenOnException(new ExceptionHandler<ResourceException>() {
                    @Override
                    public void handleException(ResourceException exception) {
                        auditingHandler.auditAccessFailure(exception.getCode(), exception.getMessage());
                    }
                });
    }

    private AuditingResultHandler newAuditingResultHandler(Context context, Request request) {
        return new AuditingResultHandler(debug, auditEventPublisher, auditEventFactory, context, request);
    }
}
