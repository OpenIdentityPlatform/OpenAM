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

import com.google.inject.name.Named;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import java.util.Set;
import javax.inject.Inject;
import javax.security.auth.Subject;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
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
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.rest.resource.SubjectContext;

/**
 * Allows for CREST-handling of stored {@link ApplicationType}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 */
public class ApplicationTypesResource implements CollectionResourceProvider {

    private static final String APPLICATION_TYPE_NAMES = "applicationTypeNames";
    private final Debug debug;

    /**
     * Guiced-constructor.
     *
     * @param debug Debugger to use
     */
    @Inject
    public ApplicationTypesResource(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
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
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        final SubjectContext sc = context.asContext(SubjectContext.class);
        final Subject mySubject = sc.getCallerSubject();

        if (mySubject == null) {
            debug.error("Error retrieving Subject identification from request.");
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        final Set<String> applTypeNames =  ApplicationTypeManager.getApplicationTypeNames(mySubject);
        final JsonValue applTypeNamesJson = jsonify(APPLICATION_TYPE_NAMES, applTypeNames);

        final Resource resource = new Resource(APPLICATION_TYPE_NAMES, Integer.toString(applTypeNames.hashCode()), applTypeNamesJson);
        handler.handleResource(resource);

        //we are only ever returning a single entry with an array of Strings. This won't require pagination.
        handler.handleResult(new QueryResult());
    }

    /**
     * Create a JSON object's representation of a set of Strings, returned in the form
     * of an array contained within an object whose name is supplied as the containerName
     * argument to this function.
     *
     * @param containerName The name of the Json array to return
     * @param arrayEntries The entries in the array
     * @return a {@link JsonValue} object representing the provided {@link Set}
     */
    private JsonValue jsonify(String containerName, Set<String> arrayEntries) {

        return JsonValue.json(JsonValue.object(JsonValue.field(containerName, arrayEntries)));

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
     * @param handler {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {

        final SubjectContext sc = context.asContext(SubjectContext.class);
        final Subject mySubject = sc.getCallerSubject();

        if (mySubject == null) {
            debug.error("Error retrieving Subject identification from request.");
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        final ApplicationType applType = ApplicationTypeManager.getAppplicationType(mySubject, resourceId);

        if (applType == null) {
            debug.error("Read failed on invalid ApplicationType.");
            handler.handleError(ResourceException.getException(ResourceException.NOT_FOUND));
            return;
        }

        try {
            final Resource resource = new Resource(resourceId, Integer.toString(applType.hashCode()), applType.toJsonValue());
            handler.handleResult(resource);
        } catch (EntitlementException e) {
            debug.message("ApplicationType could not locate class associated with defined Type.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }
    }

}
