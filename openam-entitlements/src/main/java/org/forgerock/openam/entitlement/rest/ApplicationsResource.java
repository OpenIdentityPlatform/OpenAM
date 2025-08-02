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
* Portions Copyrighted 2019 Open Source Solution Technology Corporation.
* Portions copyright 2025 3A Systems LLC.
*/
package org.forgerock.openam.entitlement.rest;

import static com.sun.identity.entitlement.Application.NAME_ATTRIBUTE;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.CONFLICT;
import static org.forgerock.json.resource.ResourceException.FORBIDDEN;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils.getPrincipalNameFromSubject;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.APPLICATIONS_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_400_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_401_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_403_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_404_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_409_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.forgerock.api.annotations.Queries;
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
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationWrapper;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.rest.RealmAwareResource;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.query.QueryByStringFilterConverter;
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
@CollectionProvider(
    details=@Handler(
        title = APPLICATIONS_RESOURCE + TITLE,
        description = APPLICATIONS_RESOURCE + DESCRIPTION,
        mvccSupported = false,
        resourceSchema = @Schema(schemaResource = "ApplicationsResource.schema.json")
    ),
    pathParam = @Parameter(
        name = "applicationName",
        type = "string",
        description = APPLICATIONS_RESOURCE + PATH_PARAM + DESCRIPTION
    )
)
public class ApplicationsResource extends RealmAwareResource {

    public static final int UNAUTHORIZED = 401;

    private static final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ApplicationTypeManagerWrapper appTypeManagerWrapper;
    private final Debug debug;

    private final ExceptionMappingHandler<EntitlementException, ResourceException> exceptionMappingHandler;

    /**
     * @param debug Debug instance.
     * @param applicationServiceFactory Application service factory responsible for creating the application service.
     * @param appTypeManagerWrapper instantiable version of the static ApplicationTypeManager class. Cannot be null.
     * @param exceptionMappingHandler Error handler to convert EntitlementExceptions to ResourceExceptions.
     */
    @Inject
    public ApplicationsResource(@Named("frRest") Debug debug, ApplicationServiceFactory applicationServiceFactory,
            ApplicationTypeManagerWrapper appTypeManagerWrapper,
            ExceptionMappingHandler<EntitlementException, ResourceException> exceptionMappingHandler) {

        Reject.ifNull(appTypeManagerWrapper);

        this.debug = debug;
        this.applicationServiceFactory = applicationServiceFactory;
        this.appTypeManagerWrapper = appTypeManagerWrapper;
        this.exceptionMappingHandler = exceptionMappingHandler;
    }

