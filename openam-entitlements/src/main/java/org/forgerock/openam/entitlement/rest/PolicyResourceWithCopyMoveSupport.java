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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_400_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_403_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_404_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_405_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_500_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.POLICY_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.POLICY_RESOURCE_WITH_COPY_MOVE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;

/**
 * Adds additional behaviour to the existing {@link PolicyResource} to support the move and copy of policies.
 *
 * @since 13.0.0
 */
@CollectionProvider(
        details = @Handler(
                title = POLICY_RESOURCE_WITH_COPY_MOVE + TITLE,
                description = POLICY_RESOURCE_WITH_COPY_MOVE + DESCRIPTION,
                mvccSupported = false,
                resourceSchema = @Schema(schemaResource = "PolicyResource.schema.json")),
        pathParam = @Parameter(
                name = "resourceId",
                type = "string",
                description = POLICY_RESOURCE + PATH_PARAM + DESCRIPTION))
public final class PolicyResourceWithCopyMoveSupport {

    private final Router router;
    private final CollectionResourceProvider policyResource;

    @Inject
    PolicyResourceWithCopyMoveSupport(@Named("PolicyResource") CollectionResourceProvider wrappedResource,
            @Named("CrestRealmRouter") Router router) {
        Reject.ifNull(router);
        this.router = router;
        this.policyResource = wrappedResource;
    }

