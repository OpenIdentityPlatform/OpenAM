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

import static org.forgerock.openam.utils.CollectionUtils.transformSet;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.RandomStringUtils;
import org.forgerock.guava.common.base.Strings;
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
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.configuration.SmsAttribute;
import org.forgerock.openam.entitlement.configuration.ResourceTypeSmsAttributes;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.query.QueryFilter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CREST filter used transform between version 1.0 and version 2.0 of the json for the application endpoint.
 *
 * @since 13.0.0
 */
public class ApplicationV1Filter implements Filter {

    private static final String RESOURCE_TYPE_UUIDS = "resourceTypeUuids";
    private static final String ACTIONS = "actions";
    private static final String RESOURCES = "resources";
    private static final String APPLICATION_NAME = "name";

    private final ResourceTypeService resourceTypeService;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ExceptionMappingHandler<EntitlementException, ResourceException> resourceErrorHandler;
    private final Debug debug;

    @Inject
    public ApplicationV1Filter(final ResourceTypeService resourceTypeService,
                               final ApplicationServiceFactory applicationServiceFactory,
                               final ExceptionMappingHandler<EntitlementException, ResourceException> resourceErrorHandler,
                               @Named("frRest") final Debug debug) {
        this.resourceTypeService = resourceTypeService;
        this.applicationServiceFactory = applicationServiceFactory;
        this.resourceErrorHandler = resourceErrorHandler;
        this.debug = debug;
    }

    /**
     * Create expects the application json to contain both actions and resources; these attributes are part of the old
     * json definition for an application. It takes these attributes and tries to identify a pre-existing resource
     * type with the same values. If it finds an entry this resource type is associated with the new application
     * json, otherwise a new resource type is dynamically created and associated.
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
        final Map<String, Boolean> actions = jsonValue.get(ACTIONS).asMap(Boolean.class);
        final Set<String> resources = jsonValue.get(RESOURCES).asSet(String.class);

        if (actions == null) {
            handler.handleError(ResourceException
                    .getException(ResourceException.BAD_REQUEST, "Invalid actions defined in request"));
            return;
        }

        if (resources == null) {
            handler.handleError(ResourceException
                    .getException(ResourceException.BAD_REQUEST, "Invalid resources defined in request"));
            return;
        }

        try {
            final ResourceType resourceType = findOrCreateResourceType(actions, resources, context, request);
            jsonValue.put(RESOURCE_TYPE_UUIDS, new HashSet<String>(Arrays.asList(resourceType.getUUID())));

            // Forward onto next handler.
            next.handleCreate(context, request, new TransformationHandler(handler, context));

        } catch (EntitlementException eE) {
            debug.error("Error filtering application create CREST request", eE);
            handler.handleError(resourceErrorHandler.handleError(context, request, eE));
        }
    }

    /**
     * Attempts to first find a resource type that has the same set of
     * actions and resources. If none is found a new resource type is created.
     *
     * @param actions
     *         the map of resource type actions
     * @param resources
     *         the set of resource type patterns
     * @param context
     *         the filter chain context
     * @param request
     *         the create request
     *
     * @return a resource type that matches the passed actions and resources
     *
     * @throws EntitlementException
     *         should some error occur finding or creating a resource type
     */
    private ResourceType findOrCreateResourceType(
            final Map<String, Boolean> actions, Set<String> resources,
            final ServerContext context, CreateRequest request) throws EntitlementException {

        final Subject callingSubject = getContextSubject(context);
        final String realm = getRealm(context);

        final Set<QueryFilter<SmsAttribute>> actionFilters = transformSet(actions.entrySet(), new ActionsToQuery());
        final Set<QueryFilter<SmsAttribute>> patternFilters = transformSet(resources, new ResourcesToQuery());

        final Set<ResourceType> resourceTypes = resourceTypeService.getResourceTypes(
                QueryFilter.and(
                        QueryFilter.and(actionFilters),
                        QueryFilter.and(patternFilters)),
                callingSubject, realm);

        if (!resourceTypes.isEmpty()) {
            // Some matching resource types have been found, return the first one.
            return resourceTypes.iterator().next();
        }

        final String resourceTypeName = generateResourceTypeName(request);
        final ResourceType resourceType = ResourceType
                .builder(resourceTypeName, realm)
                .setActions(actions)
                .setPatterns(resources)
                .setDescription("Generated resource type")
                .generateUUID()
                .build();

        // Create and return new resource type.
        return resourceTypeService.saveResourceType(callingSubject, resourceType);
    }

