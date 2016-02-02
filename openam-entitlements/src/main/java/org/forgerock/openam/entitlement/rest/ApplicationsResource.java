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
* Copyright 2014-2016 ForgeRock AS.
*/
package org.forgerock.openam.entitlement.rest;

import static com.sun.identity.entitlement.Application.NAME_SEARCH_ATTRIBUTE;
import static java.util.Collections.singleton;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils.getPrincipalNameFromSubject;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;

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
import org.forgerock.openam.entitlement.rest.query.QueryAttribute;
import org.forgerock.openam.entitlement.rest.query.QueryFilterVisitorAdapter;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationWrapper;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.rest.RealmAwareResource;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.debug.Debug;

/**
 * Endpoint for the ApplicationsResource.
 *
 * This endpoint supports the CRUDQ operations. It uses the
 * Jackson library and annotated wrapper classes to generate the
 * pojos that it will store. Similarly, when reading out from the
 * data store, we wrap the object to allow Jackson to do the serialization
 * leg for us.
 *
 */
public class ApplicationsResource extends RealmAwareResource {

    public static final String APPLICATION_QUERY_ATTRIBUTES = "ApplicationQueryAttributes";

    private static final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final ApplicationManagerWrapper appManager;
    private final ApplicationTypeManagerWrapper appTypeManagerWrapper;
    private final Map<String, QueryAttribute> queryAttributes;
    private final Debug debug;

    private final ExceptionMappingHandler<EntitlementException, ResourceException> exceptionMappingHandler;

    /**
     * @param debug Debug instance.
     * @param appManager Wrapper for the static {@link com.sun.identity.entitlement.ApplicationManager}. Cannot be null.
     * @param appTypeManagerWrapper instantiable version of the static ApplicationTypeManager class. Cannot be null.
     * @param queryAttributes Definition of Application fields that can be queried
     * @param exceptionMappingHandler Error handler to convert EntitlementExceptions to ResourceExceptions.
     */
    @Inject
    public ApplicationsResource(@Named("frRest") Debug debug, ApplicationManagerWrapper appManager,
                                ApplicationTypeManagerWrapper appTypeManagerWrapper,
                                @Named(ApplicationsResource.APPLICATION_QUERY_ATTRIBUTES)
                                Map<String, QueryAttribute> queryAttributes,
                                ExceptionMappingHandler<EntitlementException, ResourceException> exceptionMappingHandler) {

        Reject.ifNull(appManager);
        Reject.ifNull(appTypeManagerWrapper);

        this.debug = debug;
        this.appManager = appManager;
        this.appTypeManagerWrapper = appTypeManagerWrapper;
        this.queryAttributes = queryAttributes;
        this.exceptionMappingHandler = exceptionMappingHandler;
    }

