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
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
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
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationWrapper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.util.Reject;

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
public class ApplicationsResource extends SubjectAwareResource {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final ApplicationManagerWrapper appManager;
    private final ApplicationTypeManagerWrapper appTypeManagerWrapper;
    private final Debug debug;

    /**
     * @param debug Debug instance
     * @param appManager Wrapper for the static {@link com.sun.identity.entitlement.ApplicationManager}. Cannot be null.
     * @param appTypeManagerWrapper instantiable version of the static ApplicationTypeManager class. Cannot be null.
     */
    public ApplicationsResource(Debug debug, ApplicationManagerWrapper appManager,
                                ApplicationTypeManagerWrapper appTypeManagerWrapper) {

        Reject.ifNull(appManager);
        Reject.ifNull(appTypeManagerWrapper);

        this.debug = debug;
        this.appManager = appManager;
        this.appTypeManagerWrapper = appTypeManagerWrapper;

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
        final Subject subject = getContextSubject(context, handler);

        if (subject == null) {
            return;
        }

        //select
        final String realm = getRealm(context);

        final JsonValue creationRequest = request.getContent();

        final ApplicationWrapper wrapp;
        try {
            wrapp = createApplicationWrapper(creationRequest, subject, realm);
        } catch (IOException e) {
            debug.error("Application failed to create the resource specified.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        } catch (EntitlementException e) {
            debug.error("Application Type not correctly specified in request.", e);
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        try {
            appManager.saveApplication(subject, realm, wrapp.getApplication());
        } catch (EntitlementException e) {
            debug.error("Application failed to store the created resource.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        try {
            final Resource resource = new Resource(wrapp.getName(),
                    Long.toString(wrapp.getApplication().getLastModifiedDate()), wrapp.toJsonValue());
            handler.handleResult(resource);
        } catch (IOException e) {
            debug.error("Application failed to return the resource created.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

    }

    /**
     * Abstracts out the createApplicationWrapper method so that we can easily test this class.
     *
     * @param jsonValue The JsonValue to create the wrapper from
     * @return An ApplicationWrapper, wrapping the Application represented by the JsonValue provided
     * @throws IOException If there were errors creating the application
     */
    protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue) throws IOException {
        return mapper.readValue(jsonValue.toString(), ApplicationWrapper.class);
    }

    /**
     * Creates an {@link ApplicationWrapper} to hold the {@link Application} object, after having deserialized it
     * via Jackson.
     *
     * @param jsonValue The JSON to deserialize
     * @param mySubject The subject authorizing the request
     * @param realm The realm in which the request is occuring
     * @return An ApplicationWrapper containing an Application, null
     * @throws IOException If there were issues generating the
     */
    protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject, String realm)
            throws IOException, EntitlementException {

        final ApplicationWrapper wrapp = createApplicationWrapper(jsonValue);

        final JsonValue appTypeValue = jsonValue.get("applicationType");

        if (appTypeValue == null || appTypeValue.asString().isEmpty()
                || !wrapp.setApplicationType(mySubject, appTypeValue.asString())) {
            debug.error("Specified Application Type was not available.");
            throw new EntitlementException(EntitlementException.JSON_PARSE_ERROR);
        }

        wrapp.setRealm(realm);

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

        final Subject subject = getContextSubject(context, handler);

        if (subject == null) {
            return;
        }

        final String realm = getRealm(context);

        try {
            appManager.deleteApplication(subject, realm, resourceId);

            final Resource resource = new Resource(resourceId, "0", JsonValue.json(JsonValue.object()));
            handler.handleResult(resource);
        } catch (EntitlementException e) {
            debug.error("Application failed to delete the resource specified.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
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
        final Subject subject = getContextSubject(context, handler);

        if (subject == null) {
            return;
        }

        //select
        final String realm = getRealm(context);
        List<ApplicationWrapper> apps = new LinkedList<ApplicationWrapper>();

        try {
            final Set<String> appNames = appManager.getApplicationNames(subject, realm);

            for (String appName : appNames) {
                final Application application = appManager.getApplication(subject, realm, appName);
                apps.add(createApplicationWrapper(application, appTypeManagerWrapper));
            }
        } catch (EntitlementException e) {
            debug.error("Application failed to retrieve the resource specified.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        handler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);

        int remaining = 0;
        try {
            if (apps != null) {
                remaining = apps.size();
                for (ApplicationWrapper app : apps) {
                    boolean keepGoing = handler.handleResource(
                            new Resource(app.getName(), Long.toString(app.getLastModifiedDate()), app.toJsonValue()));
                    remaining--;
                    if (!keepGoing) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            debug.error("Unable to convert resource to JSON.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
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

        final Subject subject = getContextSubject(context, handler);

        if (subject == null) {
            return;
        }

        final String realm = getRealm(context);

        try {
            final Application app = appManager.getApplication(subject, realm, resourceId);
            final ApplicationWrapper wrapp = new ApplicationWrapper(app, appTypeManagerWrapper);

            final Resource resource = new Resource(resourceId, Long.toString(app.getLastModifiedDate()), wrapp.toJsonValue());
            handler.handleResult(resource);
        } catch (EntitlementException e) {
            debug.error("Application failed to retrieve the resource specified.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        } catch (IOException e) {
            debug.error("Error converting resource to JSON format.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
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

        final Subject subject = getContextSubject(context, handler);

        if (subject == null) {
            return;
        }

        final String realm = getRealm(context);

        final ApplicationWrapper wrapp;
        try {
            wrapp = createApplicationWrapper(request.getContent(), subject, realm);
        } catch (IOException e) {
            debug.error("Application Type not correctly specified in request.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        } catch (EntitlementException e) {
            debug.error("Application failed to create the resource specified.", e);
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        final Application oldApplication;
        try {
            oldApplication = appManager.getApplication(subject, realm, resourceId);
        } catch (EntitlementException e) {
            debug.error("Error retrieving Application to update.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        if (oldApplication == null) {
            handler.handleError(ResourceException.getException(ResourceException.NOT_FOUND));
            return;
        }

        try {
            appManager.updateApplication(oldApplication, wrapp.getApplication(), subject, realm);
        } catch (EntitlementException e) {
            debug.error("Unable to perform update operation.");
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        try {
            final Resource resource = new Resource(wrapp.getName(),
                    Long.toString(wrapp.getApplication().getLastModifiedDate()), wrapp.toJsonValue());
            handler.handleResult(resource);
        } catch (IOException e) {
            debug.error("Application failed to return the resource updated.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

    }

    /**
     * Retrieves the Realm from a provided {@link ServerContext}.
     *
     * @param context The request context
     * @return a String containing the name of the realm associated with this request.
     */
    private String getRealm(ServerContext context) {

        final RealmContext rc = context.asContext(RealmContext.class);

        if (rc.getRealm().equals("")) {
            return "/";
        }

        return rc.getRealm();
    }

}