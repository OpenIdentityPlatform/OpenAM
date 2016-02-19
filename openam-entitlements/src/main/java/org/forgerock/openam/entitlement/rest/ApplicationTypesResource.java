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

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeWrapper;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.SubjectAwareResource;
import org.forgerock.util.promise.Promise;

/**
 * Allows for CREST-handling of stored {@link ApplicationType}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 */
public class ApplicationTypesResource extends SubjectAwareResource {

    private final ApplicationTypeManagerWrapper typeManager;
    private final Debug debug;

    /**
     * Guiced-constructor.
     *
     * @param typeManager from which to locate application types
     * @param debug Debugger to use
     */
    @Inject
    public ApplicationTypesResource(final ApplicationTypeManagerWrapper typeManager, @Named("frRest") Debug debug) {
        this.typeManager = typeManager;
        this.debug = debug;
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
     * Unsupported by this endpoint.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return RestUtils.generateUnsupportedOperation();
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
     * Unsupported by this endpoint.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Reads the details of all {@link ApplicationType}s in the system.
     *
     * The user's {@link SecurityContext} must indicate they are a user with administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        //auth
        final Subject mySubject = getContextSubject(context);

        if (mySubject == null) {
            debug.error("ApplicationsTypesResource :: QUERY : Unknown Subject");
            return new InternalServerErrorException().asPromise();
        }

        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);

        //select
        final Set<String> appTypeNames =  typeManager.getApplicationTypeNames(mySubject);
        List<ApplicationTypeWrapper> appTypes = new LinkedList<>();

        for (String appTypeName : appTypeNames) {
            final ApplicationType type = typeManager.getApplicationType(mySubject, appTypeName);
            final ApplicationTypeWrapper wrap = new ApplicationTypeWrapper(type);
            if (type != null) {
                appTypes.add(wrap);
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("ApplicationTypesResource :: QUERY by " + principalName +
                            ": ApplicationType was not found: " + appTypeName);
                }
            }
        }

        final List<ResourceResponse> applicationsList = getResourceResponses(appTypes);

        QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
        return QueryResponsePresentation.perform(handler, request, applicationsList);
    }

    protected List<ResourceResponse> getResourceResponses(List<ApplicationTypeWrapper> appTypes) {
        final List<ResourceResponse> applicationsList = new ArrayList<>();

        for (ApplicationTypeWrapper entry : appTypes) {
            try {
                applicationsList.add(newResourceResponse(entry.getName(), null, entry.toJsonValue()));
            } catch (IOException e) {
                if (debug.warningEnabled()) {
                    debug.warning("ApplicationTypesResource :: getResourceResponses - Error applying " +
                            "jsonification to the ApplicationType class representation.", e);
                }
            }
        }
        return applicationsList;
    }

    /**
     * Reads the details of a single instance of an {@link ApplicationType} - the instance
     * referred to by the passed-in resourceId.
     *
     * The user's {@link SecurityContext} must indicate they are a user with administrator-level access.
     *
     * @param context {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {

        //auth
        final Subject mySubject = getContextSubject(context);

        if (mySubject == null) {
            debug.error("ApplicationsTypesResource :: READ : Unknown Subject");
            return new InternalServerErrorException().asPromise();
        }

        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);

        final ApplicationType applType = typeManager.getApplicationType(mySubject, resourceId);
        final ApplicationTypeWrapper wrap = new ApplicationTypeWrapper(applType);

        if (applType == null) {
            if (debug.errorEnabled()) {
                debug.error("ApplicationTypesResource :: READ by " + principalName +
                        ": Requested application type short name not found: " + resourceId);
            }
            return new NotFoundException().asPromise();
        }

        try {
            final ResourceResponse resource = newResourceResponse(resourceId,
                    String.valueOf(currentTimeMillis()), JsonValue.json(wrap.toJsonValue()));
            return newResultPromise(resource);
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("ApplicationTypesResource :: READ by " + principalName +
                        ": Could not jsonify class associated with defined Type: " + resourceId, e);
            }
            return new InternalServerErrorException().asPromise();
        }
    }
}