    @Action(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 405,
                            description = POLICY_RESOURCE + ERROR_405_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = POLICY_RESOURCE + ERROR_500_DESCRIPTION),
                    @ApiError(
                            code = 501,
                            description = POLICY_RESOURCE + "error.501." + DESCRIPTION)},
            description = POLICY_RESOURCE + "evaluate." + ACTION_DESCRIPTION),
            request = @Schema(schemaResource = "PolicyResource.evaluate.action.request.schema.json"),
            response = @Schema(schemaResource = "PolicyResource.action.response.schema.json")
    )
    public Promise<ActionResponse, ResourceException> evaluate(Context context, ActionRequest actionRequest) {
        return policyResource.actionCollection(context, actionRequest);
    }

    @Action(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 405,
                            description = POLICY_RESOURCE + ERROR_405_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = POLICY_RESOURCE + ERROR_500_DESCRIPTION),
                    @ApiError(
                            code = 501,
                            description = POLICY_RESOURCE + "error.501." + DESCRIPTION)},
            description = POLICY_RESOURCE + "evaluatetree." + ACTION_DESCRIPTION),
            request = @Schema(schemaResource = "PolicyResource.evaluatetree.action.request.schema.json"),
            response = @Schema(schemaResource = "PolicyResource.action.response.schema.json")
    )
    public Promise<ActionResponse, ResourceException> evaluateTree(Context context, ActionRequest actionRequest) {
        return policyResource.actionCollection(context, actionRequest);
    }

    @Create(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 403,
                            description = POLICY_RESOURCE + ERROR_403_DESCRIPTION)},
            description = POLICY_RESOURCE + CREATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> createInstance(
            Context context, CreateRequest createRequest) {
        return policyResource.createInstance(context, createRequest);
    }

    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 403,
                            description = POLICY_RESOURCE + ERROR_403_DESCRIPTION)},
            description = POLICY_RESOURCE + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> deleteInstance(
            Context context, String resourceId, DeleteRequest deleteRequest) {
        return policyResource.deleteInstance(context, resourceId, deleteRequest);
    }

    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE + ERROR_400_DESCRIPTION)},
            description = POLICY_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
    public Promise<QueryResponse, ResourceException> queryCollection(
            Context context, QueryRequest queryRequest, QueryResourceHandler queryResourceHandler) {
        return policyResource.queryCollection(context, queryRequest, queryResourceHandler);
    }

    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE + ERROR_400_DESCRIPTION)},
            description = POLICY_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(
            Context context, String resourceId, ReadRequest readRequest) {
        return policyResource.readInstance(context, resourceId, readRequest);
    }

    @Update(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = POLICY_RESOURCE + ERROR_404_DESCRIPTION)},
            description = POLICY_RESOURCE + UPDATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> updateInstance(
            Context context, String resourceId, UpdateRequest updateRequest) {
        return policyResource.updateInstance(context, resourceId, updateRequest);
    }

    @Action(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 403,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_403_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_404_DESCRIPTION)},
            description = POLICY_RESOURCE_WITH_COPY_MOVE + "copy." + ACTION_DESCRIPTION),
            request = @Schema(schemaResource = "PolicyResourceWithCopyMoveSupport.copy.move.action.request.schema" +
                    ".json"),
            response = @Schema(schemaResource = "PolicyResource.schema.json")
    )
    public Promise<ActionResponse, ResourceException> copy(Context context, ActionRequest actionRequest) {
        try {
            return Promises.newResultPromise(copyOrMovePoliciesByApplication(context, actionRequest, PolicyAction
                    .COPY));
        } catch (ResourceException rE) {
            return rE.asPromise();
        }
    }

    @Action(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 403,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_403_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_404_DESCRIPTION)},
            description = POLICY_RESOURCE_WITH_COPY_MOVE + "move." + ACTION_DESCRIPTION),
            request = @Schema(schemaResource = "PolicyResourceWithCopyMoveSupport.copy.move.action.request.schema" +
                    ".json"),
            response = @Schema(schemaResource = "PolicyResource.schema.json")
    )
    public Promise<ActionResponse, ResourceException> move(Context context, ActionRequest actionRequest) {
        try {
            return Promises.newResultPromise(copyOrMovePoliciesByApplication(context, actionRequest, PolicyAction
                    .MOVE));
        } catch (ResourceException rE) {
            return rE.asPromise();
        }
    }

    private ActionResponse copyOrMovePoliciesByApplication(Context context,
            ActionRequest request, PolicyAction copyOrMoveAction) throws ResourceException {

        JsonValue payload = request.getContent();
        JsonValue from = payload.get("from");
        JsonValue to = payload.get("to");

        if (from.isNull()) {
            throw new BadRequestException("from definition is missing");
        }

        if (!from.isDefined("application")) {
            throw new BadRequestException("from application definition is missing");
        }

        String sourceApplication = from
                .get("application")
                .asString();

        if (to.isNull()) {
            throw new BadRequestException("to definition is missing");
        }

        Realm sourceRealm = RealmContext.getRealm(context);

        String destinationRealm = to
                .get("realm")
                .defaultTo(sourceRealm.asPath())
                .asString();

        String destinationApplication = to
                .get("application")
                .defaultTo(sourceApplication)
                .asString();

        JsonValue resourceTypeMapping = payload
                .get("resourceTypeMapping")
                .defaultTo(Collections.emptyMap());

        String namePostfix = to
                .get("namePostfix")
                .defaultTo("")
                .asString();

        QueryRequest queryRequest = Requests.newQueryRequest("policies");
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("applicationName"), sourceApplication));

        final List<JsonValue> policies = new ArrayList<>();
        router.handleQuery(context, queryRequest, new QueryResourceHandler() {

            @Override
            public boolean handleResource(ResourceResponse resourceResponse) {
                policies.add(resourceResponse.getContent());
                return true;
            }

        }).getOrThrowUninterruptibly();

        JsonValue actionResponseContent = json(array());

        for (JsonValue policy : policies) {
            ActionResponse response = copyOrMoveGivenPolicy(context, policy, destinationRealm,
                    destinationApplication, namePostfix, resourceTypeMapping, copyOrMoveAction);
            actionResponseContent.add(response.getJsonContent().asMap());
        }

        return Responses.newActionResponse(actionResponseContent);
    }

    private ActionResponse copyOrMoveGivenPolicy(Context context, JsonValue policy, String destinationRealm,
            String destinationApplication, String namePostfix, JsonValue resourceTypeMapping,
            PolicyAction copyOrMoveAction) throws ResourceException {

        String name = policy.get("name").asString();
        String copiedName = name + namePostfix;

        String sourceResourceType = policy.get("resourceTypeUuid").asString();
        String destinationResourceType = resourceTypeMapping
                .get(sourceResourceType)
                .defaultTo(sourceResourceType)
                .asString();

        JsonValue newPayload = json(
                object(
                        field("to",
                                object(
                                        field("name", copiedName),
                                        field("realm", destinationRealm),
                                        field("application", destinationApplication),
                                        field("resourceType", destinationResourceType)))));

        String copyOrMoveActionName = copyOrMoveAction.name().toLowerCase();
        ActionRequest newActionRequest = Requests.newActionRequest("policies", name, copyOrMoveActionName);
        newActionRequest.setContent(newPayload);

        return router
                .handleAction(context, newActionRequest)
                .getOrThrowUninterruptibly();
    }

    @Action(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 403,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_403_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_404_DESCRIPTION)},
            description = POLICY_RESOURCE_WITH_COPY_MOVE + "copy.item." + ACTION_DESCRIPTION),
            request = @Schema(schemaResource = "PolicyResourceWithCopyMoveSupport.copy.move.item.action.request" +
                    ".schema.json"),
            response = @Schema(schemaResource = "PolicyResource.schema.json")
    )
    public Promise<ActionResponse, ResourceException> copy(Context context, String resourceId,
            ActionRequest actionRequest) {
        try {
            return Promises.newResultPromise(copyPolicy(context, resourceId, actionRequest));
        } catch (ResourceException rE) {
            return rE.asPromise();
        }
    }

    @Action(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 403,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_403_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = POLICY_RESOURCE_WITH_COPY_MOVE + ERROR_404_DESCRIPTION)},
            description = POLICY_RESOURCE_WITH_COPY_MOVE + "move.item." + ACTION_DESCRIPTION),
            request = @Schema(schemaResource = "PolicyResourceWithCopyMoveSupport.copy.move.item.action.request" +
                    ".schema.json"),
            response = @Schema(schemaResource = "PolicyResource.schema.json")
    )
    public Promise<ActionResponse, ResourceException> move(Context context, String resourceId,
            ActionRequest actionRequest) {
        try {
            return Promises.newResultPromise(movePolicy(context, resourceId, actionRequest));
        } catch (ResourceException rE) {
            return rE.asPromise();
        }
    }

    private ActionResponse movePolicy(Context context, String resourceId, ActionRequest request)
            throws ResourceException {
        ActionResponse copyResponse = copyPolicy(context, resourceId, request);
        DeleteRequest deleteRequest = Requests.newDeleteRequest("policies", resourceId);
        router.handleDelete(context, deleteRequest).getOrThrowUninterruptibly();
        return copyResponse;
    }

    private ActionResponse copyPolicy(Context context, String resourceId, ActionRequest request)
            throws ResourceException {
        String sourceRealm = RealmContext.getRealm(context).asPath();
        JsonValue payload = request.getContent().get("to");

        if (payload.isNull()) {
            throw new BadRequestException("to definition is missing");
        }

        String destinationRealm = payload
                .get("realm")
                .defaultTo(sourceRealm)
                .asString();

        ReadRequest readRequest = Requests.newReadRequest("policies", resourceId);
        JsonValue policy = router.handleRead(context, readRequest)
                .getOrThrowUninterruptibly()
                .getContent();

        String sourceApplication = policy.get("applicationName").asString();
        String sourceResourceType = policy.get("resourceTypeUuid").asString();

        String destinationApplication = payload
                .get("application")
                .defaultTo(sourceApplication)
                .asString();

        String destinationResourceTypeId = payload
                .get("resourceType")
                .defaultTo(sourceResourceType)
                .asString();

        String copiedName = payload
                .get("name")
                .defaultTo(resourceId)
                .asString();

        if (sourceRealm.equals(destinationRealm) && resourceId.equals(copiedName)) {
            throw new BadRequestException("policy name already exists within the realm");
        }

        policy.put("name", copiedName);
        policy.put("applicationName", destinationApplication);
        policy.put("resourceTypeUuid", destinationResourceTypeId);

        RealmContext updatedContext;
        try {
            updatedContext = new RealmContext(context, Realm.of(destinationRealm));
        } catch (RealmLookupException e) {
            throw new BadRequestException("Invalid destination realm: " + e.getRealm(), e);
        }

        CreateRequest createRequest = Requests.newCreateRequest("policies", policy);
        JsonValue copiedPolicy = router.handleCreate(updatedContext, createRequest)
                .getOrThrowUninterruptibly()
                .getContent();

        return Responses.newActionResponse(copiedPolicy);
    }

}
