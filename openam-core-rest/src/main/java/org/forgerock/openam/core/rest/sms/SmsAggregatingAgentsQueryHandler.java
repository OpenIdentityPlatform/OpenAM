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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SMS_AGGREGATING_AGENTS_QUERY_HANDLER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;

/**
 * A handler for handling queries on the {@literal realm-config/agents} resource.
 *
 * @since 14.0.0
 */
@RequestHandler(@Handler(mvccSupported = false,
        title = SMS_AGGREGATING_AGENTS_QUERY_HANDLER + "title",
        description = SMS_AGGREGATING_AGENTS_QUERY_HANDLER + "description",
        resourceSchema = @Schema(schemaResource = "SmsAggregatingAgentsQueryHandler.schema.json")))
public class SmsAggregatingAgentsQueryHandler {

    private static final ClassLoader CLASS_LOADER = SmsAggregatingAgentsQueryHandler.class.getClassLoader();
    private static final String OAUTH2_CLIENT = "OAuth2Client";

    private static final JsonPointer POINTER_ID = new JsonPointer("/_id");
    private static final JsonPointer POINTER_TYPE = new JsonPointer("/_type");
    private static final JsonPointer POINTER_STATUS = new JsonPointer("/_status");
    private static final JsonPointer POINTER_LOCATION = new JsonPointer("/_location");

    private final ServiceSchema schema;
    private final Debug debug;
    private final String serviceName;
    private final String serviceVersion;
    private Map<String, SmsJsonConverter> converters = new HashMap<>();


    /**
     * Create an instance of the SmsAggregatingAgentsQueryHandler
     *
     * @param schema The Agents Service Schema
     * @param debug The debugger
     * @throws SMSException Thrown if sub schema cannot be accessed
     */
    public SmsAggregatingAgentsQueryHandler(ServiceSchema schema, Debug debug) throws SMSException {
        this.schema = schema;
        this.debug = debug;
        this.serviceName = schema.getServiceName();
        this.serviceVersion = schema.getVersion();

        for (String subSchema : schema.getSubSchemaNames()) {
            ServiceSchema subServiceSchema = schema.getSubSchema(subSchema);
            converters.put(subSchema, new SmsJsonConverter(subServiceSchema));
        }
    }

