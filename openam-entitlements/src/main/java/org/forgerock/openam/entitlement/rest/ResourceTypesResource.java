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

import static com.sun.identity.entitlement.EntitlementException.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_400_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_404_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_409_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_500_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.RESOURCE_TYPES_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.openam.utils.StringUtils.*;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.security.auth.Subject;

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
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.rest.wrappers.JsonResourceType;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.rest.RealmAwareResource;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.query.DataQueryFilterVisitor;
import org.forgerock.openam.rest.query.QueryException;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;

/**
 * Allows for CREST-handling of stored {@link org.forgerock.openam.entitlement.ResourceType}s which know about realms.
 *
 * Note that unlike {@link com.sun.identity.entitlement.ApplicationType}s.,
 * {@link org.forgerock.openam.entitlement.ResourceType}s can be changed, so the full range of CRUD operations are
 * supported here.
 */
@CollectionProvider(
        details = @Handler(
                title = RESOURCE_TYPES_RESOURCE + TITLE,
                description = RESOURCE_TYPES_RESOURCE + DESCRIPTION,
                mvccSupported = false,
                resourceSchema = @Schema(schemaResource = "ResourceTypesResource.schema.json")),
        pathParam = @Parameter(
                name = "resourceId",
                type = "string",
                description = RESOURCE_TYPES_RESOURCE + PATH_PARAM + DESCRIPTION))
public class ResourceTypesResource extends RealmAwareResource {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResourceTypeService resourceTypeService;
    private final Debug logger;
    private final ExceptionMappingHandler<EntitlementException, ResourceException> exceptionMappingHandler;

