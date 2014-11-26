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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryFilterVisitorAdapter;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationWrapper;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.util.Reject;

import static org.apache.commons.lang.StringUtils.isNotBlank;

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

    private static final ObjectMapper mapper = new ObjectMapper();
    private final ApplicationManagerWrapper appManager;
    private final ApplicationTypeManagerWrapper appTypeManagerWrapper;
    private final Map<String, QueryAttribute> queryAttributes;
    private final Debug debug;

    private final ResourceErrorHandler<EntitlementException> resourceErrorHandler;

    /**
     * @param debug Debug instance.
     * @param appManager Wrapper for the static {@link com.sun.identity.entitlement.ApplicationManager}. Cannot be null.
     * @param appTypeManagerWrapper instantiable version of the static ApplicationTypeManager class. Cannot be null.
     * @param queryAttributes Definition of Application fields that can be queried
     * @param resourceErrorHandler Error handler to convert EntitlementExceptions to ResourceExceptions.
     */
    @Inject
    public ApplicationsResource(@Named("frRest") Debug debug, ApplicationManagerWrapper appManager,
                                ApplicationTypeManagerWrapper appTypeManagerWrapper,
                                @Named(ApplicationsResource.APPLICATION_QUERY_ATTRIBUTES)
                                Map<String, QueryAttribute> queryAttributes,
                                ResourceErrorHandler<EntitlementException> resourceErrorHandler) {

        Reject.ifNull(appManager);
        Reject.ifNull(appTypeManagerWrapper);

        this.debug = debug;
        this.appManager = appManager;
        this.appTypeManagerWrapper = appTypeManagerWrapper;
        this.queryAttributes = queryAttributes;
        this.resourceErrorHandler = resourceErrorHandler;

        mapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Not Supported Action Collection Operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Not Supported Action Instance Operation.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Create an {@link Application}.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {

        //auth
        final Subject callingSubject = getContextSubject(context);

        if (callingSubject == null) {
            debug.error("ApplicationsResource :: CREATE : Unknown Subject");
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        final String realm = getRealm(context);

        //select
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);
        final JsonValue creationRequest = request.getContent();

        final ApplicationWrapper wrapp;
        final Application previousApp;

        try {
            wrapp = createApplicationWrapper(creationRequest, callingSubject);

            if (!realm.equals(wrapp.getApplication().getRealm())) {
                throw new EntitlementException(EntitlementException.INVALID_APP_REALM,
                                new String[] { wrapp.getApplication().getRealm(), realm });
            }

            String wrappName = wrapp.getName();
            String newResourceId = request.getNewResourceId();
            if (wrappName == null) {
                wrapp.setName(newResourceId);
            }
            if (isNotBlank(newResourceId) && !newResourceId.equals(wrappName)) {
                debug.error("ApplicationsResource :: CREATE : Resource name and JSON body name do not match.");
                throw new EntitlementException(EntitlementException.APPLICATION_NAME_MISMATCH);
            }

            // OPENAM-5031
            // This is a bad solution and should be rewritten when we have time.  This code rejects anything in the
            // name that when encoded differs from the original.  So, for instance "+" becomes "\+".
            // What we should do is to encode the name for storage purposes, and decode it before presentation to the
            // user.
            String appName = wrapp.getApplication().getName();
            if (!appName.equals(DN.escapeAttributeValue(appName))) {
                throw new EntitlementException(EntitlementException.INVALID_VALUE,
                        new Object[]{"policy name \"" + appName + "\""});
            }

            previousApp = appManager.getApplication(callingSubject, realm, appName);

            if (previousApp != null) { //return conflict
                throw new EntitlementException(EntitlementException.APPLICATION_ALREADY_EXISTS);
            }

            appManager.saveApplication(callingSubject, wrapp.getApplication());
            Application savedApp = appManager.getApplication(callingSubject, realm, appName);
            ApplicationWrapper savedAppWrapper = createApplicationWrapper(savedApp, appTypeManagerWrapper);

            final Resource resource = new Resource(savedAppWrapper.getName(),
                    Long.toString(savedAppWrapper.getLastModifiedDate()), savedAppWrapper.toJsonValue());
            if (debug.messageEnabled()) {
                debug.message("ApplicationsResource :: CREATE by " + principalName +
                        ": for Application: " + wrapp.getName());
            }
            handler.handleResult(resource);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ApplicationsResource :: CREATE by " + principalName +
                        ": Application creation failed. ", e);
            }
            handler.handleError(resourceErrorHandler.handleError(request, e));
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
            if (debug.errorEnabled()) {
                debug.error("ApplicationsResource.createApplicationWrapper() : " +
                        "Specified Application Type was not available.");
            }
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
     * @param handler {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                              ResultHandler<Resource> handler) {

        //auth
        final Subject callingSubject = getContextSubject(context);

        if (callingSubject == null) {
            debug.error("ApplicationsResource :: DELETE : Unknown Subject");
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        final String realm = getRealm(context);
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);

        try {
            Application oldApp = appManager.getApplication(callingSubject, realm, resourceId);

            if (oldApp == null) {
                throw new EntitlementException(EntitlementException.NO_SUCH_APPLICATION,
                                new String[] { resourceId });
            }

            appManager.deleteApplication(callingSubject, realm, resourceId);

            final Resource resource = new Resource(resourceId, "0", JsonValue.json(JsonValue.object()));
            handler.handleResult(resource);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ApplicationsResource :: DELETE by " + principalName +
                        ": Application failed to delete the resource specified. ", e);
            }
            handler.handleError(resourceErrorHandler.handleError(request, e));
        }

    }

    /**
     * Not Supported Patch Operation.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Queries for a collection of resources.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        //auth
        final Subject mySubject = getContextSubject(context);

        if (mySubject == null) {
            debug.error("ApplicationsResource :: UPDATE : Unknown Subject");
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        //select
        final String realm = getRealm(context);
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);

        List<ApplicationWrapper> apps = new LinkedList<ApplicationWrapper>();
        int remaining = 0;

        try {
            final Set<String> appNames = query(request, mySubject, realm);

            for (String appName : appNames) {
                final Application application = appManager.getApplication(mySubject, realm, appName);

                if (application == null) {
                    debug.warning("Unable to find application " + appName);
                    continue;
                }

                apps.add(createApplicationWrapper(application, appTypeManagerWrapper));
            }

            handler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);

            if (apps.size() > 0) {
                remaining = apps.size();
                for (ApplicationWrapper app : apps) {
                    boolean keepGoing = handler.handleResource(
                            new Resource(app.getName(), Long.toString(app.getLastModifiedDate()), app.toJsonValue()));
                    remaining--;
                    if (debug.messageEnabled()) {
                        debug.message("ApplicationsResource :: QUERY by " + principalName +
                                ": Added resource to response: " + app.getName());
                    }
                    if (!keepGoing) {
                        break;
                    }
                }
            }
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ApplicationsResource :: QUERY by " + principalName +
                        ": Failed to query resource.", e);

            }
            handler.handleError(resourceErrorHandler.handleError(request, e));
            return;
        }

        handler.handleResult(new QueryResult(null, remaining));
    }

    /**
     * Reads an instance of an application.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler) {

        final Subject mySubject = getContextSubject(context);

        if (mySubject == null) {
            debug.error("ApplicationsResource :: READ : Unknown Subject");
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        final String realm = getRealm(context);
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);

        try {
            final Application app = appManager.getApplication(mySubject, realm, resourceId);

            if (app == null) {
                throw new EntitlementException(EntitlementException.APP_RETRIEVAL_ERROR, new String[] { realm });
            }

            final ApplicationWrapper wrapp = createApplicationWrapper(app, appTypeManagerWrapper);

            final Resource resource = new Resource(resourceId, Long.toString(app.getLastModifiedDate()),
                    wrapp.toJsonValue());
            handler.handleResult(resource);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ApplicationsResource :: READ by " + principalName +
                        ": Application failed to retrieve the resource specified.", e);
            }
            handler.handleError(resourceErrorHandler.handleError(request, e));
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
     * @param handler {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler) {

        final Subject mySubject = getContextSubject(context);

        if (mySubject == null) {
            debug.error("ApplicationsResource :: UPDATE : Unknown Subject");
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);

        final ApplicationWrapper wrapp;
        final Application oldApplication;

        try {
            wrapp = createApplicationWrapper(request.getContent(), mySubject);

            if (wrapp.getName() == null) {
                wrapp.setName(resourceId);
            }

            oldApplication = appManager.getApplication(mySubject, getRealm(context), resourceId);

            if (oldApplication == null) {
                throw new EntitlementException(EntitlementException.NOT_FOUND, new String[] { resourceId });
            }

            if (!getRealm(context).equals(wrapp.getApplication().getRealm())) {
                 throw new EntitlementException(EntitlementException.INVALID_APP_REALM,
                                new String[] { wrapp.getApplication().getRealm(), getRealm(context) });
            }

            if (!resourceId.equals(wrapp.getName()) && //return conflict
                    appManager.getApplication(mySubject, getRealm(context), wrapp.getName()) != null) {
                throw new EntitlementException(EntitlementException.APPLICATION_ALREADY_EXISTS);
            }

            appManager.updateApplication(oldApplication, wrapp.getApplication(), mySubject);

            final Resource resource = new Resource(wrapp.getName(),
                    Long.toString(wrapp.getApplication().getLastModifiedDate()), wrapp.toJsonValue());

            handler.handleResult(resource);

        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ApplicationsResource :: UPDATE by " + principalName +
                        ": Error performing update operation.", e);
            }
            handler.handleError(resourceErrorHandler.handleError(request, e));
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

        QueryFilter queryFilter = request.getQueryFilter();
        if (queryFilter == null) {
            // Return everything
            queryFilter = QueryFilter.alwaysTrue();
        }

        try {
            Set<SearchFilter> searchFilters = queryFilter.accept(
                    new ApplicationQueryBuilder(queryAttributes),
                    new HashSet<SearchFilter>());
            return appManager.search(subject, realm, searchFilters);

        } catch (UnsupportedOperationException ex) {
            throw new EntitlementException(EntitlementException.INVALID_SEARCH_FILTER, new Object[]{ ex.getMessage() });
        } catch (IllegalArgumentException ex) {
            throw new EntitlementException(EntitlementException.INVALID_VALUE, new Object[] { ex.getMessage() });
        }
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
            filters.add(comparison(field.leaf(), SearchFilter.Operator.EQUAL_OPERATOR, valueAssertion));
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
            // Treat as greater-than (both are >= in the underlying implementation)
            return visitGreaterThanFilter(filters, field, valueAssertion);
        }

        @Override
        public Set<SearchFilter> visitLessThanFilter(Set<SearchFilter> filters, JsonPointer field,
                                                     Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.LESSER_THAN_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitLessThanOrEqualToFilter(Set<SearchFilter> filters, JsonPointer field,
                                                              Object valueAssertion) {
            return visitLessThanFilter(filters, field, valueAssertion);
        }

    }
}