    private ApplicationService appService(Subject subject, String realm) {
        return applicationServiceFactory.create(subject, realm);
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
    @Create(
        operationDescription = @Operation(
            description = APPLICATIONS_RESOURCE + CREATE_DESCRIPTION,
            errors = {
                @ApiError(
                    code = BAD_REQUEST,
                    description = APPLICATIONS_RESOURCE + "create." + ERROR_400_DESCRIPTION
                ),
                @ApiError(
                    code = UNAUTHORIZED,
                    description = APPLICATIONS_RESOURCE + ERROR_401_DESCRIPTION
                ),
                @ApiError(
                    code = CONFLICT,
                    description = APPLICATIONS_RESOURCE + ERROR_409_DESCRIPTION
                )
            }
        )
    )
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {

        final Subject subject = getContextSubject(context);
        if (subject == null) {
            debug.error("ApplicationsResource :: CREATE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(context);
        String applicationId = null;
        try {
            ApplicationWrapper applicationWrapper = createApplicationWrapper(request.getContent(), subject);
            ensureApplicationIdMatch(applicationWrapper, request.getNewResourceId());
            applicationId = applicationWrapper.getName();
            validateApplicationId(applicationId);
            if (applicationExists(applicationId, realm, subject)) {
                throw new EntitlementException(EntitlementException.APPLICATION_ALREADY_EXISTS);
            }
            Application application = appService(subject, realm).saveApplication(applicationWrapper.getApplication());
            return newResultPromise(newResourceResponse(application.getName(),
                    Long.toString(application.getLastModifiedDate()), applicationToJson(application)));
        } catch (EntitlementException e) {
            debug.error("ApplicationsResource :: CREATE by {}: Application creation failed. {}",
                    getPrincipalNameFromSubject(subject), applicationId, e);
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
    @Delete(
        operationDescription = @Operation(
            description = APPLICATIONS_RESOURCE + DELETE_DESCRIPTION,
            errors = {
                @ApiError(
                    code = UNAUTHORIZED,
                    description = APPLICATIONS_RESOURCE + ERROR_401_DESCRIPTION
                ),
                @ApiError(
                    code = NOT_FOUND,
                    description = APPLICATIONS_RESOURCE + ERROR_404_DESCRIPTION
                )
            }
        )
    )
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
            appService(subject, realm).deleteApplication(resourceId);
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
    @Query(
        operationDescription = @Operation(
            description = APPLICATIONS_RESOURCE + QUERY_DESCRIPTION
        ),
        type = QueryType.FILTER,
        queryableFields = "*"
    )
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        final Subject subject = getContextSubject(context);
        if (subject == null) {
            debug.error("ApplicationsResource :: UPDATE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(context);
        QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
        if (queryFilter == null) {
            // Return everything
            queryFilter = QueryFilter.alwaysTrue();
        }
        QueryFilter<String> stringQueryFilter = queryFilter.accept(new QueryByStringFilterConverter(), null);
        try {
            Set<Application> applications = appService(subject, realm).search(stringQueryFilter);
            List<ResourceResponse> results = new ArrayList<>();
            for (Application application : applications) {
                results.add(newResourceResponse(application.getName(), null, applicationToJson(application)));
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
    @Read(
        operationDescription = @Operation(
            description = APPLICATIONS_RESOURCE + READ_DESCRIPTION,
            errors = {
                @ApiError(
                    code = UNAUTHORIZED,
                    description = APPLICATIONS_RESOURCE + ERROR_401_DESCRIPTION
                ),
                @ApiError(
                    code = NOT_FOUND,
                    description = APPLICATIONS_RESOURCE + ERROR_404_DESCRIPTION
                )
            }
        )
    )
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
            final Application application = appService(subject, realm).getApplication(resourceId);
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
    @Update(
        operationDescription = @Operation(
            description = APPLICATIONS_RESOURCE + UPDATE_DESCRIPTION,
            errors = {
                @ApiError(
                    code = BAD_REQUEST,
                    description = APPLICATIONS_RESOURCE + "update." + ERROR_400_DESCRIPTION
                ),
                @ApiError(
                    code = UNAUTHORIZED,
                    description = APPLICATIONS_RESOURCE + ERROR_401_DESCRIPTION
                ),
                @ApiError(
                    code = FORBIDDEN,
                    description = APPLICATIONS_RESOURCE + ERROR_403_DESCRIPTION
                ),
                @ApiError(
                    code = NOT_FOUND,
                    description = APPLICATIONS_RESOURCE + ERROR_404_DESCRIPTION
                )
            }
        )
    )
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
            Application application = appService(subject, realm).saveApplication(applicationWrapper.getApplication());
            return newResultPromise(newResourceResponse(application.getName(),
                    Long.toString(application.getLastModifiedDate()), applicationToJson(application)));
        } catch (EntitlementException e) {
            debug.error("ApplicationsResource :: UPDATE by {}: Error performing update operation.",
                    getPrincipalNameFromSubject(subject), e);
            return exceptionMappingHandler.handleError(context, request, e).asPromise();
        }
    }

    private boolean applicationExists(String applicationId, String realm, Subject subject) throws EntitlementException {
        return isNotEmpty(appService(subject, realm).search(QueryFilter.equalTo(NAME_ATTRIBUTE, applicationId)));
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
            throw new EntitlementException(EntitlementException.INVALID_APPLICATION_ID);
        }
    }

    private JsonValue applicationToJson(Application application) throws EntitlementException {
        return createApplicationWrapper(application, appTypeManagerWrapper).toJsonValue();
    }
}
