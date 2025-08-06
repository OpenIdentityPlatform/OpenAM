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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CONDITION_TYPES_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_404_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.forgerock.openam.entitlement.conditions.environment.IPv6Condition;
import org.forgerock.openam.entitlement.rest.model.json.JsonEntitlementConditionModule;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.LogicalCondition;
import com.sun.identity.shared.debug.Debug;

/**
 * Allows for CREST-handling of stored {@link EntitlementCondition}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 */
@CollectionProvider(
        details = @Handler(
                title = CONDITION_TYPES_RESOURCE + TITLE,
                description = CONDITION_TYPES_RESOURCE + DESCRIPTION,
                mvccSupported = false,
                resourceSchema = @Schema(schemaResource = "ConditionTypesResource.schema.json")),
        pathParam = @Parameter(
                name = "resourceId",
                type = "string",
                description = CONDITION_TYPES_RESOURCE + PATH_PARAM + DESCRIPTION))
public class ConditionTypesResource {

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
     * {@inheritDoc}
     *
     * Uses the {@link EntitlementRegistry} to locate the {@link EntitlementCondition}s to return.
     *
     * Looks up all the names of conditions registered in the system, and then returns each one to the
     * result handler having determined its schema and jsonified it.
     */
    @Query(operationDescription = @Operation(
            description = CONDITION_TYPES_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
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
    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 404,
                            description = CONDITION_TYPES_RESOURCE + ERROR_404_DESCRIPTION)},
            description = CONDITION_TYPES_RESOURCE + READ_DESCRIPTION))
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
