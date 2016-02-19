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

import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.LogicalCondition;
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
import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.forgerock.openam.entitlement.conditions.environment.IPv6Condition;
import org.forgerock.openam.entitlement.rest.model.json.JsonEntitlementConditionModule;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Allows for CREST-handling of stored {@link EntitlementCondition}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 */
public class ConditionTypesResource implements CollectionResourceProvider {

    private final static String JSON_OBJ_TITLE = "title";
    private final static String JSON_OBJ_CONFIG = "config";
    private final static String JSON_OBJ_LOGICAL = "logical";

    private final static ObjectMapper mapper = new ObjectMapper().registerModule(new JsonEntitlementConditionModule() {
        @Override
        public void setupModule(SetupContext context) {
            context.setMixInAnnotations(IPv4Condition.class, IPvXConditionMixin.class);
            context.setMixInAnnotations(IPv6Condition.class, IPvXConditionMixin.class);
            super.setupModule(context);
        }
    });
    private final Debug debug;
    private final EntitlementRegistry entitlementRegistry;

    /**
     * Guiced constructor.
     *
     * @param debug Debug instance
     * @param entitlementRegistry from which to locate condition types. Cannot be null.
     */
    @Inject
    public ConditionTypesResource(@Named("frRest") Debug debug, EntitlementRegistry entitlementRegistry) {
        Reject.ifNull(entitlementRegistry);

        this.debug = debug;
        this.entitlementRegistry = entitlementRegistry;
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
     * {@inheritDoc}
     *
     * Uses the {@link EntitlementRegistry} to locate the {@link EntitlementCondition}s to return.
     *
     * Looks up all the names of conditions registered in the system, and then returns each one to the
     * result handler having determined its schema and jsonified it.
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        final Set<String> conditionTypeNames = new TreeSet<>();
        List<ResourceResponse> conditionTypes = new ArrayList<>();

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        conditionTypeNames.addAll(entitlementRegistry.getConditionsShortNames());

        for (String conditionTypeName : conditionTypeNames) {
            final Class<? extends EntitlementCondition> conditionClass =
                    entitlementRegistry.getConditionType(conditionTypeName);

            if (conditionClass == null) {
                if (debug.warningEnabled()) {
                    debug.warning("ConditionTypesResource :: QUERY by " + principalName +
                            ": Requested condition short name not found: " + conditionTypeName);
                }
                continue;
            }

            final JsonValue json = jsonify(conditionClass, conditionTypeName,
                    LogicalCondition.class.isAssignableFrom(conditionClass));

            if (json != null) {
                conditionTypes.add(newResourceResponse(json.get(JSON_OBJ_TITLE).asString(), null, json));
            }
        }

        QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
        return QueryResponsePresentation.perform(handler, request, conditionTypes);
    }

    /**
     * {@inheritDoc}
     *
     * Uses the {@link EntitlementRegistry} to locate the {@link EntitlementCondition} to return.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {

        final Class<? extends EntitlementCondition> conditionClass = entitlementRegistry.getConditionType(resourceId);

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        if (conditionClass == null) {
            if (debug.errorEnabled()) {
                debug.error("ConditionTypesResource :: READ by " + principalName +
                        ": Requested condition short name not found: " + resourceId);
            }
            return new NotFoundException().asPromise();
        }

        final JsonValue json = jsonify(conditionClass, resourceId,
                LogicalCondition.class.isAssignableFrom(conditionClass));

        final ResourceResponse resource = newResourceResponse(resourceId, String.valueOf(currentTimeMillis()), json);
        return newResultPromise(resource);
    }

    /**
     * Transforms a subclass of {@link EntitlementCondition} in to a JsonSchema representation.
     * This schema is then combined with the Condition's name (taken as the resourceId) and all this is
     * compiled together into a new {@link JsonValue} object until "title" and "config" fields respectively.
     *
     * @param conditionClass The class whose schema to produce.
     * @param resourceId The ID of the resource to return
     * @return A JsonValue containing the schema of the EntitlementCondition
     */
    private JsonValue jsonify(Class<? extends EntitlementCondition> conditionClass, String resourceId,
                                boolean logical) {
        try {

            final JsonSchema schema = mapper.generateJsonSchema(conditionClass);

            //this will remove the 'name' attribute from those conditions which incorporate it unnecessarily
            final JsonNode node = schema.getSchemaNode().get("properties");

            if (node instanceof ObjectNode) {
                final ObjectNode alter = (ObjectNode) node;
                alter.remove("name");
            }

            return JsonValue.json(JsonValue.object(
                    JsonValue.field(JSON_OBJ_TITLE, resourceId),
                    JsonValue.field(JSON_OBJ_LOGICAL, logical),
                    JsonValue.field(JSON_OBJ_CONFIG, schema)));

        } catch (JsonMappingException e) {
            if (debug.errorEnabled()) {
                debug.error("ConditionTypesResource :: JSONIFY - Error applying " +
                        "jsonification to the Condition class representation.", e);
            }
            return null;
        }
    }

    private abstract class IPvXConditionMixin {
        //Ignore just for conditiontypes query
        @JsonIgnore
        abstract List<String> getIpRange();
    }
}
