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
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_404_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SUBJECT_TYPES_RESOURCE;
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
import org.forgerock.openam.entitlement.rest.model.json.JsonEntitlementConditionModule;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.LogicalSubject;
import com.sun.identity.shared.debug.Debug;

/**
 * Allows for CREST-handling of stored {@link EntitlementSubject}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 *
 * @see ConditionTypesResource
 */
@CollectionProvider(
        details = @Handler(
                title = SUBJECT_TYPES_RESOURCE + TITLE,
                description = SUBJECT_TYPES_RESOURCE + DESCRIPTION,
                mvccSupported = false,
                resourceSchema = @Schema(schemaResource = "SubjectTypesResource.schema.json")),
        pathParam = @Parameter(
                name = "resourceId",
                type = "string",
                description = SUBJECT_TYPES_RESOURCE + PATH_PARAM + DESCRIPTION))
public class SubjectTypesResource {

    private final static String JSON_OBJ_TITLE = "title";
    private final static String JSON_OBJ_LOGICAL = "logical";
    private final static String JSON_OBJ_CONFIG = "config";

    private final static ObjectMapper mapper = new ObjectMapper().registerModule(new JsonEntitlementConditionModule());
    private final Debug debug;
    private final EntitlementRegistry entitlementRegistry;

    /**
     * Guiced constructor.
     *
     * @param debug Debug instance
     * @param entitlementRegistry from which to locate subject types. Cannot be null.
     */
    @Inject
    public SubjectTypesResource(@Named("frRest") Debug debug, EntitlementRegistry entitlementRegistry) {
        Reject.ifNull(entitlementRegistry);

        this.debug = debug;
        this.entitlementRegistry = entitlementRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * Uses the {@link EntitlementRegistry} to locate the {@link EntitlementSubject}s to return.
     *
     * Looks up all the names of subjects registered in the system, and then returns each one to the
     * result handler having determined its schema and jsonified it.
     */
    @Query(operationDescription = @Operation(
            description = SUBJECT_TYPES_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        final Set<String> subjectTypeNames = new TreeSet<>();
        List<ResourceResponse> subjectTypes = new ArrayList<>();

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        subjectTypeNames.addAll(entitlementRegistry.getSubjectsShortNames());

        for (String subjectTypeName : subjectTypeNames) {
            final Class<? extends EntitlementSubject> subjectClass =
                    entitlementRegistry.getSubjectType(subjectTypeName);

            if (subjectClass == null) {
                if (debug.warningEnabled()) {
                    debug.warning("SubjectTypesResource :: QUERY by " + principalName +
                            ": Listed subject short name not found: " + subjectTypeName);
                }
                continue;
            }

            final JsonValue json = jsonify(subjectClass, subjectTypeName,
                    LogicalSubject.class.isAssignableFrom(subjectClass));

            if (json != null) {
                String id = json.get(JSON_OBJ_TITLE).asString();
                subjectTypes.add(newResourceResponse(id, null, json));
            }
        }

        QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
        return QueryResponsePresentation.perform(handler, request, subjectTypes);
    }

    /**
     * {@inheritDoc}
     *
     * Uses the {@link EntitlementRegistry} to locate the {@link EntitlementSubject} to return.
     */
    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 404,
                            description = SUBJECT_TYPES_RESOURCE + ERROR_404_DESCRIPTION)},
            description = SUBJECT_TYPES_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {

        final Class<? extends EntitlementSubject> subjectClass = entitlementRegistry.getSubjectType(resourceId);

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        if (subjectClass == null) {
            if (debug.errorEnabled()) {
                debug.error("SubjectTypesResource :: READ by " + principalName +
                        "Requested subject short name not found: " + resourceId);
            }
            return new NotFoundException().asPromise();
        }

        final JsonValue json = jsonify(subjectClass, resourceId,
                LogicalSubject.class.isAssignableFrom(subjectClass));

        final ResourceResponse resource = newResourceResponse(resourceId,
                String.valueOf(currentTimeMillis()), json);

        return newResultPromise(resource);
    }

    /**
     * Transforms a subclass of {@link EntitlementSubject} in to a JsonSchema representation.
     * This schema is then combined with the Subject's name (taken as the resourceId) and all this is
     * compiled together into a new {@link JsonValue} object until "title" and "config" fields respectively.
     *
     * @param subjectClass The class whose schema to produce.
     * @param resourceId The ID of the resource to return
     * @return A JsonValue containing the schema of the EntitlementSubject
     */
    private JsonValue jsonify(Class<? extends EntitlementSubject> subjectClass, String resourceId,
                                boolean logical) {
        try {
            final JsonSchema schema = mapper.generateJsonSchema(subjectClass);

            //this will remove the 'subjectName' attribute from those subjects which incorporate it unnecessarily
            final JsonNode node = schema.getSchemaNode().get("properties");

            if (node instanceof ObjectNode) {
                final ObjectNode alter = (ObjectNode) node;
                alter.remove("subjectName");
            }

            return JsonValue.json(JsonValue.object(
                    JsonValue.field(JSON_OBJ_TITLE, resourceId),
                    JsonValue.field(JSON_OBJ_LOGICAL, logical),
                    JsonValue.field(JSON_OBJ_CONFIG, schema)));

        } catch (JsonMappingException e) {
            if (debug.errorEnabled()) {
                debug.error("SubjectTypesResource :: JSONIFY - Error applying " +
                        "jsonification to the Subject class representation.", e);
            }
            return null;
        }
    }
}