    /**
     * Not Supported Action Collection Operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Not Supported Action Instance Operation.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Create an {@link Application}.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {

        final Subject subject = getContextSubject(context);
        if (subject == null) {
            debug.error("ApplicationsResource :: CREATE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(context);
        try {
            ApplicationWrapper applicationWrapper = createApplicationWrapper(request.getContent(), subject);
            ensureApplicationIdMatch(applicationWrapper, request.getNewResourceId());
            String applicationId = applicationWrapper.getName();
            validateApplicationId(applicationId);
            if (applicationExists(applicationId, realm, subject)) {
                throw new EntitlementException(EntitlementException.APPLICATION_ALREADY_EXISTS);
            }
            Application application = appManager.saveApplication(subject, realm, applicationWrapper.getApplication());
            return newResultPromise(newResourceResponse(application.getName(),
                    Long.toString(application.getLastModifiedDate()), applicationToJson(application)));
        } catch (EntitlementException e) {
            debug.error("ApplicationsResource :: CREATE by {}: Application creation failed. ",
                    getPrincipalNameFromSubject(subject), e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Abstracts out the createApplicationWrapper method so that we can easily test this class.
     *
     * @param jsonValue The JsonValue to create the wrapper from
     * @return An ApplicationWrapper, wrapping the Application represented by the JsonValue provided
     * @throws EntitlementException If there were errors writing the application to disk
     */
    protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue) throws EntitlementException {
        try {
            return mapper.readValue(jsonValue.toString(), ApplicationWrapper.class);
        } catch (IOException e) {
            throw new EntitlementException(EntitlementException.INVALID_CLASS, e.getCause().getMessage());
        }
    }

    /**
     * Creates an {@link ApplicationWrapper} to hold the {@link Application} object, after having deserialized it
     * via Jackson.
     *
     * @param jsonValue The JSON to deserialize
     * @param mySubject The subject authorizing the request
     * @return An ApplicationWrapper containing an Application, null
     * @throws EntitlementException If there were issues generating the application wrapper
     */
    protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject)
            throws EntitlementException {

        final ApplicationWrapper wrapp = createApplicationWrapper(jsonValue);

        final JsonValue appTypeValue = jsonValue.get("applicationType");

        if (appTypeValue.getObject() == null || appTypeValue.asString().isEmpty()
                || !wrapp.setApplicationType(mySubject, appTypeValue.asString())) {
            debug.error("ApplicationsResource.createApplicationWrapper() : " +
                        "Specified Application Type was not available.");
            throw new EntitlementException(EntitlementException.INVALID_APP_TYPE);
        }

        return wrapp;
    }

    /**
     * Creates an {@link ApplicationWrapper} to hold the {@link Application} object.
     * <p/>
     * This method provides an abstraction to aid testing.
     *
     * @param application
     *         The application
     * @param type
     *         The application type
     *
     * @return A new {@link ApplicationWrapper} wrapping the passed application
     */
    protected ApplicationWrapper createApplicationWrapper(Application application, ApplicationTypeManagerWrapper type) {
        return new ApplicationWrapper(application, type);
    }

    /**
     * Deletes an {@link Application} as per the {@link DeleteRequest}.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {

        final Subject subject = getContextSubject(context);
        if (subject == null) {
            debug.error("ApplicationsResource :: DELETE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(context);
        try {
            if (!applicationExists(resourceId, realm, subject)) {
                throw new EntitlementException(EntitlementException.NO_SUCH_APPLICATION, resourceId);
            }
            appManager.deleteApplication(subject, realm, resourceId);
            return newResultPromise(newResourceResponse(resourceId, "0", JsonValue.json(JsonValue.object())));
        } catch (EntitlementException e) {
            debug.error("ApplicationsResource :: DELETE by {}: Application failed to delete the resource specified. ",
                    getPrincipalNameFromSubject(subject), e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Not Supported Patch Operation.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Queries for a collection of resources.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        final Subject subject = getContextSubject(context);
        if (subject == null) {
            debug.error("ApplicationsResource :: UPDATE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(context);
        try {
            List<ResourceResponse> results = new ArrayList<>();
            final Set<String> applicationIds = query(request, subject, realm);
            for (String applicationId : applicationIds) {
                final Application application = appManager.getApplication(subject, realm, applicationId);
                if (application == null) {
                    debug.warning("Unable to find application {}", applicationId);
                    continue;
                }
                ApplicationWrapper wrapper = createApplicationWrapper(application, appTypeManagerWrapper);
                results.add(newResourceResponse(wrapper.getName(), null, wrapper.toJsonValue()));
            }
            QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
            return QueryResponsePresentation.perform(handler, request, results);
        } catch (EntitlementException e) {
            debug.error("ApplicationsResource :: QUERY by {}: Failed to query resource.",
                    getPrincipalNameFromSubject(subject), e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Reads an instance of an application.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {

        final Subject subject = getContextSubject(context);
        if (subject == null) {
            debug.error("ApplicationsResource :: READ : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(context);
        try {
            final Application application = appManager.getApplication(subject, realm, resourceId);
            if (application == null) {
                throw new EntitlementException(EntitlementException.APP_RETRIEVAL_ERROR, realm);
            }
            ApplicationWrapper applicationWrapper = createApplicationWrapper(application, appTypeManagerWrapper);
            return newResultPromise(newResourceResponse(resourceId,
                    Long.toString(application.getLastModifiedDate()), applicationWrapper.toJsonValue()));
        } catch (EntitlementException e) {
            debug.error("ApplicationsResource :: READ by {}: Application failed to retrieve the resource specified.",
                    getPrincipalNameFromSubject(subject), e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Updates an existing {@link Application}.
     * The resourceId is the name of the application to update.
     * The new Application may alter this name, but doing so will mean the original
     * resourceId will no longer reference the new Application.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {

        final Subject subject = getContextSubject(context);
        if (subject == null) {
            debug.error("ApplicationsResource :: UPDATE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(context);
        try {
            ApplicationWrapper applicationWrapper = createApplicationWrapper(request.getContent(), subject);
            ensureApplicationIdMatch(applicationWrapper, resourceId);
            if (!applicationExists(resourceId, realm, subject)) {
                throw new EntitlementException(EntitlementException.NOT_FOUND, resourceId);
            }
            Application application = appManager.saveApplication(subject, realm, applicationWrapper.getApplication());
            return newResultPromise(newResourceResponse(application.getName(),
                    Long.toString(application.getLastModifiedDate()), applicationToJson(application)));
        } catch (EntitlementException e) {
            debug.error("ApplicationsResource :: UPDATE by {}: Error performing update operation.",
                    getPrincipalNameFromSubject(subject), e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    /**
     * Query-based wrapper for the method {@link ApplicationManagerWrapper#search(Subject, String, Set)}.
     *
     * @param request the query request.
     * @param subject The subject authorizing the update - will be validated for permission.
     * @param realm The realm from which to gather the {@link Application} names.
     * @return the names of those Applications that match the query.
     * @throws EntitlementException if an error occurs or the query is invalid.
     * @since 12.0.0
     */
    Set<String> query(QueryRequest request, Subject subject, String realm) throws EntitlementException {

        QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
        if (queryFilter == null) {
            // Return everything
            queryFilter = QueryFilter.alwaysTrue();
        }

        try {
            Set<SearchFilter> searchFilters = queryFilter.accept(
                    new ApplicationQueryBuilder(queryAttributes), new HashSet<SearchFilter>());
            return appManager.search(subject, realm, searchFilters);

        } catch (UnsupportedOperationException ex) {
            throw new EntitlementException(EntitlementException.INVALID_SEARCH_FILTER, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new EntitlementException(EntitlementException.INVALID_VALUE, ex.getMessage());
        }
    }

    private boolean applicationExists(String applicationId, String realm, Subject subject) throws EntitlementException {
        SearchFilter searchFilter = new SearchFilter(NAME_SEARCH_ATTRIBUTE, applicationId);
        Set<String> applicationIds = appManager.search(subject, realm, singleton(searchFilter));
        return isNotEmpty(applicationIds);
    }

    private void ensureApplicationIdMatch(ApplicationWrapper applicationWrapper, String resourceId)
            throws EntitlementException {

        String applicationId = applicationWrapper.getName();
        if (applicationId != null && resourceId != null) {
            if (!applicationId.equals(resourceId)) {
                debug.error("ApplicationsResource :: Resource name and JSON body name do not match.");
                throw new EntitlementException(EntitlementException.APPLICATION_NAME_MISMATCH);
            }
        }

        if (isBlank(applicationId)) {
            applicationWrapper.setName(resourceId);
        }
    }

    private void validateApplicationId(String applicationId) throws EntitlementException {
        if (applicationId == null) {
            throw new EntitlementException(EntitlementException.MISSING_APPLICATION_NAME);
        }
        // OPENAM-5031
        // This is a bad solution and should be rewritten when we have time.  This code rejects anything in the
        // name that when encoded differs from the original.  So, for instance "+" becomes "\+".
        // What we should do is to encode the name for storage purposes, and decode it before presentation to the
        // user.
        if (!applicationId.equals(DN.escapeAttributeValue(applicationId))) {
            throw new EntitlementException(EntitlementException.INVALID_VALUE, "policy name \"" + applicationId + "\"");
        }
    }

    private JsonValue applicationToJson(Application application) throws EntitlementException {
        return createApplicationWrapper(application, appTypeManagerWrapper).toJsonValue();
    }

    /**
     * Converts a set of CREST {@link QueryFilter} into a set of entitlement {@link SearchFilter}.
     *
     * @since 12.0.0
     */
    private static final class ApplicationQueryBuilder extends QueryFilterVisitorAdapter {

        ApplicationQueryBuilder(Map<String, QueryAttribute> queryAttributes) {
            super("application", queryAttributes);
        }

        @Override
        public Set<SearchFilter> visitEqualsFilter(Set<SearchFilter> filters, JsonPointer field,
                                                   Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.EQUALS_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitGreaterThanFilter(Set<SearchFilter> filters, JsonPointer field,
                                                        Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.GREATER_THAN_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitGreaterThanOrEqualToFilter(Set<SearchFilter> filters, JsonPointer field,
                                                                 Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.GREATER_THAN_OR_EQUAL_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitLessThanFilter(Set<SearchFilter> filters, JsonPointer field,
                                                     Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.LESS_THAN_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitLessThanOrEqualToFilter(Set<SearchFilter> filters, JsonPointer field,
                                                              Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.LESS_THAN_OR_EQUAL_OPERATOR, valueAssertion));
            return filters;
        }
    }
}
