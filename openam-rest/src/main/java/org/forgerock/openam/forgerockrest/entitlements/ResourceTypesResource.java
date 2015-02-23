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
package org.forgerock.openam.forgerockrest.entitlements;

import static com.sun.identity.entitlement.EntitlementException.*;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
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
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.JsonResourceType;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.promise.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Allows for CREST-handling of stored {@link org.forgerock.openam.entitlement.ResourceType}s which know about realms.
 *
 * Note that unlike {@link com.sun.identity.entitlement.ApplicationType}s.,
 * {@link org.forgerock.openam.entitlement.ResourceType}s can be changed, so the full range of CRUD operations are
 * supported here.
 */
public class ResourceTypesResource extends RealmAwareResource {

    private static final String METHOD_PUT = "PUT";
    private static final int METHOD_NOT_ALLOWED = 405;

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
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Create {@link org.forgerock.openam.entitlement.ResourceType} in the system.
     *
     * The user's {@link org.forgerock.json.resource.SecurityContext} must indicate they are a user with
     * administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {

        if (METHOD_PUT.equalsIgnoreCase(context.asContext(HttpContext.class).getMethod())) {
            handler.handleError(ResourceException.getException(METHOD_NOT_ALLOWED));
        }

        String principalName = "unknown";
        try {
            final Subject subject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(subject);
            final String realm = getRealm(context);
            final JsonResourceType jsonWrapper = createJsonResourceType(request.getContent());

            if (!realm.equals(jsonWrapper.getRealm())) {
                throw new EntitlementException(INVALID_RESOURCE_TYPE_REALM, jsonWrapper.getRealm(), realm);
            }

            if (StringUtils.isEmpty(jsonWrapper.getName())) {
                throw new EntitlementException(MISSING_RESOURCE_TYPE_NAME);
            }

            // Here we save the resource type and use that returned, since the resource type service
            // adds all manner of good stuff - creation dates, updated dates, etc. etc.  It is the resource type filled
            // out with this extra stuff that we put into the resource and the user gets to see.
            //
            final ResourceType savedResourceType = resourceTypeService.saveResourceType(subject,
                    jsonWrapper.getResourceType(true));

            if (logger.messageEnabled()) {
                logger.message("ResourceTypeResource :: CREATE by "
                        + principalName
                        + ": for Resource Type: "
                        + savedResourceType.getName());
            }
            handler.handleResult(new Resource(savedResourceType.getUUID(), null,
                    new JsonResourceType(savedResourceType).toJsonValue()));
        } catch (EntitlementException e) {
            if (logger.errorEnabled()) {
                logger.error("ResourceTypeResource :: CREATE by "
                             + principalName
                             + ": Resource Type creation failed. ",
                             e);
            }
            handler.handleError(exceptionMappingHandler.handleError(context, request, e));
        }
    }

