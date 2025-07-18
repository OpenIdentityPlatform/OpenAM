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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;
import static org.forgerock.openam.rest.RestUtils.crestProtocolVersion;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jakarta.inject.Inject;
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
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;

/**
 * REST endpoint for UMA policy management.
 *
 * @since 13.0.0
 */
@CollectionProvider(details = @Handler(
        mvccSupported = true,
        title = UMA_POLICY_RESOURCE + TITLE,
        description =  UMA_POLICY_RESOURCE + DESCRIPTION,
        parameters = {
                @Parameter(
                        name = "user",
                        type = "string",
                        description = UMA_POLICY_RESOURCE + "pathParam.user." + DESCRIPTION)},
        resourceSchema = @Schema(schemaResource = "UmaPolicyResource.schema.json")),
        pathParam = @Parameter(
                name = "resourceId",
                type = "string",
                description = UMA_POLICY_RESOURCE + "pathParam.resourceId." + DESCRIPTION))
public class UmaPolicyResource {

    private final UmaPolicyService umaPolicyService;

    /**
     * Creates an instance of the {@code UmaPolicyResource}.
     *
     * @param umaPolicyService An instance of the {@code UmaPolicyService}.
     */
    @Inject
    public UmaPolicyResource(UmaPolicyService umaPolicyService) {
        this.umaPolicyService = umaPolicyService;
    }

    /**
     * {@link RequestHandler#handleCreate(Context, CreateRequest)
     * Adds} a new resource instance to the collection.
     * <p>
     * Create requests are targeted at the collection itself and may include a
     * user-provided resource ID for the new resource as part of the request
     * itself. The user-provider resource ID may be accessed using the method
     * {@link CreateRequest#getNewResourceId()}.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The create request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleCreate(Context, CreateRequest)
     * @see CreateRequest#getNewResourceId()
     */
    @Create(operationDescription = @Operation(
            description = UMA_POLICY_RESOURCE + CREATE_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = BAD_REQUEST,
                            description = UMA_POLICY_RESOURCE + CREATE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = FORBIDDEN,
                            description = UMA_POLICY_RESOURCE + CREATE + ERROR_403_DESCRIPTION),
                    @ApiError(
                            code = CONFLICT,
                            description = UMA_POLICY_RESOURCE + CREATE + ERROR_409_DESCRIPTION),
                    @ApiError(
                            code = INTERNAL_ERROR,
                            description = UMA_POLICY_RESOURCE + CREATE + ERROR_500_DESCRIPTION)}))
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context,
            final CreateRequest request) {
        if (request.getNewResourceId() != null) {
            return new NotSupportedException("Cannot provide a policy ID").asPromise();
        }
        return umaPolicyService.createPolicy(context, request.getContent())
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(UmaPolicy result) {
                        return newResultPromise(newResourceResponse(result.getId(), result.getRevision(), json(object())));
                    }
                });
    }

    /**
     * {@link RequestHandler#handleRead(Context, ReadRequest)
     * Reads} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The read request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleRead(Context, ReadRequest)
     */
    @Read(operationDescription = @Operation(
            description = UMA_POLICY_RESOURCE + READ_DESCRIPTION
    ))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, final String resourceId,
            ReadRequest request) {
        return umaPolicyService.readPolicy(context, resourceId)
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(UmaPolicy result) {
                        return newResultPromise(newResourceResponse(result.getId(), result.getRevision(), result.asJson()));
                    }
                });
    }

    /**
     * {@link RequestHandler#handleUpdate(Context, UpdateRequest)
     * Updates} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The update request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleUpdate(Context, UpdateRequest)
     */
    @Update(operationDescription = @Operation(
            description = UMA_POLICY_RESOURCE + UPDATE_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = BAD_REQUEST,
                            description = UMA_POLICY_RESOURCE + UPDATE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = FORBIDDEN,
                            description = UMA_POLICY_RESOURCE + UPDATE + ERROR_403_DESCRIPTION),
                    @ApiError(
                            code = INTERNAL_ERROR,
                            description = UMA_POLICY_RESOURCE + UPDATE + ERROR_500_DESCRIPTION)}))
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return umaPolicyService.updatePolicy(context, resourceId, request.getContent())
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(UmaPolicy result) {
                        return newResultPromise(newResourceResponse(result.getId(), result.getRevision(), result.asJson()));
                    }
                });
    }

    /**
     * {@link RequestHandler#handleDelete(Context, DeleteRequest)
     * Removes} a resource instance from the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The delete request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleDelete(Context, DeleteRequest)
     */
    @Delete(operationDescription = @Operation(
            description = UMA_POLICY_RESOURCE + DELETE_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = BAD_REQUEST,
                            description = UMA_POLICY_RESOURCE + DELETE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = NOT_FOUND,
                            description = UMA_POLICY_RESOURCE + DELETE + ERROR_404_DESCRIPTION),
                    @ApiError(
                            code = INTERNAL_ERROR,
                            description = UMA_POLICY_RESOURCE + DELETE + ERROR_500_DESCRIPTION)}))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, final String resourceId,
            DeleteRequest request) {
        return umaPolicyService.deletePolicy(context, resourceId)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) throws ResourceException {
                        return newResultPromise(newResourceResponse(resourceId, "0", json(object())));
                    }
                });
    }

    /**
     * {@link RequestHandler#handleQuery(Context, QueryRequest, QueryResourceHandler)
     * Searches} the collection for all resources which match the query request
     * criteria.
     * <p>
     * Implementations must invoke
     * {@link QueryResourceHandler#handleResource(ResourceResponse)} for each resource
     * which matches the query criteria. Once all matching resources have been
     * returned implementations are required to return either a
     * {@link QueryResponse} if the query has completed successfully, or
     * {@link ResourceException} if the query did not complete successfully
     * (even if some matching resources were returned).
     *
     * @param context
     *            The request server context.
     * @param request
     *            The query request.
     * @param handler
     *            The query resource handler to be notified for each matching
     *            resource.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleQuery(Context, QueryRequest, QueryResourceHandler)
     */
    @Query(operationDescription = @Operation(
            description = UMA_POLICY_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*")
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, final QueryRequest request,
            final QueryResourceHandler handler) {
        return umaPolicyService.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResponse, Collection<UmaPolicy>>, QueryResponse, ResourceException>() {
                    @Override
                    public Promise<QueryResponse, ResourceException> apply(Pair<QueryResponse, Collection<UmaPolicy>> result) {
                        List<ResourceResponse> values = new ArrayList<>();
                        for (UmaPolicy policy : result.getSecond()) {
                            values.add(newResourceResponse(policy.getId(), null, policy.asJson()));
                        }
                        return QueryResponsePresentation.perform(handler, request, values);
                    }
                });
    }
}