    /**
     * Guiced-constructor.
     *
     * @param logger Logger to use
     * @param exceptionMappingHandler exception mapping handler
     * @param resourceTypeService the resource type service implementation
     */
    @Inject
    public ResourceTypesResource(
            @Named("frRest") Debug logger,
            ExceptionMappingHandler<EntitlementException, ResourceException> exceptionMappingHandler,
            ResourceTypeService resourceTypeService) {
        this.resourceTypeService = resourceTypeService;
        this.logger = logger;
        this.exceptionMappingHandler = exceptionMappingHandler;
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Create {@link org.forgerock.openam.entitlement.ResourceType} in the system.
     *
     * The user's {@link Context} must indicate they are a user with administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Create(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 409,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_409_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_500_DESCRIPTION)},
            description = RESOURCE_TYPES_RESOURCE + CREATE_DESCRIPTION))
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        String principalName = "unknown";
        try {
            final Subject subject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(subject);
            final JsonResourceType jsonWrapper = createJsonResourceType(request.getContent());

            ensureResourceIdMatch(jsonWrapper, request.getNewResourceId());

            if (isEmpty(jsonWrapper.getName())) {
                throw new EntitlementException(MISSING_RESOURCE_TYPE_NAME);
            }

            final ResourceType savedResourceType = resourceTypeService.saveResourceType(subject, getRealm(context),
                    jsonWrapper.getResourceType(isBlank(jsonWrapper.getUUID())));

            logger.message("ResourceTypeResource :: CREATE by {}: for Resource Type: {}", principalName,
                    savedResourceType.getName());

            return newResultPromise(newResourceResponse(savedResourceType.getUUID(), null,
                    new JsonResourceType(savedResourceType).toJsonValue()));
        } catch (EntitlementException e) {
            logger.error("ResourceTypeResource :: CREATE by {}: Resource Type creation failed. ", principalName, e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Delete a {@link org.forgerock.openam.entitlement.ResourceType} in the system.
     *
     * The user's {@link Context} must indicate they are a user with administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_404_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_500_DESCRIPTION)},
            description = RESOURCE_TYPES_RESOURCE + DELETE_DESCRIPTION))
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {

        String principalName = "unknown";
        try {
            final Subject callingSubject = getSubject(context);
            final String realm = getRealm(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);

            resourceTypeService.deleteResourceType(callingSubject, realm, resourceId);

            final ResourceResponse resource = newResourceResponse(resourceId, "0", JsonValue.json(JsonValue.object()));
            return newResultPromise(resource);
        } catch (EntitlementException e) {
            if (logger.errorEnabled()) {
                logger.error("ApplicationsResource :: DELETE by "
                        + principalName
                        + ": Application failed to delete the resource specified. ", e);
            }
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Update a {@link org.forgerock.openam.entitlement.ResourceType} in the system.
     *
     * The user's {@link Context} must indicate they are a user with administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Update(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_404_DESCRIPTION),
                    @ApiError(
                            code = 409,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_409_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_500_DESCRIPTION)},
            description = RESOURCE_TYPES_RESOURCE + UPDATE_DESCRIPTION))
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {

        String principalName = "unknown";
        try {
            final Subject subject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(subject);
            final JsonResourceType jsonWrapper = createJsonResourceType(request.getContent());

            ensureResourceIdMatch(jsonWrapper, resourceId);

            if (isEmpty(jsonWrapper.getName())) {
                throw new EntitlementException(MISSING_RESOURCE_TYPE_NAME);
            }

            final ResourceType updatedResourceType = resourceTypeService.updateResourceType(subject, getRealm(context),
                    jsonWrapper.getResourceType(false));

            logger.message("ResourceTypeResource :: UPDATE by {}: for Resource Type: {}", principalName,
                    jsonWrapper.getName());

            return newResultPromise(newResourceResponse(updatedResourceType.getUUID(), null,
                    new JsonResourceType(updatedResourceType).toJsonValue()));

        } catch (EntitlementException e) {
            logger.error("ResourceTypeResource :: UPDATE by {}: Resource Type update failed. ", principalName, e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Reads the details of all {@link org.forgerock.openam.entitlement.ResourceType}s in the system.
     *
     * The user's {@link Context} must indicate they are a user with administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 404,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_404_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_500_DESCRIPTION)},
            description = RESOURCE_TYPES_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        String principalName = "unknown";
        String realm = getRealm(context);
        QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();

        try {
            Subject subject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(subject);
            Map<String, Map<String, Set<String>>> configData = resourceTypeService.getResourceTypesData(subject, realm);

            Set<String> filterResults;
            if (queryFilter == null) {
                filterResults = configData.keySet();
            } else {
                filterResults = queryFilter.accept(new DataQueryFilterVisitor(), configData);
            }

            List<ResourceResponse> results = new ArrayList<>();
            for (String uuid : filterResults) {
                ResourceType resourceType = resourceTypeService.getResourceType(subject, realm, uuid);
                results.add(newResourceResponse(resourceType.getUUID(), null,
                        new JsonResourceType(resourceType).toJsonValue()));
            }

            QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
            return QueryResponsePresentation.perform(handler, request, results);

        } catch (EntitlementException ee) {
            if (logger.errorEnabled()) {
                logger.error("ResourceTypesResource :: QUERY by " + principalName
                        + ": Caused EntitlementException: ", ee);
            }
            return exceptionMappingHandler.handleError(context, request, ee).asPromise();
        } catch (QueryException e) {
            return new BadRequestException(e.getL10NMessage(ServerContextUtils.getLocaleFromContext(context)))
                    .asPromise();
        }
    }

    /**
     * Reads the details of a single instance of an {@link org.forgerock.openam.entitlement.ResourceType} - the instance
     * referred to by the passed-in resourceId.
     *
     * The user's {@link Context} must indicate they are a user with administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 404,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_404_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = RESOURCE_TYPES_RESOURCE + ERROR_500_DESCRIPTION)},
            description = RESOURCE_TYPES_RESOURCE + READ_DESCRIPTION))
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {

        String principalName = "unknown";
        try {
            Subject theSubject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(theSubject);
            final String realm = getRealm(context);

            ResourceType resourceType = resourceTypeService.getResourceType(theSubject, realm, resourceId);
            if (resourceType == null) {
                throw new EntitlementException(NO_SUCH_RESOURCE_TYPE, resourceId, realm);
            }
            JsonResourceType wrapper = new JsonResourceType(resourceType);

            final ResourceResponse resource = newResourceResponse(resourceId,
                    String.valueOf(currentTimeMillis()),
                    JsonValue.json(wrapper.toJsonValue()));
            return newResultPromise(resource);

        } catch (EntitlementException ee) {
            if (logger.errorEnabled()) {
                logger.error("ResourceTypesResource :: READ by "
                        + principalName
                        + ": Could not jsonify class associated with defined Type: " + resourceId, ee);
            }
            return exceptionMappingHandler.handleError(context, request, ee).asPromise();
        }
    }

    /**
     * Abstracts out the createJsonResourceType method so that we can easily test this class.
     *
     * @param jsonValue The JsonValue to create the wrapper from
     * @return An ApplicationWrapper, wrapping the Application represented by the JsonValue provided
     * @throws EntitlementException If there were errors writing the application to disk
     */
    private JsonResourceType createJsonResourceType(JsonValue jsonValue) throws EntitlementException {
        try {
            String s = jsonValue.toString();
            return MAPPER.readValue(s, JsonResourceType.class);
        } catch (IOException e) {
            logger.error("Caught IOException while creating JSON wrapper", e);
            throw new EntitlementException(INVALID_CLASS, getMessage(e));
        }
    }

    /**
     * Return a suitable message from the exception.  If the exception has a cause, and that cause has a message,
     * return that, otherwise return the exception's message.
     *
     * @param e The exception
     * @return An appropriate message.
     */
    private String getMessage(Exception e) {
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            return e.getCause().getMessage();
        }
        return e.getMessage();
    }

    /**
     * Get the subject from the context, or die trying.
     * @param context The context
     * @return The subject
     * @throws EntitlementException if we fail to get the subject
     */
    private Subject getSubject(Context context) throws EntitlementException {
        Subject result = getContextSubject(context);
        if (result == null) {
            throw new EntitlementException(EntitlementException.INTERNAL_ERROR, "Cannot retrieve subject");
        }
        return result;
    }

    private void ensureResourceIdMatch(JsonResourceType jsonWrapper, String resourceId) throws EntitlementException {
        String uuid = jsonWrapper.getUUID();
        if (uuid != null && resourceId != null) {
            if (!uuid.equals(resourceId)) {
                throw new EntitlementException(RESOURCE_TYPE_ID_MISMATCH);
            }
        }

        if (isBlank(uuid)) {
            jsonWrapper.setUUID(resourceId);
        }
    }
}
