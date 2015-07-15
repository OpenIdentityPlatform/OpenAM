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

package org.forgerock.openam.rest.uma;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.forgerockrest.utils.JsonValueQueryFilterVisitor;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.PendingRequestsService;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * CREST resource for UMA Pending Requests.
 *
 * @since 13.0.0
 */
public class PendingRequestResource implements CollectionResourceProvider {

    private static final String APPROVE_ACTION_ID = "approve";
    private static final String DENY_ACTION_ID = "deny";
    private static final JsonValueQueryFilterVisitor QUERY_VISITOR = new JsonValueQueryFilterVisitor();

    private final PendingRequestsService service;
    private final ContextHelper contextHelper;

    @Inject
    public PendingRequestResource(PendingRequestsService service,
            ContextHelper contextHelper) {
        this.service = service;
        this.contextHelper = contextHelper;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            if (APPROVE_ACTION_ID.equalsIgnoreCase(request.getAction())) {
                List<Promise<Void, ResourceException>> promises = new ArrayList<>();
                JsonValue content = request.getContent();
                for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                    promises.add(service.approvePendingRequest(context, pendingRequest.getId(),
                            content.get(pendingRequest.getId()), getRealm(context)));
                }
                handlePendingRequestApproval(promises, handler);
            } else if (DENY_ACTION_ID.equalsIgnoreCase(request.getAction())) {
                for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                    service.denyPendingRequest(pendingRequest.getId(), getRealm(context));
                }
                handler.handleResult(json(object()));
            } else {
                handler.handleError(new NotSupportedException("Action, " + request.getAction()
                        + ", is not supported."));
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            if (APPROVE_ACTION_ID.equalsIgnoreCase(request.getAction())) {
                handlePendingRequestApproval(service.approvePendingRequest(context, resourceId, request.getContent(),
                                getRealm(context)), handler);
            } else if (DENY_ACTION_ID.equalsIgnoreCase(request.getAction())) {
                service.denyPendingRequest(resourceId, getRealm(context));
                handler.handleResult(json(object()));
            } else {
                handler.handleError(new NotSupportedException("Action, " + request.getAction() + ", is not supported."));
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    private void handlePendingRequestApproval(Promise<Void, ResourceException> promise,
            final ResultHandler<JsonValue> handler) {
        handlePendingRequestApproval(Collections.singletonList(promise), handler);
    }

    private void handlePendingRequestApproval(List<Promise<Void, ResourceException>> promises,
            final ResultHandler<JsonValue> handler) {
        Promises.when(promises)
                .thenOnResult(new org.forgerock.util.promise.ResultHandler<List<Void>>() {
                    @Override
                    public void handleResult(List<Void> result) {
                        handler.handleResult(json(object()));
                    }
                })
                .thenOnException(new ExceptionHandler<ResourceException>() {
                    @Override
                    public void handleException(ResourceException exception) {
                        handler.handleError(exception);
                    }
                });
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        if (request.getQueryFilter() == null) {
            handler.handleError(new NotSupportedException("Only query filter is supported."));
            return;
        }

        handler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);

        try {
            for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                if (request.getQueryFilter().accept(QUERY_VISITOR, pendingRequest.asJson())) {
                    handler.handleResource(newResource(pendingRequest));
                }
            }
            handler.handleResult(new QueryResult());
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {
        try {
            handler.handleResult(newResource(service.readPendingRequest(resourceId)));
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    private Set<UmaPendingRequest> queryResourceOwnerPendingRequests(ServerContext context) throws ResourceException {
        return service.queryPendingRequests(contextHelper.getUserId(context), getRealm(context));
    }

    private Resource newResource(UmaPendingRequest request) {
        return new Resource(request.getId(), String.valueOf(request.hashCode()), request.asJson());
    }

    private String getRealm(ServerContext context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }


}
