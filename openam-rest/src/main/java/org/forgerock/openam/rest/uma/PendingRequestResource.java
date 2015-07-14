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
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
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
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.PendingRequestsService;

/**
 * CREST resource for UMA Pending Requests.
 *
 * @since 13.0.0
 */
public class PendingRequestResource implements CollectionResourceProvider {

    private static final String APPROVE_ACTION_ID = "approve";
    private static final String DENY_ACTION_ID = "deny";

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
        if (APPROVE_ACTION_ID.equalsIgnoreCase(request.getAction())) {
            try {
                for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                    service.approvePendingRequest(pendingRequest.getId(), getRealm(context));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            }
            handler.handleResult(json(object()));
        } else if (DENY_ACTION_ID.equalsIgnoreCase(request.getAction())) {
            try {
                for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                    service.denyPendingRequest(pendingRequest.getId(), getRealm(context));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            }
            handler.handleResult(json(object()));
        } else {
            handler.handleError(new NotSupportedException("Action, " + request.getAction() + ", is not supported."));
        }
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            if (APPROVE_ACTION_ID.equalsIgnoreCase(request.getAction())) {
                service.approvePendingRequest(resourceId, getRealm(context));
                handler.handleResult(json(object()));
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

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        if (!"true".equalsIgnoreCase(request.getQueryFilter().toString())) {
            handler.handleError(new NotSupportedException("Only query filter 'true' is supported."));
            return;
        }
        //TODO support filtering
        handler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);

        try {
            for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                handler.handleResource(newResource(pendingRequest));
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
        handler.handleError(new InternalServerErrorException());
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