    /**
     * Search and filters the agents to match the filtering criteria
     *
     * @param context The request context
     * @param request The query request
     * @param handler The query resource handler
     * @return The query result
     * @throws InternalServerErrorException Thrown when query fails
     */
    @Query(operationDescription = @Operation(description = SMS_AGGREGATING_AGENTS_QUERY_HANDLER + "query.description"),
            type = QueryType.FILTER, queryableFields = "*")
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) throws InternalServerErrorException {

        try {
            //search
            Collection<JsonValue> resultItems = search(context);
            //filter
            List<ResourceResponse> filteredResponses = filterAndPrepareResponse(resultItems, request);
            //sort/page and respond
            QueryResponsePresentation.perform(handler, request, filteredResponses);
        } catch (SMSException | SSOException e) {
            debug.error("SmsAggregatingAgentsQueryHandler:: Unable to query agent config: ", e);
            return new InternalServerErrorException("Unable to query agent config.", e).asPromise();
        }
        return newResultPromise(newQueryResponse());
    }

    private Collection<JsonValue> search(Context context) throws SSOException, SMSException {
        SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        ServiceConfigManager scm = new ServiceConfigManager(ssoToken, serviceName, serviceVersion);
        String realm = context.asContext(RealmContext.class).getRealm().asPath();
        ServiceConfig config = scm.getOrganizationConfig(realm, null);
        Set<String> schemaNames = schema.getSubSchemaNames();

        Collection<JsonValue> queryRes = new HashSet<>();
        for (String schemaName : schemaNames) {
            if (!OAUTH2_CLIENT.equals(schemaName)) {
                Set<String> names = config.getSubConfigNames("*", schemaName);
                for (String configName : names) {
                    ServiceConfig subConfig = config.getSubConfig(configName);
                    SmsJsonConverter converter = converters.get(schemaName);
                    JsonValue value = converter.toJson(realm, subConfig.getAttributes(), false, json(object()));
                    value.add("_id", configName);
                    value.add("_type", schemaName);
                    transformAgentJson(value);
                    queryRes.add(value);
                }
            }
        }
        return queryRes;
    }

    private List<ResourceResponse> filterAndPrepareResponse(Collection<JsonValue> resultItems, QueryRequest request) {
        Collection<JsonValue> filteredItems =
                request.getQueryFilter().accept(new AgentQueryFilterVisitor(), resultItems);
        List<ResourceResponse> resourceResponses = new ArrayList<>();
        for (JsonValue item : filteredItems) {
            resourceResponses.add(newResourceResponse(item.get(POINTER_ID).asString(), String.valueOf(item), item));
        }
        return resourceResponses;
    }

    /**
     * A query filter that filter the agents by name, type, status and location
     */
    private static final class AgentQueryFilterVisitor extends
            BaseQueryFilterVisitor<Collection<JsonValue>, Collection<JsonValue>, JsonPointer> {

        private Set<JsonPointer> supportedFilterFields = new HashSet<>();

        public AgentQueryFilterVisitor() {
            supportedFilterFields.add(POINTER_ID);
            supportedFilterFields.add(POINTER_TYPE);
            supportedFilterFields.add(POINTER_STATUS);
            supportedFilterFields.add(POINTER_LOCATION);
        }

        @Override
        public Collection<JsonValue> visitBooleanLiteralFilter(
                Collection<JsonValue> resultItems, boolean value) {
            return value ? resultItems : Collections.<JsonValue>emptySet();
        }

        @Override
        public Collection<JsonValue> visitAndFilter(Collection<JsonValue> resultItems,
                List<QueryFilter<JsonPointer>> subFilters) {
            Collection<JsonValue> filteredItems = resultItems;
            for (QueryFilter<JsonPointer> filter : subFilters) {
                filteredItems = filter.accept(this, filteredItems);
            }
            return filteredItems;
        }

        @Override
        public Collection<JsonValue> visitOrFilter(Collection<JsonValue> resultItems,
                List<QueryFilter<JsonPointer>> subFilters) {
            Collection<JsonValue> filteredItems = new HashSet<>();
            for (QueryFilter<JsonPointer> filter : subFilters) {
                filteredItems.addAll(filter.accept(this, resultItems));
            }
            return filteredItems;
        }

        @Override
        public Collection<JsonValue> visitContainsFilter(Collection<JsonValue> resultItems,
                JsonPointer field, Object valueAssertion) {
            Collection<JsonValue> filteredItems = new HashSet<>();
            if (supportedFilterFields.contains(field)) {
                for (JsonValue item : resultItems) {
                    if (item.get(field).asString().contains((String) valueAssertion)) {
                        filteredItems.add(item);
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported field, " + field.toString());
            }
            return filteredItems;
        }

        @Override
        public Collection<JsonValue> visitEqualsFilter(Collection<JsonValue> resultItems,
                JsonPointer field, Object valueAssertion) {
            Collection<JsonValue> filteredItems = new HashSet<>();
            if (supportedFilterFields.contains(field)) {
                for (JsonValue item : resultItems) {
                    if (item.get(field).asString().equals(valueAssertion)) {
                        filteredItems.add(item);
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported field, " + field.toString());
            }
            return filteredItems;
        }
    }

    private static final JsonPointer LOCATION_SOURCE_POINTER =
            new JsonPointer("/com.sun.identity.agents.config.repository.location");
    private static final JsonPointer LOCATION_DEST_POINTER = new JsonPointer("/_location");
    private static final JsonPointer STATUS_SOURCE_POINTER = new JsonPointer("/sunIdentityServerDeviceStatus");
    private static final JsonPointer STATUS_DEST_POINTER = new JsonPointer("/_status");

    private void transformAgentJson(JsonValue value) {
        move(value, LOCATION_SOURCE_POINTER, LOCATION_DEST_POINTER);
        move(value, STATUS_SOURCE_POINTER, STATUS_DEST_POINTER);
    }

    private void move(JsonValue value, JsonPointer source, JsonPointer dest) {
        JsonValue target = value.get(source);
        if (target != null) {
            value.remove(source);
            value.add(dest, target.asString());
        }
    }
}