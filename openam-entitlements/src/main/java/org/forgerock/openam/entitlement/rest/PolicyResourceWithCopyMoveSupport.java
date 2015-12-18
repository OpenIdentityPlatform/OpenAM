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

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.DecoratedCollectionResourceProvider;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds additional behaviour to the existing {@link PolicyResource} to support the move and copy of policies.
 *
 * @since 13.0.0
 */
final class PolicyResourceWithCopyMoveSupport extends DecoratedCollectionResourceProvider {

    private final Router router;

    @Inject
    PolicyResourceWithCopyMoveSupport(@Named("PolicyResource") CollectionResourceProvider wrappedResource,
            @Named("CrestRealmRouter") Router router) {
        super(wrappedResource);
        Reject.ifNull(router);
        this.router = router;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest actionRequest) {
        String actionString = actionRequest.getAction();
        PolicyAction action = PolicyAction.getAction(actionString);

        try {
            switch (action) {
            case MOVE:
            case COPY:
                return Promises.newResultPromise(copyOrMovePoliciesByApplication(context, actionRequest, action));
            default:
                return super.actionCollection(context, actionRequest);
            }
        } catch (ResourceException rE) {
            return rE.asPromise();
        }

    }

    private ActionResponse copyOrMovePoliciesByApplication(Context context,
            ActionRequest request, PolicyAction copyOrMoveAction) throws ResourceException {

        JsonValue payload = request.getContent();
        JsonValue from = payload.get("from");
        JsonValue to = payload.get("to");

        if (from.isNull()) {
            throw new BadRequestException("from definition is missing");
        }

        if (!from.isDefined("application")) {
            throw new BadRequestException("from application definition is missing");
        }

        String sourceApplication = from
                .get("application")
                .asString();

        if (to.isNull()) {
            throw new BadRequestException("to definition is missing");
        }

        String sourceRealm = RealmContext.getRealm(context);

        String destinationRealm = to
                .get("realm")
                .defaultTo(sourceRealm)
                .asString();

        String destinationApplication = to
                .get("application")
                .defaultTo(sourceApplication)
                .asString();

        JsonValue resourceTypeMapping = payload
                .get("resourceTypeMapping")
                .defaultTo(Collections.emptyMap());

        String namePostfix = to
                .get("namePostfix")
                .defaultTo("")
                .asString();

        QueryRequest queryRequest = Requests.newQueryRequest("policies");
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("applicationName"), sourceApplication));

        final List<JsonValue> policies = new ArrayList<>();
        router.handleQuery(context, queryRequest, new QueryResourceHandler() {

            @Override
            public boolean handleResource(ResourceResponse resourceResponse) {
                policies.add(resourceResponse.getContent());
                return true;
            }

        }).getOrThrowUninterruptibly();

        JsonValue actionResponseContent = json(array());

        for (JsonValue policy : policies) {
            ActionResponse response = copyOrMoveGivenPolicy(context, policy, destinationRealm,
                    destinationApplication, namePostfix, resourceTypeMapping, copyOrMoveAction);
            actionResponseContent.add(response.getJsonContent().asMap());
        }

        return Responses.newActionResponse(actionResponseContent);
    }

    private ActionResponse copyOrMoveGivenPolicy(Context context, JsonValue policy, String destinationRealm,
            String destinationApplication, String namePostfix, JsonValue resourceTypeMapping,
            PolicyAction copyOrMoveAction) throws ResourceException {

        String name = policy.get("name").asString();
        String copiedName = name + namePostfix;

        String sourceResourceType = policy.get("resourceTypeUuid").asString();
        String destinationResourceType = resourceTypeMapping
                .get(sourceResourceType)
                .defaultTo(sourceResourceType)
                .asString();

        JsonValue newPayload = json(
                object(
                        field("to",
                                object(
                                        field("name", copiedName),
                                        field("realm", destinationRealm),
                                        field("application", destinationApplication),
                                        field("resourceType", destinationResourceType)))));

        String copyOrMoveActionName = copyOrMoveAction.name().toLowerCase();
        ActionRequest newActionRequest = Requests.newActionRequest("policies", name, copyOrMoveActionName);
        newActionRequest.setContent(newPayload);

        return router
                .handleAction(context, newActionRequest)
                .getOrThrowUninterruptibly();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(
            Context context, String resourceId, ActionRequest actionRequest) {

        String actionString = actionRequest.getAction();
        PolicyAction action = PolicyAction.getAction(actionString);

        try {
            switch (action) {
            case MOVE:
                return Promises.newResultPromise(movePolicy(context, resourceId, actionRequest));
            case COPY:
                return Promises.newResultPromise(copyPolicy(context, resourceId, actionRequest));
            default:
                return super.actionInstance(context, resourceId, actionRequest);
            }
        } catch (ResourceException rE) {
            return rE.asPromise();
        }
    }

    private ActionResponse movePolicy(
            Context context, String resourceId, ActionRequest request) throws ResourceException {
        ActionResponse copyResponse = copyPolicy(context, resourceId, request);
        DeleteRequest deleteRequest = Requests.newDeleteRequest("policies", resourceId);
        router.handleDelete(context, deleteRequest).getOrThrowUninterruptibly();
        return copyResponse;
    }

    private ActionResponse copyPolicy(
            Context context, String resourceId, ActionRequest request) throws ResourceException {
        String sourceRealm = RealmContext.getRealm(context);
        JsonValue payload = request.getContent().get("to");

        if (payload.isNull()) {
            throw new BadRequestException("to definition is missing");
        }

        String destinationRealm = payload
                .get("realm")
                .defaultTo(sourceRealm)
                .asString();

        ReadRequest readRequest = Requests.newReadRequest("policies", resourceId);
        JsonValue policy = router.handleRead(context, readRequest)
                .getOrThrowUninterruptibly()
                .getContent();

        String sourceApplication = policy.get("applicationName").asString();
        String sourceResourceType = policy.get("resourceTypeUuid").asString();

        String destinationApplication = payload
                .get("application")
                .defaultTo(sourceApplication)
                .asString();

        String destinationResourceTypeId = payload
                .get("resourceType")
                .defaultTo(sourceResourceType)
                .asString();

        String copiedName = payload
                .get("name")
                .defaultTo(resourceId)
                .asString();

        if (sourceRealm.equals(destinationRealm) && resourceId.equals(copiedName)) {
            throw new BadRequestException("policy name already exists within the realm");
        }

        policy.put("name", copiedName);
        policy.put("applicationName", destinationApplication);
        policy.put("resourceTypeUuid", destinationResourceTypeId);

        RealmContext updatedContext = new RealmContext(context);
        updatedContext.setOverrideRealm(destinationRealm);

        CreateRequest createRequest = Requests.newCreateRequest("policies", policy);
        JsonValue copiedPolicy = router.handleCreate(updatedContext, createRequest)
                .getOrThrowUninterruptibly()
                .getContent();

        return Responses.newActionResponse(copiedPolicy);
    }

}
