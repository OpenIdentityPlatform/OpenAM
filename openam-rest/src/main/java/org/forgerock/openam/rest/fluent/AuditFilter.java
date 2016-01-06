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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.rest.fluent;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Response;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.promise.RuntimeExceptionHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Filter which will audit any requests that pass through it.
 *
 * @since 13.0.0
 */
public class AuditFilter implements Filter {

    private final Debug debug;
    private final CrestAuditorFactory crestAuditorFactory;

    /**
     * Guiced constructor.
     *
     * @param debug Debug instance.
     * @param crestAuditorFactory CrestAuditorFactory for CrestAuditor instances.
     */
    @Inject
    public AuditFilter(@Named("frRest") Debug debug, CrestAuditorFactory crestAuditorFactory) {
        this.debug = debug;
        this.crestAuditorFactory = crestAuditorFactory;
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

        final CrestAuditor auditor = newAuditor(context, request);
        auditor.auditAccessAttempt();

        return auditResponse(next.handleAction(context, request), auditor, request);
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

        CrestAuditor auditor = newAuditor(context, request);
        auditor.auditAccessAttempt();

        return auditResponse(next.handleCreate(context, request), auditor, request);
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

        CrestAuditor auditor = newAuditor(context, request);
        auditor.auditAccessAttempt();

        return auditResponse(next.handleDelete(context, request), auditor, request);
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

        CrestAuditor auditor = newAuditor(context, request);
        auditor.auditAccessAttempt();

        return auditResponse(next.handlePatch(context, request), auditor, request);
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

        CrestAuditor auditor = newAuditor(context, request);
        auditor.auditAccessAttempt();

        return auditResponse(next.handleQuery(context, request, handler), auditor, request);
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

        CrestAuditor auditor = newAuditor(context, request);
        auditor.auditAccessAttempt();

        return auditResponse(next.handleRead(context, request), auditor, request);
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

        CrestAuditor auditor = newAuditor(context, request);
        auditor.auditAccessAttempt();

        return auditResponse(next.handleUpdate(context, request), auditor, request);
    }

    private <T extends Response> Promise<T, ResourceException> auditResponse(Promise<T, ResourceException> promise,
            final CrestAuditor auditingHandler, final Request request) {
        return promise
                .thenOnResult(new ResultHandler<Response>() {
                    @Override
                    public void handleResult(Response response) {
                        auditingHandler.auditAccessSuccess(getDetail(request, response));
                    }
                })
                .thenOnException(new ExceptionHandler<ResourceException>() {
                    @Override
                    public void handleException(ResourceException exception) {
                        auditingHandler.auditAccessFailure(exception.getCode(), exception.getMessage());
                    }
                })
                .thenOnRuntimeException(new RuntimeExceptionHandler() {
                    @Override
                    public void handleRuntimeException(RuntimeException exception) {
                        auditingHandler.auditAccessFailure(ResourceException.INTERNAL_ERROR, exception.getMessage());
                    }
                });
    }