    /**
     * Delete a {@link org.forgerock.openam.entitlement.ResourceType} in the system.
     *
     * The user's {@link org.forgerock.json.resource.SecurityContext} must indicate they are a user with
     * administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context,
                               String resourceId,
                               DeleteRequest request,
                               ResultHandler<Resource> handler) {

        String principalName = "unknown";
        try {
            final Subject callingSubject = getSubject(context);
            final String realm = getRealm(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);

            resourceTypeService.deleteResourceType(callingSubject, realm, resourceId);

            final Resource resource = new Resource(resourceId, "0", JsonValue.json(JsonValue.object()));
            handler.handleResult(resource);
        } catch (EntitlementException e) {
            if (logger.errorEnabled()) {
                logger.error("ApplicationsResource :: DELETE by "
                        + principalName
                        + ": Application failed to delete the resource specified. ", e);
            }
            handler.handleError(exceptionMappingHandler.handleError(context, request, e));
        }
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Update a {@link org.forgerock.openam.entitlement.ResourceType} in the system.
     *
     * The user's {@link org.forgerock.json.resource.SecurityContext} must indicate they are a user with
     * administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler) {

        String principalName = "unknown";
        try {
            final Subject subject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(subject);
            final String realm = getRealm(context);

            final JsonResourceType jsonWrapper = createJsonResourceType(request.getContent());

            if (!realm.equals(jsonWrapper.getRealm())) {
                throw new EntitlementException(INVALID_RESOURCE_TYPE_REALM, jsonWrapper.getRealm(), realm);
            }

            if (StringUtils.isEmpty(jsonWrapper.getName())) {
                throw new EntitlementException(MISSING_RESOURCE_TYPE_NAME);
            }

            final ResourceType updatedResourceType = resourceTypeService.updateResourceType(subject,
                    jsonWrapper.getResourceType(false));

            if (logger.messageEnabled()) {
                logger.message("ResourceTypeResource :: UPDATE by "
                        + principalName
                        + ": for Resource Type: "
                        + jsonWrapper.getName());
            }

            handler.handleResult(new Resource(updatedResourceType.getUUID(), null,
                    new JsonResourceType(updatedResourceType).toJsonValue()));

        } catch (EntitlementException e) {
            if (logger.errorEnabled()) {
                logger.error("ResourceTypeResource :: UPDATE by "
                             + principalName
                             + ": Resource Type update failed. ", e);
            }
            handler.handleError(exceptionMappingHandler.handleError(context, request, e));
        }
    }

    /**
     * Reads the details of all {@link org.forgerock.openam.entitlement.ResourceType}s in the system.
     *
     * The user's {@link org.forgerock.json.resource.SecurityContext} must indicate they are a user with
     * administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        String principalName = "unknown";
        List<JsonResourceType> jsonWrappedResourceTypes = new LinkedList<JsonResourceType>();
        try {
            final Subject mySubject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);
            final String realm = getRealm(context);

            Set<ResourceType> resourceTypes = resourceTypeService.getResourceTypes(mySubject, realm);
            if (resourceTypes != null) {
                for (ResourceType resourceType : resourceTypes) {
                    jsonWrappedResourceTypes.add(new JsonResourceType(resourceType));
                }
            }
        } catch (EntitlementException ee) {
            if (logger.errorEnabled()) {
                logger.error("ResourceTypesResource :: QUERY by "
                             + principalName
                             + ": Caused EntitlementException: ",
                             ee);
            }
            // we can safely continue here, because resourceTypes will be empty
        }

        final List<JsonValue> jsonifiedResourceTypes = jsonify(jsonWrappedResourceTypes);

        handler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);

        int remaining = jsonWrappedResourceTypes.size();
        if (remaining > 0) {
            for (JsonValue resourceTypeToReturn : jsonifiedResourceTypes) {

                final JsonValue resourceId = resourceTypeToReturn.get(new JsonPointer(JsonResourceType.FIELD_NAME));
                final String id = resourceId != null ? resourceId.toString() : null;

                if (!handler.handleResource(new Resource(id,
                        String.valueOf(System.currentTimeMillis()), resourceTypeToReturn))) {
                    break;
                }

                remaining--;
                if (logger.messageEnabled()) {
                    logger.message("ApplicationTypesResource :: QUERY by "
                            + principalName
                            + ": Added resource to response: "
                            + id);
                }
            }
        }
        handler.handleResult(new QueryResult(null, remaining));
    }

    /**
     * Takes a set of ResourceTypes and for each returns their Json representation as a JsonValue.
     *
     * @param types The ResourceTypes whose values to look up and return in the JsonValue
     * @return a {@link org.forgerock.json.fluent.JsonValue} object representing the provided {@link java.util.Set}
     */
    private List<JsonValue> jsonify(List<JsonResourceType> types) {

        JsonResourceTypeToJsonValueMapper mapper = new JsonResourceTypeToJsonValueMapper();
        List<JsonValue> resourceTypeList = new ArrayList<JsonValue>();
        try {
            resourceTypeList = CollectionUtils.transformList(types, mapper);
        } catch (EntitlementException ee) {
            if (logger.warningEnabled()) {
                logger.warning("ResourceTypesResource :: JSONIFY - Error applying "
                               + "jsonification to the ResourceType class representation.",
                               ee);
            }
        }
        return resourceTypeList;
    }

    /**
     * Reads the details of a single instance of an {@link org.forgerock.openam.entitlement.ResourceType} - the instance
     * referred to by the passed-in resourceId.
     *
     * The user's {@link org.forgerock.json.resource.SecurityContext} must indicate they are a user with
     * administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler) {

        String principalName = "unknown";
        try {
            Subject theSubject = getSubject(context);
            principalName = PrincipalRestUtils.getPrincipalNameFromSubject(theSubject);
            final String realm = getRealm(context);

            ResourceType resourceType = resourceTypeService.getResourceType(theSubject, realm, resourceId);
            JsonResourceType wrapper = new JsonResourceType(resourceType);

            final Resource resource = new Resource(resourceId,
                                                   String.valueOf(System.currentTimeMillis()),
                                                   JsonValue.json(wrapper.toJsonValue()));
            handler.handleResult(resource);

        } catch (EntitlementException ee) {
            if (logger.errorEnabled()) {
                logger.error("ResourceTypesResource :: READ by "
                        + principalName
                        + ": Could not jsonify class associated with defined Type: " + resourceId, ee);
            }
            handler.handleError(exceptionMappingHandler.handleError(context, request, ee));
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
    private Subject getSubject(ServerContext context) throws EntitlementException {
        Subject result = getContextSubject(context);
        if (result == null) {
            throw new EntitlementException(INTERNAL_ERROR, "Cannot retrieve subject");
        }
        return result;
    }


    /**
     * Mapping function that maps a JsonResourceType to a JsonValue
     */
    private static final class JsonResourceTypeToJsonValueMapper
            implements Function<JsonResourceType, JsonValue, EntitlementException> {

        @Override
        public JsonValue apply(final JsonResourceType entry) throws EntitlementException {
            return entry.toJsonValue();
        }
    }
}