    /**
     * Generates a new resource type name in the format applicationName + "resourceType" + four random numbers.
     *
     * @param request
     *         the create request
     *
     * @return a newly generate resource type name
     *
     * @throws EntitlementException
     *         if the application name cannot be determined
     */
    private String generateResourceTypeName(CreateRequest request) throws EntitlementException {
        String applicationName = request.getNewResourceId();

        if (applicationName == null) {
            applicationName = request.getContent().get(APPLICATION_NAME).asString();

            if (applicationName == null) {
                throw new EntitlementException(EntitlementException.INVALID_VALUE, APPLICATION_NAME);
            }
        }

        return applicationName + "ResourceType" + RandomStringUtils.randomNumeric(4);
    }

    /**
     * Update expects the application json to contain both actions and resources; these attributes are part of the old
     * json definition for an application. It also expects that the mentioned application exists with exactly one
     * resource type - no resource types or many resource types is not acceptable, else it is impossible to determine
     * which resource type applies to the set of actions and resources being passed as part of the application json.
     * <p/>
     * Changes to the actions and/or resources will be reflected in the applications associated resource type.
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
    public void filterUpdate(final ServerContext context, final UpdateRequest request,
                             final ResultHandler<Resource> handler, final RequestHandler next) {

        final JsonValue jsonValue = request.getContent();
        final Map<String, Boolean> actions = jsonValue.get(ACTIONS).asMap(Boolean.class);
        final Set<String> resources = jsonValue.get(RESOURCES).asSet(String.class);

        if (actions == null) {
            handler.handleError(ResourceException
                    .getException(ResourceException.BAD_REQUEST, "Invalid actions defined in request"));
            return;
        }

        if (resources == null) {
            handler.handleError(ResourceException
                    .getException(ResourceException.BAD_REQUEST, "Invalid resources defined in request"));
            return;
        }

        final String realm = getRealm(context);
        final Subject callingSubject = getContextSubject(context);
        final String applicationName = request.getResourceName();

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
                                "Cannot modify application with more than one " +
                                        "resource type using version 1.0 of this endpoint"));
                return;
            }

            // Retrieve the resource type from the applications single resource type.
            final String resourceTypeUuid = application.getResourceTypeUuids().iterator().next();
            ResourceType resourceType = resourceTypeService.getResourceType(callingSubject, realm, resourceTypeUuid);

            boolean resourceTypeModified = false;

            if (!actions.equals(resourceType.getActions())) {
                resourceTypeModified = true;
                resourceType = resourceType
                        .builder()
                        .setActions(actions)
                        .build();
            }

            if (!resources.equals(resourceType.getPatterns())) {
                resourceTypeModified = true;
                resourceType = resourceType
                        .builder()
                        .setPatterns(resources)
                        .build();
            }

            if (resourceTypeModified) {
                resourceTypeService.updateResourceType(callingSubject, resourceType);
            }

            // Ensure the resource type UUID isn't lost.
            jsonValue.put(RESOURCE_TYPE_UUIDS, new HashSet<String>(Arrays.asList(resourceTypeUuid)));

        } catch (EntitlementException eE) {
            debug.error("Error filtering application update CREST request", eE);
            handler.handleError(resourceErrorHandler.handleError(context, request, eE));
            return;
        }

        // Forward onto next handler.
        next.handleUpdate(context, request, new TransformationHandler(handler, context));
    }

    /**
     * Delete does nothing further other than to forward the request on. This results in any
     * associated resource type  being left orphaned if it is not used by any other application.
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
     * Transforms each application result such that each application's resource types are removed and a single set of
     * actions and resources are represented instead. The set of actions and resources are a union of their respective
     * parts from the associated resource types.
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

        final Subject callingSubject = getContextSubject(context);
        final String realm = getRealm(context);

        final List<Resource> resources = new ArrayList<Resource>();

        // Forward onto next handler.
        next.handleQuery(context, request, new QueryResultHandler() {

            @Override
            public boolean handleResource(Resource resource) {
                return resources.add(resource);
            }

            @Override
            public void handleResult(QueryResult result) {
                try {
                    for (Resource resource : resources) {
                        final JsonValue jsonValue = resource.getContent();
                        transformJson(jsonValue, callingSubject, realm);
                        handler.handleResource(resource);
                    }
                } catch (EntitlementException eE) {
                    debug.error("Error filtering application query CREST request", eE);
                    handler.handleError(resourceErrorHandler.handleError(context, request, eE));
                    return;
                }

                handler.handleResult(result);
            }

            @Override
            public void handleError(ResourceException error) {
                handler.handleError(error);
            }

        });
    }

    /**
     * Transforms the application result such that its resource types are removed and a single set of  actions and
     * resources are represented instead. The set of actions and resources are a union of their respective parts from
     * the associated resource types.
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
    public void filterRead(final ServerContext context, final ReadRequest request,
                           final ResultHandler<Resource> handler, final RequestHandler next) {
        // Forward onto next handler.
        next.handleRead(context, request, new TransformationHandler(handler, context));
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

    /*
     * Operation not currently supported. If the destination resource handler provides an implementation to this method
     * an appropriate implementation will need to be considered here also.
     */
    @Override
    public void filterAction(ServerContext context, ActionRequest request,
                             ResultHandler<JsonValue> handler, RequestHandler next) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Given the json representation of an application swaps out the resource type UUIDs for a set of actions and
     * resources that is the union of actions and resources represented by the associated resource types.
     *
     * @param jsonValue
     *         application json
     * @param callingSubject
     *         the calling subject
     * @param realm
     *         the realm
     *
     * @throws EntitlementException
     *         should an error occur during transformation
     */
    private void transformJson(
            final JsonValue jsonValue, final Subject callingSubject, final String realm) throws EntitlementException {

        final Map<String, Boolean> actions = new HashMap<String, Boolean>();
        final Set<String> resources = new HashSet<String>();

        final Set<String> resourceTypeUuids = jsonValue
                .get(RESOURCE_TYPE_UUIDS)
                .required()
                .asSet(String.class);

        for (String resourceTypeUuid : resourceTypeUuids) {
            final ResourceType resourceType = resourceTypeService
                    .getResourceType(callingSubject, realm, resourceTypeUuid);

            if (resourceType == null) {
                throw new EntitlementException(EntitlementException.NO_SUCH_RESOURCE_TYPE, resourceTypeUuid);
            }

            actions.putAll(resourceType.getActions());
            resources.addAll(resourceType.getPatterns());
        }

        jsonValue.remove(RESOURCE_TYPE_UUIDS);
        jsonValue.add(ACTIONS, actions);
        jsonValue.add(RESOURCES, resources);
    }

