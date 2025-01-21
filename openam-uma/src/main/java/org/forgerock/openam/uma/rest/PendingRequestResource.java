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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_400_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_500_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PENDING_REQUEST_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.forgerockrest.utils.JsonValueQueryFilterVisitor;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.PendingRequestsService;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * CREST resource for UMA Pending Requests.
 *
 * @since 13.0.0
 */
@CollectionProvider(
        details = @Handler(
                title = PENDING_REQUEST_RESOURCE + TITLE,
                description = PENDING_REQUEST_RESOURCE + DESCRIPTION,
                mvccSupported = false,
                parameters = {
                        @Parameter(
                                name = "user",
                                type = "string",
                                description = PENDING_REQUEST_RESOURCE + "pathparams.user")},
                resourceSchema = @Schema(schemaResource = "PendingRequestResource.schema.json")),
        pathParam = @Parameter(
                name = "pendingRequestId",
                type = "string",
                description = PENDING_REQUEST_RESOURCE + PATH_PARAM + DESCRIPTION))
public class PendingRequestResource {

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

    @Action(name = "approveAll",
            operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 500,
                                    description = PENDING_REQUEST_RESOURCE + ERROR_500_DESCRIPTION
                            )},
                    description = PENDING_REQUEST_RESOURCE + "action.approveAll." + DESCRIPTION
            ),
            request = @Schema(schemaResource = "PendingRequestResource.action.approve.request.schema.json"),
            response = @Schema())
    public Promise<ActionResponse, ResourceException> approveAll(Context context, ActionRequest request) {
        try {
            List<Promise<Void, ResourceException>> promises = new ArrayList<>();
            JsonValue content = request.getContent();
            for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                promises.add(service.approvePendingRequest(context, pendingRequest.getId(),
                        content.get(pendingRequest.getId()), ServerContextUtils.getRealm(context)));
            }
            return handlePendingRequestApproval(promises);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Action(name = "denyAll",
            operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 400,
                                    description = PENDING_REQUEST_RESOURCE + ERROR_400_DESCRIPTION
                            ),
                            @ApiError(
                                    code = 500,
                                    description = PENDING_REQUEST_RESOURCE + ERROR_500_DESCRIPTION
                            )},
                    description = PENDING_REQUEST_RESOURCE + "action.denyAll." + DESCRIPTION
            ),
            request = @Schema(),
            response = @Schema())
    public Promise<ActionResponse, ResourceException> denyAll(Context context, ActionRequest request) {
        try {
                for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                    service.denyPendingRequest(pendingRequest.getId(), ServerContextUtils.getRealm(context));
                }
                return newResultPromise(newActionResponse((json(object()))));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Action(name = APPROVE_ACTION_ID,
            operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 400,
                                    description = PENDING_REQUEST_RESOURCE + ERROR_400_DESCRIPTION
                            )
                    },
                    description = PENDING_REQUEST_RESOURCE + "action.approve." + DESCRIPTION
            ),
            request = @Schema(schemaResource = "PendingRequestResource.action.approve.request.schema.json"),
            response = @Schema())
    public Promise<ActionResponse, ResourceException> approve(Context context, String resourceId,
                                                                     ActionRequest request) {
        return handlePendingRequestApproval(service.approvePendingRequest(context, resourceId, request.getContent(),
                ServerContextUtils.getRealm(context)));
    }

    @Action(name = DENY_ACTION_ID,
            operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 400,
                                    description = PENDING_REQUEST_RESOURCE + ERROR_400_DESCRIPTION
                            ),
                            @ApiError(
                                    code = 500,
                                    description = PENDING_REQUEST_RESOURCE + ERROR_500_DESCRIPTION
                            )},
                    description = PENDING_REQUEST_RESOURCE + "action.deny." + DESCRIPTION
            ),
            request = @Schema(),
            response = @Schema())
    public Promise<ActionResponse, ResourceException> deny(Context context, String resourceId,
                                                              ActionRequest request) {
        try {
            service.denyPendingRequest(resourceId, ServerContextUtils.getRealm(context));
            return newResultPromise(newActionResponse(json(object())));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }


    private Promise<ActionResponse, ResourceException> handlePendingRequestApproval(Promise<Void, ResourceException> promise) {
        return handlePendingRequestApproval(Collections.singletonList(promise));
    }

    private Promise<ActionResponse, ResourceException> handlePendingRequestApproval(List<Promise<Void, ResourceException>> promises) {
        return Promises.when(promises)
                .thenAsync(new AsyncFunction<List<Void>, ActionResponse, ResourceException>() {
                    @Override
                    public Promise<ActionResponse, ResourceException> apply(List<Void> value) throws ResourceException {
                        return newResultPromise(newActionResponse(json(object())));
                    }
                });
    }

    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = PENDING_REQUEST_RESOURCE + ERROR_500_DESCRIPTION
                    )},
            description = PENDING_REQUEST_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*")
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        if (request.getQueryFilter() == null) {
            return new NotSupportedException("Only query filter is supported.").asPromise();
        }

        try {
            List<ResourceResponse> values = new ArrayList<>();
            // Filter items based on query filter.
            for (UmaPendingRequest pendingRequest : queryResourceOwnerPendingRequests(context)) {
                if (request.getQueryFilter().accept(QUERY_VISITOR, pendingRequest.asJson())) {
                    values.add(newResourceResponse(pendingRequest.getId(), null, pendingRequest.asJson()));
                }
            }

            // Sort and Page for presentation
            QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
            return QueryResponsePresentation.perform(handler, request, values);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Read(operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 500,
                                    description = PENDING_REQUEST_RESOURCE + ERROR_500_DESCRIPTION)},
                    description = PENDING_REQUEST_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        try {
            return newResultPromise(newResource(service.readPendingRequest(resourceId)));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private Set<UmaPendingRequest> queryResourceOwnerPendingRequests(Context context) throws ResourceException {
        return service.queryPendingRequests(contextHelper.getUserId(context), ServerContextUtils.getRealm(context));
    }

    private ResourceResponse newResource(UmaPendingRequest request) {
        return newResourceResponse(request.getId(), String.valueOf(request.hashCode()), request.asJson());
    }

}
