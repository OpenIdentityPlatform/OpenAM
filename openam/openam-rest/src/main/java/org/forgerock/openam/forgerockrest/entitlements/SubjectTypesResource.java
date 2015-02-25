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

import javax.inject.Named;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.LogicalSubject;
import com.sun.identity.shared.debug.Debug;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;
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
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.entitlements.model.json.JsonEntitlementConditionModule;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.util.Reject;

/**
 * Allows for CREST-handling of stored {@link EntitlementSubject}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 *
 * @see ConditionTypesResource
 */
public class SubjectTypesResource implements CollectionResourceProvider {

    private final static String JSON_OBJ_TITLE = "title";
    private final static String JSON_OBJ_LOGICAL = "logical";
    private final static String JSON_OBJ_CONFIG = "config";

    private final JsonPointer JSON_POINTER_TO_TITLE = new JsonPointer(JSON_OBJ_TITLE);

    private final static ObjectMapper mapper = new ObjectMapper().withModule(new JsonEntitlementConditionModule());
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
     * Unsupported by this endpoint.
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Unsupported by this endpoint.
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     *
     * Uses the {@link EntitlementRegistry} to locate the {@link EntitlementSubject}s to return.
     *
     * Looks up all the names of subjects registered in the system, and then returns each one to the
     * result handler having determined its schema and jsonified it.
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        final Set<String> subjectTypeNames = new TreeSet<String>();
        List<JsonValue> subjectTypes = new ArrayList<JsonValue>();

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
                subjectTypes.add(json);
            }
        }

        handler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);

        int remaining = 0;
        if (subjectTypes.size() > 0) {
            remaining = subjectTypes.size();
            for (JsonValue subjectTypeToReturn : subjectTypes) {

                final JsonValue resourceId = subjectTypeToReturn.get(JSON_POINTER_TO_TITLE);
                final String id = resourceId != null ? resourceId.toString() : null;

                boolean keepGoing = handler.handleResource(new Resource(id,
                        String.valueOf(System.currentTimeMillis()), subjectTypeToReturn));
                remaining--;
                if (debug.messageEnabled()) {
                    debug.message("SubjectTypesResource :: QUERY by " + principalName +
                            ": Added resource to response: " + id);
                }
                if (!keepGoing) {
                    break;
                }
            }
        }

        handler.handleResult(new QueryResult(null, remaining));

    }

    /**
     * {@inheritDoc}
     *
     * Uses the {@link EntitlementRegistry} to locate the {@link EntitlementSubject} to return.
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler) {

        final Class<? extends EntitlementSubject> subjectClass = entitlementRegistry.getSubjectType(resourceId);

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        if (subjectClass == null) {
            if (debug.errorEnabled()) {
                debug.error("SubjectTypesResource :: READ by " + principalName +
                        "Requested subject short name not found: " + resourceId);
            }
            handler.handleError(ResourceException.getException(ResourceException.NOT_FOUND));
            return;
        }

        final JsonValue json = jsonify(subjectClass, resourceId,
                LogicalSubject.class.isAssignableFrom(subjectClass));

        final Resource resource = new Resource(resourceId, String.valueOf(System.currentTimeMillis()), json);
        handler.handleResult(resource);
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
