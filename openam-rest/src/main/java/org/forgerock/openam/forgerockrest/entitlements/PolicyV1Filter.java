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

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.rest.resource.ContextHelper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;

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
     * @param handler
     *         the result handler
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public void filterAction(ServerContext context, ActionRequest request,
                             ResultHandler<JsonValue> handler, RequestHandler next) {
        // Forward onto next handler.
        next.handleAction(context, request, handler);
    }

    /**
     * The policy json will not have any resource type defined. Create retrieves the policy's associated application
     * and uses the applications associated resource type for the policy.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the create request
     * @param handler
     *         the result handler
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public void filterCreate(final ServerContext context, final CreateRequest request,
                             final ResultHandler<Resource> handler, final RequestHandler next) {

        final JsonValue jsonValue = request.getContent();
        final String applicationName = jsonValue.get("applicationName").asString();

        if (applicationName == null) {
            handler.handleError(ResourceException
                    .getException(ResourceException.BAD_REQUEST, "Invalid application name defined in request"));
            return;
        }

        final Subject callingSubject = contextHelper.getSubject(context);
        final String realm = contextHelper.getRealm(context);

        try {
            final ApplicationService applicationService = applicationServiceFactory.create(callingSubject, realm);
            final Application application = applicationService.getApplication(applicationName);

            if (application == null) {
                handler.handleError(ResourceException
                        .getException(ResourceException.BAD_REQUEST, "Unable to find application " + applicationName));
                return;
            }

            if (application.getResourceTypeUuids().size() != 1) {
                handler.handleError(ResourceException
                        .getException(ResourceException.BAD_REQUEST,
                                "Cannot create policy under an application with more than " +
                                        "one resource type using version 1.0 of this endpoint"));
                return;
            }

            // Retrieve the resource type from the applications single resource type.
            final String resourceTypeUuid = application.getResourceTypeUuids().iterator().next();
            jsonValue.put(RESOURCE_TYPE_UUID, resourceTypeUuid);

        } catch (EntitlementException eE) {
            debug.error("Error filtering policy create CREST request", eE);
            handler.handleError(resourceErrorHandler.handleError(context, request, eE));
        }

        next.handleCreate(context, request, new TransformationHandler(handler));
    }

    /**
     * Update simply forwards the request on the assumption that
     * any existing policy must be associated with a resource type.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the update request
     * @param handler
     *         the result handler
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public void filterUpdate(ServerContext context, UpdateRequest request,
                             ResultHandler<Resource> handler, RequestHandler next) {
        // Forward onto next handler.
        next.handleUpdate(context, request, new TransformationHandler(handler));
    }

    /**
     * Delete simply forwards the request, no assumptions here.
     *
     * @param context
     *         the filter chain context
     * @param request
     *         the delete request
     * @param handler
     *         the result handler
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public void filterDelete(ServerContext context, DeleteRequest request,
                             ResultHandler<Resource> handler, RequestHandler next) {
        // Forward onto next handler.
        next.handleDelete(context, request, handler);
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
    public void filterQuery(final ServerContext context, final QueryRequest request,
                            final QueryResultHandler handler, final RequestHandler next) {
        next.handleQuery(context, request, new QueryResultHandler() {

            @Override
            public boolean handleResource(Resource resource) {
                final JsonValue jsonValue = resource.getContent();
                jsonValue.remove(RESOURCE_TYPE_UUID);
                return handler.handleResource(resource);
            }

            @Override
            public void handleResult(QueryResult result) {
                handler.handleResult(result);
            }

            @Override
            public void handleError(ResourceException error) {
                handler.handleError(error);
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
     * @param handler
     *         the result handler
     * @param next
     *         a request handler representing the remainder of the filter chain
     */
    @Override
    public void filterRead(ServerContext context, ReadRequest request,
                           ResultHandler<Resource> handler, RequestHandler next) {
        // Forward onto next handler.
        next.handleRead(context, request, new TransformationHandler(handler));
    }

    /*
     * Operation not currently supported. If the destination resource handler provides an implementation to this method
     * an appropriate implementation will need to be considered here also.
     */
    @Override
    public void filterPatch(ServerContext context, PatchRequest request,
                            ResultHandler<Resource> handler, RequestHandler next) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Inner static class to handle the appropriate policy json transformation.
     */
    private static final class TransformationHandler implements ResultHandler<Resource> {

        private final ResultHandler<Resource> delegate;

        TransformationHandler(
                final ResultHandler<Resource> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handleResult(Resource result) {
            final JsonValue jsonValue = result.getContent();
            jsonValue.remove(RESOURCE_TYPE_UUID);
            delegate.handleResult(result);
        }

        @Override
        public void handleError(ResourceException error) {
            delegate.handleError(error);
        }

    }

}
