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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package org.forgerock.openam.entitlement.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

/**
 * CREST filter used transform between version 1.0 and version 2.0 of the json for the policy endpoint.
 *
 * @since 13.0.0
 */
public class PolicyV1Filter implements Filter {

    private static final String RESOURCE_TYPE_UUID = "resourceTypeUuid";

    private final ApplicationServiceFactory applicationServiceFactory;
    private final ExceptionMappingHandler<EntitlementException, ResourceException> resourceErrorHandler;
    private final ContextHelper contextHelper;
    private final Debug debug;

    @Inject
    public PolicyV1Filter(final ApplicationServiceFactory applicationServiceFactory,
                          final ExceptionMappingHandler<EntitlementException, ResourceException> resourceErrorHandler,
                          final ContextHelper contextHelper,
                          @Named("frRest") final Debug debug) {
        this.applicationServiceFactory = applicationServiceFactory;
        this.resourceErrorHandler = resourceErrorHandler;
        this.contextHelper = contextHelper;
        this.debug = debug;
    }

    /**
     * Simply forward action request on. At present json surrounding
     * policy actions doesn't concern itself with resource types.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the action request
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        // Forward onto next handler.
        return next.handleAction(context, request)
                .thenOnResult(new ResultHandler<ActionResponse>() {
                    @Override
                    public void handleResult(ActionResponse response) {
                        for (JsonValue entry : response.getJsonContent()) {
                            entry.remove("ttl");
                        }
                    }
                });
    }

    /**
     * The policy json will not have any resource type defined. Create retrieves the policy's associated application
     * and uses the applications associated resource type for the policy.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the create request
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        try {
            final JsonValue jsonValue = request.getContent();
            final Subject callingSubject = contextHelper.getSubject(context);
            final String realm = contextHelper.getRealm(context);

            retrieveResourceType(jsonValue, callingSubject, realm);

        } catch (EntitlementException eE) {
            debug.error("Error filtering policy create CREST request", eE);
            return resourceErrorHandler.handleError(context, request, eE).asPromise();
        } catch (ResourceException rE) {
            debug.error("Error filtering policy create CREST request", rE);
            return rE.asPromise();
        }

        return transform(next.handleCreate(context, request));
    }

    /**
     * Update simply forwards the request on the assumption that
     * any existing policy must be associated with a resource type.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the update request
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        try {
            final JsonValue jsonValue = request.getContent();
            final Subject callingSubject = contextHelper.getSubject(context);
            final String realm = contextHelper.getRealm(context);

            retrieveResourceType(jsonValue, callingSubject, realm);

        } catch (EntitlementException eE) {
            debug.error("Error filtering policy create CREST request", eE);
            return resourceErrorHandler.handleError(context, request, eE).asPromise();
        } catch (ResourceException rE) {
            debug.error("Error filtering policy create CREST request", rE);
            return rE.asPromise();
        }

        return transform(next.handleUpdate(context, request));
    }

    /**
     * Retrieves the resource type Id from the containing application
     * and sets it within the policies' JSON representation.
     *
     * @param jsonValue
     *         the policies' JSON representation
     * @param callingSubject
     *         the calling subject
     * @param realm
     *         the realm
     *
     * @throws EntitlementException
     *         should some policy error occur
     * @throws ResourceException
     *         should some violation occur that doesn't satisfy policy v1.0
     */
    private void retrieveResourceType(JsonValue jsonValue, Subject callingSubject, String realm) throws EntitlementException, ResourceException {

        final String applicationName = jsonValue.get("applicationName").asString();

        if (applicationName == null) {
            throw new BadRequestException("Invalid application name defined in request");
        }

        final ApplicationService applicationService = applicationServiceFactory.create(callingSubject, realm);
        final Application application = applicationService.getApplication(applicationName);

        if (application == null) {
            throw new NotFoundException("Unable to find application " + applicationName);
        }

        if (application.getResourceTypeUuids().size() != 1) {
            throw new BadRequestException("Cannot create policy under an application with more than " +
                                    "one resource type using version 1.0 of this endpoint");
        }

        // Retrieve the resource type from the applications single resource type.
        final String resourceTypeUuid = application.getResourceTypeUuids().iterator().next();
        jsonValue.put(RESOURCE_TYPE_UUID, resourceTypeUuid);
    }

    /**
     * Delete simply forwards the request, no assumptions here.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the delete request
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        // Forward onto next handler.
        return next.handleDelete(context, request);
    }

    /**
     * Query forwards the request, no assumptions here.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the query request
     * @param handler
     *         the result handler
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(final Context context,
            final QueryRequest request, final QueryResourceHandler handler, final RequestHandler next) {
        return next.handleQuery(context, request, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resource) {
                final JsonValue jsonValue = resource.getContent();
                jsonValue.remove(RESOURCE_TYPE_UUID);
                return handler.handleResource(resource);
            }
        });
    }

    /**
     * Read forwards the request, no assumptions here.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the read request
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        // Forward onto next handler.
        return transform(next.handleRead(context, request));
    }

    /*
     * Operation not currently supported. If the destination resource handler provides an implementation to this method
     * an appropriate implementation will need to be considered here also.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        return RestUtils.generateUnsupportedOperation();
    }

    private Promise<ResourceResponse, ResourceException> transform(Promise<ResourceResponse, ResourceException> promise) {
        return promise
                .thenOnResult(new ResultHandler<ResourceResponse>() {
                    @Override
                    public void handleResult(ResourceResponse response) {
                        JsonValue jsonValue = response.getContent();
                        jsonValue.remove(RESOURCE_TYPE_UUID);
                    }
                });
    }
}