    /**
     * Retrieves the subject from the context.
     *
     * @param context
     *         the server context
     *
     * @return the calling subject
     */
    private Subject getContextSubject(ServerContext context) {
        if (!context.containsContext(SSOTokenContext.class)) {
            return null;
        }

        return context
                .asContext(SSOTokenContext.class)
                .getCallerSubject();
    }

    /**
     * Retrieves the realm from the context.
     *
     * @param context
     *         the server context
     *
     * @return the realm
     */
    private String getRealm(ServerContext context) {
        if (!context.containsContext(RealmContext.class)) {
            return null;
        }

        final String realm = context
                .asContext(RealmContext.class)
                .getResolvedRealm();

        return Strings.isNullOrEmpty(realm) ? "/" : realm;
    }

    /**
     * Inner class to handle the appropriate application json transformation.
     */
    private final class TransformationHandler implements ResultHandler<Resource> {

        private final ResultHandler<Resource> delegate;
        private final Subject callingSubject;
        private final String realm;

        TransformationHandler(
                final ResultHandler<Resource> delegate, final ServerContext context) {
            this.delegate = delegate;
            this.callingSubject = getContextSubject(context);
            this.realm = getRealm(context);
        }

        @Override
        public void handleResult(Resource result) {
            final JsonValue jsonValue = result.getContent();

            try {
                transformJson(jsonValue, callingSubject, realm);
            } catch (EntitlementException eE) {
                debug.error("Error filtering application CREST request", eE);
                delegate.handleError(resourceErrorHandler.handleError(eE));
                return;
            }

            delegate.handleResult(result);
        }

        @Override
        public void handleError(ResourceException error) {
            delegate.handleError(error);
        }

    }

    /**
     * Static inner class used to transform actions to query filters.
     */
    private static final class ActionsToQuery implements
            Function<Map.Entry<String, Boolean>, QueryFilter<SmsAttribute>, NeverThrowsException> {

        @Override
        public QueryFilter<SmsAttribute> apply(Map.Entry<String, Boolean> value) {
            final String actionValue = value.getKey() + "=" + value.getValue();
            return QueryFilter.equalTo(ResourceTypeSmsAttributes.ACTIONS, actionValue);
        }

    }

    /**
     * Static inner class used to transform resources to query filters.
     */
    private static final class ResourcesToQuery implements
            Function<String, QueryFilter<SmsAttribute>, NeverThrowsException> {

        @Override
        public QueryFilter<SmsAttribute> apply(String value) {
            return QueryFilter.equalTo(ResourceTypeSmsAttributes.PATTERNS, value);
        }

    }

}
