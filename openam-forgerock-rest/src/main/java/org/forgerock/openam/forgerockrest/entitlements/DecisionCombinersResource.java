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
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.shared.debug.Debug;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.security.auth.Subject;
import org.forgerock.json.fluent.JsonPointer;
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
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.rest.resource.SubjectContext;

/**
 * Allows for CREST-handling of stored {@link EntitlementCombiner}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 *
 * We return purely the name (title) of the classes which are registered, as
 * further knowledge of the workings of the combiners is stored as logic, which
 * we do not expose over JSON.
 */
public class DecisionCombinersResource implements CollectionResourceProvider {

    private final static String JSON_OBJ_TITLE = "title";

    private final Debug debug;
    private final EntitlementRegistry entitlementRegistry;

    /**
     * Guiced constructor.
     *
     * @param debug Debug instance
     */
    @Inject
    public DecisionCombinersResource(@Named("frRest") Debug debug, EntitlementRegistry entitlementRegistry) {
        this.debug = debug;
        this.entitlementRegistry = entitlementRegistry;
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
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
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
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        final Subject mySubject = getSubject(context, handler);

        if (mySubject == null) {
            return;
        }

        final Set<String> combinerTypeNames = new TreeSet<String>();
        List<JsonValue> combinerTypes = new ArrayList<JsonValue>();

        combinerTypeNames.addAll(entitlementRegistry.getCombinersShortNames());

        for (String combinerTypeName : combinerTypeNames) {
            final Class<? extends EntitlementCombiner> conditionClass =
                    entitlementRegistry.getCombinerType(combinerTypeName);

            if (conditionClass == null) {
                debug.error("Listed combiner short name not found: " + combinerTypeName);
                continue;
            }

            final JsonValue json = jsonify(combinerTypeName);

            if (json != null) {
                combinerTypes.add(json);
            }
        }

        int totalSize = combinerTypes.size();
        int pageSize = request.getPageSize();
        int offset = request.getPagedResultsOffset();

        if (pageSize > 0) {
            combinerTypes = combinerTypes.subList(offset, offset + pageSize);
        }

        final JsonPointer jp = new JsonPointer(JSON_OBJ_TITLE);

        for (JsonValue combinerTypeToReturn : combinerTypes) {

            final JsonValue resourceId = combinerTypeToReturn.get(jp);
            final String id = resourceId != null ? resourceId.toString() : null;

            final Resource resource = new Resource(id, "0", combinerTypeToReturn);

            handler.handleResource(resource);
        }

        //paginate
        if (pageSize > 0) {
            final String lastIndex = offset + pageSize > totalSize ?
                    String.valueOf(totalSize) : String.valueOf(offset + pageSize);
            handler.handleResult(new QueryResult(lastIndex, max(0, totalSize - (offset + pageSize))));
        } else {
            handler.handleResult(new QueryResult(null, -1));
        }

    }

    /**
     * Retrieves the {@link Subject} from the {@link ServerContext}.
     *
     * @param context The ServerContext containing an appropriate {@link SubjectContext}.
     * @param handler The handler via which to return any errors or issues.
     * @return The Subject object, or null.
     */
    private Subject getSubject(ServerContext context, ResultHandler handler) {

        final SubjectContext sc = context.asContext(SubjectContext.class);
        final Subject mySubject = sc.getCallerSubject();

        if (mySubject == null) {
            debug.error("Error retrieving combiner identification from request.");
            handler.handleError(ResourceException.getException(ResourceException.FORBIDDEN));
            return null;
        }

        return mySubject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler) {

        final Subject mySubject = getSubject(context, handler);

        if (mySubject == null) {
            return;
        }

        final Class<? extends EntitlementCombiner> combinerClass = entitlementRegistry.getCombinerType(resourceId);

        if (combinerClass == null) {
            debug.error("Requested combiner short name not found: " + resourceId);
            handler.handleError(ResourceException.getException(ResourceException.NOT_FOUND));
            return;
        }

        final JsonValue json = jsonify(resourceId);

        final Resource resource = new Resource(resourceId, "0", json);
        handler.handleResult(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Wraps the resource's name in a {@link JsonValue} object to return to the called.
     *
     * @param resourceId The ID of the resource to return
     * @return A JsonValue containing the title of the resourceId
     */
    protected JsonValue jsonify(String resourceId) {
            return JsonValue.json(JsonValue.object(JsonValue.field(JSON_OBJ_TITLE, resourceId)));
    }
}