    private CrestAuditor newAuditor(Context context, Request request) {
        return crestAuditorFactory.create(context, request);
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to the a successful
     * {@link Response} to a {@link Request}. This information is logged in the access audit log.
     *
     * @param request The {@link Request} instance from which details may be taken. Cannot be null.
     * @param response The {@link Response} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details.
     */
    private JsonValue getDetail(Request request, Response response) {
        Reject.ifNull(request, "request cannot be null.");
        Reject.ifNull(request, "response cannot be null.");

        RequestType requestType = request.getRequestType();

        switch (requestType) {
            case CREATE:
                return getCreateSuccessDetail((CreateRequest) request, (ResourceResponse) response);
            case READ:
                return getReadSuccessDetail((ReadRequest) request, (ResourceResponse) response);
            case UPDATE:
                return getUpdateSuccessDetail((UpdateRequest) request, (ResourceResponse) response);
            case DELETE:
                return getDeleteSuccessDetail((DeleteRequest) request, (ResourceResponse) response);
            case PATCH:
                return getPatchSuccessDetail((PatchRequest) request, (ResourceResponse) response);
            case ACTION:
                return getActionSuccessDetail((ActionRequest) request, (ActionResponse) response);
            case QUERY:
                return getQuerySuccessDetail((QueryRequest) request, (QueryResponse) response);
            default:
                throw new IllegalStateException("Unknown RequestType");
        }
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to an
     * {@link ResourceResponse} to a successful {@link CreateRequest}. This information is logged in the access
     * audit log. Subclasses can implement this method if they need to return details.
     *
     * @param request The {@link CreateRequest} instance from which details may be taken. Cannot be null.
     * @param response The {@link ResourceResponse} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details. The default for no overridden
     * implementation is provided here as null.
     */
    public JsonValue getCreateSuccessDetail(CreateRequest request, ResourceResponse response) {
        return null;
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to an
     * {@link ResourceResponse} to a successful {@link ReadRequest}. This information is logged in the access
     * audit log. Subclasses can implement this method if they need to return details.
     *
     * @param request The {@link ReadRequest} instance from which details may be taken. Cannot be null.
     * @param response The {@link ResourceResponse} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details. The default for no overridden
     * implementation is provided here as null.
     */
    public JsonValue getReadSuccessDetail(ReadRequest request, ResourceResponse response) {
        return null;
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to an
     * {@link ResourceResponse} to a successful {@link UpdateRequest}. This information is logged in the access
     * audit log. Subclasses can implement this method if they need to return details.
     *
     * @param request The {@link UpdateRequest} instance from which details may be taken. Cannot be null.
     * @param response The {@link ResourceResponse} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details. The default for no overridden
     * implementation is provided here as null.
     */
    public JsonValue getUpdateSuccessDetail(UpdateRequest request, ResourceResponse response) {
        return null;
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to an
     * {@link ResourceResponse} to a successful {@link UpdateRequest}. This information is logged in the access
     * audit log. Subclasses can implement this method if they need to return details.
     *
     * @param request The {@link UpdateRequest} instance from which details may be taken. Cannot be null.
     * @param response The {@link ResourceResponse} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details. The default for no overridden
     * implementation is provided here as null.
     */
    public JsonValue getDeleteSuccessDetail(DeleteRequest request, ResourceResponse response) {
        return null;
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to an
     * {@link ResourceResponse} to a successful {@link PatchRequest}. This information is logged in the access
     * audit log. Subclasses can implement this method if they need to return details.
     *
     * @param request The {@link PatchRequest} instance from which details may be taken. Cannot be null.
     * @param response The {@link ResourceResponse} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details. The default for no overridden
     * implementation is provided here as null.
     */
    public JsonValue getPatchSuccessDetail(PatchRequest request, ResourceResponse response) {
        return null;
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to an
     * {@link ActionResponse} to a successful {@link ActionRequest}. This information is logged in the access
     * audit log. Subclasses can implement this method if they need to return details.
     *
     * @param request The {@link ActionRequest} instance from which details may be taken. Cannot be null.
     * @param response The {@link ActionResponse} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details. The default for no overridden
     * implementation is provided here as null.
     */
    public JsonValue getActionSuccessDetail(ActionRequest request, ActionResponse response) {
        return null;
    }

    /**
     * Provides additional details (e.g. failure description or summary of the payload) relating to an
     * {@link QueryResponse} to a successful {@link QueryRequest}. This information is logged in the access
     * audit log. Subclasses can implement this method if they need to return details.
     *
     * @param request The {@link QueryRequest} instance from which details may be taken. Cannot be null.
     * @param response The {@link QueryResponse} instance from which details may be taken. Cannot be null.
     * @return {@link JsonValue} free-form details, or null to indicate no details. The default for no overridden
     * implementation is provided here as null.
     */
    public JsonValue getQuerySuccessDetail(QueryRequest request, QueryResponse response) {
        return null;
    }
}
