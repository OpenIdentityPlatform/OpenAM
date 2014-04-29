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
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.shared.debug.Debug;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.security.auth.Subject;
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
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.entitlements.model.json.JsonEntitlementConditionModule;
import org.forgerock.openam.rest.resource.SubjectContext;

/**
 * Allows for CREST-handling of stored {@link com.sun.identity.policy.interfaces.Condition}s.
 *
 * These are unmodfiable - even by an administrator. As such this
 * endpoint only supports the READ and QUERY operations.
 */
public class ConditionTypesResource implements CollectionResourceProvider {

    private final static String JSON_OBJ_TITLE = "title";
    private final static String JSON_OBJ_CONFIG = "config";

    private final static ObjectMapper mapper = new ObjectMapper().withModule(new JsonEntitlementConditionModule());
    private final Debug debug;
    private final EntitlementRegistry entitlementRegistry;

    /**
     * Guiced constructor.
     *
     * @param debug Debug instance
     */
    @Inject
    public ConditionTypesResource(@Named("frRest") Debug debug, EntitlementRegistry entitlementRegistry) {
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
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        final Subject mySubject = getSubject(context, handler);

        if (mySubject == null) {
            return;
        }

        final Set<String> conditionTypeNames;
        List<JsonValue> conditionTypes = new ArrayList<JsonValue>();

        conditionTypeNames = entitlementRegistry.getConditionsShortNames();

        for (String conditionTypeName : conditionTypeNames) {
            final Class<? extends EntitlementCondition> conditionClass = entitlementRegistry.getConditionType(conditionTypeName);

            if (conditionClass == null) {
                debug.error("Listed condition short name not found: " + conditionTypeName);
                continue;
            }

            final JsonValue json = jsonify(conditionClass, conditionTypeName);

            if (json != null) {
                conditionTypes.add(json);
            }
        }

        int totalSize = conditionTypes.size();
        int pageSize = request.getPageSize();
        int offset = request.getPagedResultsOffset();

        if (pageSize > 0) {
            conditionTypes = conditionTypes.subList(offset, offset + pageSize);
        }

        final JsonPointer jp = new JsonPointer(JSON_OBJ_TITLE);

        for (JsonValue conditionTypeToReturn : conditionTypes) {

            final JsonValue resourceId = conditionTypeToReturn.get(jp);
            final String id = resourceId != null ? resourceId.toString() : null;

            final Resource resource = new Resource(id, "0", conditionTypeToReturn);

            handler.handleResource(resource);
        }

        //paginate
        if (pageSize > 0) {
            final String lastIndex = offset + pageSize > totalSize ? String.valueOf(totalSize) : String.valueOf(offset + pageSize);
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
            debug.error("Error retrieving Subject identification from request.");
            handler.handleError(ResourceException.getException(ResourceException.FORBIDDEN));
            return null;
        }

        return mySubject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {

        final Subject mySubject = getSubject(context, handler);

        if (mySubject == null) {
            return;
        }

        final Class<? extends EntitlementCondition> conditionClass = entitlementRegistry.getConditionType(resourceId);

        if (conditionClass == null) {
            debug.error("Requested condition short name not found: " + resourceId);
            handler.handleError(ResourceException.getException(ResourceException.NOT_FOUND));
            return;
        }

        final JsonValue json = jsonify(conditionClass, resourceId);

        final Resource resource = new Resource(resourceId, "0", json);
        handler.handleResult(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
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
    protected JsonValue jsonify(Class<? extends EntitlementCondition> conditionClass, String resourceId) {
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
                    JsonValue.field(JSON_OBJ_CONFIG, schema)));

        } catch (JsonMappingException e) {
            debug.error("Error applying jsonification to the Condition class representation.", e);
            return null;
        }
    }
}
