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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.util.promise.Promise;

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
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        final Set<String> combinerTypeNames = new TreeSet<>();
        List<ResourceResponse> combinerTypes = new ArrayList<>();

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        combinerTypeNames.addAll(entitlementRegistry.getCombinersShortNames());

        for (String combinerTypeName : combinerTypeNames) {
            final Class<? extends EntitlementCombiner> conditionClass =
                    entitlementRegistry.getCombinerType(combinerTypeName);

            if (conditionClass == null) {
                if (debug.warningEnabled()) {
                    debug.warning("DecisionCombinersResource :: QUERY by " + principalName +
                            ": Listed combiner short name not found: " + combinerTypeName);
                }
                continue;
            }

            final JsonValue json = jsonify(combinerTypeName);

            if (json != null) {
                if (json != null) {
                    String id = json.get(JSON_OBJ_TITLE).asString();
                    combinerTypes.add(newResourceResponse(id, null, json));
                }
            }
        }

        QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
        return QueryResponsePresentation.perform(handler, request, combinerTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {

        final Class<? extends EntitlementCombiner> combinerClass = entitlementRegistry.getCombinerType(resourceId);

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        if (combinerClass == null) {
            if (debug.errorEnabled()) {
                debug.error("DecisionCombinersResource :: READ by " + principalName +
                        ": Requested combiner short name not found: " + resourceId);
            }
            return new NotFoundException().asPromise();
        }

        final JsonValue json = jsonify(resourceId);

        final ResourceResponse resource = newResourceResponse(resourceId, String.valueOf(currentTimeMillis()), json);
        return newResultPromise(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
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
